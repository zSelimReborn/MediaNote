package com.reborn.medianote

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.SpeechRecognizer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.text.Text
import com.reborn.medianote.frontend.NoteGridAdapter
import com.reborn.medianote.frontend.NoteViewModel
import com.reborn.medianote.model.note.Note
import com.reborn.medianote.model.note.NoteType
import com.reborn.medianote.model.utils.PermissionsUtils
import com.reborn.medianote.ocr.Recognizer
import com.reborn.medianote.record.SpeechToText
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val ADD_NOTE_REQUEST = 1
        const val EDIT_NOTE_REQUEST = 2
        const val PICK_IMAGE_REQUEST = 3
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var noNotesMessage: TextView
    private lateinit var bottomAppBar: BottomAppBar

    private lateinit var vm: NoteViewModel
    private lateinit var adapter: NoteGridAdapter

    private lateinit var speechRecognizer: SpeechToText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBottomAppBar()

        vm = ViewModelProvider(this).get(NoteViewModel::class.java)

        initializeRecycleView()
        initializeSpeechRecognizer()
        initializeListeners()
    }

    private fun setupBottomAppBar() {
        bottomAppBar = findViewById(R.id.bottomAppBar)

        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.newImgNote -> {
                    newImgNoteMenuClick()
                    true
                }
                R.id.newAudioNote -> {
                    newAudioNoteMenuClick()
                    true
                }

                else -> super.onOptionsItemSelected(it)
            }
        }
    }

    private fun newImgNoteMenuClick() {
        if (!PermissionsUtils.hasImagePermission(this)) {
            PermissionsUtils.askImagePermissions(this)
            return
        }

        pickImage()
        return
    }

    private fun newAudioNoteMenuClick() {
        if (!PermissionsUtils.hasAudioPermission(this)) {
            PermissionsUtils.askAudioPermission(this)
            return
        }

        micOn()
        speechRecognizer.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return super.onOptionsItemSelected(item)
    }

    private fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, getString(R.string.label_image_chooser)), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            requestNewNoteFromImage(data)
        } else if (requestCode == ADD_NOTE_REQUEST && resultCode == Activity.RESULT_OK) {
            saveNote(data)
            showMessage(R.string.add_note_success_message)
        } else if (requestCode == EDIT_NOTE_REQUEST && resultCode == Activity.RESULT_OK) {
            editOrDeleteNote(data)
            val resMessage: Int = if (data?.getIntExtra(AddEditActivity.EXTRA_NOTE_DELETE, 0) == 1) R.string.delete_note_success_message else R.string.edit_note_success_message
            showMessage(resMessage)
        } else {
            showMessage(R.string.no_note_saving_message)
        }
    }

    private fun initializeRecycleView() {
        recyclerView = findViewById(R.id.notesListView)
        noNotesMessage = findViewById(R.id.noNotesAvailable)

        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        vm.getAllNotes().observe(this, Observer {
            adapter = NoteGridAdapter(it) { note, position -> onClickNote(note, position) }
            recyclerView.adapter = adapter

            updateViewOnListNotes(it)
        })
    }

    private fun updateViewOnListNotes(notes: List<Note>) {
        if (notes.isEmpty()) {
            recyclerView.visibility = View.GONE
            noNotesMessage.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noNotesMessage.visibility = View.GONE
        }
    }

    private fun initializeListeners() {
        findViewById<FloatingActionButton>(R.id.newNoteButton).setOnClickListener { _ ->
            val intent = Intent(this, AddEditActivity::class.java)

            startActivityForResult(intent, ADD_NOTE_REQUEST)
        }

        findViewById<EditText>(R.id.searchNoteInput).addTextChangedListener {
            adapter.filter.filter(it.toString())
        }
    }

    private fun saveNote(data: Intent?) {
        val title = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_TITLE)
        val content = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_CONTENT)
        val type = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_TYPE)?: NoteType.NORMAL.type

        val imageUri = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_IMAGE)
        var imageUriString: String = ""
        if (imageUri != null) {
            imageUriString = imageUri.toString()
        }

        if (title.isNullOrEmpty() || content.isNullOrEmpty()) {
            return
        }

        vm.insert(Note(title, content, type, imageUriString))
    }

    private fun editOrDeleteNote(data: Intent?) {
        val deleteRequest = data?.getIntExtra(AddEditActivity.EXTRA_NOTE_DELETE, 0)
        val id = data?.getIntExtra(AddEditActivity.EXTRA_NOTE_ID, -1)
        val type = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_TYPE)?: NoteType.NORMAL.type

        if (deleteRequest == 1) {
            if (id == -1) { return }
            val position = data.getIntExtra(AddEditActivity.EXTRA_NOTE_POSITION, -1)
            if (position == -1) { return }

            val note = adapter.getNoteAt(position)
            vm.delete(note)
            return
        }

        val title = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_TITLE)
        val content = data?.getStringExtra(AddEditActivity.EXTRA_NOTE_CONTENT)
        val position = data?.getIntExtra(AddEditActivity.EXTRA_NOTE_POSITION, -1)

        var imageUriString = ""
        if (position != -1 && position != null) {
            imageUriString = adapter.getNoteAt(position).mediaUrl
        }

        if (id == -1 || title.isNullOrEmpty() || content.isNullOrEmpty()) {
            return
        }

        vm.update(Note(title, content, type, imageUriString, id))
    }

    private fun onClickNote(note: Note, position: Int) {
        val intent = Intent(this, AddEditActivity::class.java)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_ID, note.id)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_TITLE, note.title)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_CONTENT, note.content)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_POSITION, position)

        val imageUri = Uri.fromFile(File(note.mediaUrl))
        if (imageUri != null && note.type == NoteType.IMAGE.type) {
            intent.putExtra(AddEditActivity.EXTRA_NOTE_IMAGE, imageUri)
        }

        startActivityForResult(intent, EDIT_NOTE_REQUEST)
    }

    private fun requestNewNoteFromImage(data: Intent?) {
        val intent = Intent(this, AddEditActivity::class.java)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_IMAGE, data?.data)
        startActivityForResult(intent, ADD_NOTE_REQUEST)
    }

    private fun requestNewNoteFromAudio(content: String) {
        val intent = Intent(this, AddEditActivity::class.java)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_TYPE, NoteType.AUDIO.type)
        intent.putExtra(AddEditActivity.EXTRA_NOTE_CONTENT, content)

        startActivityForResult(intent, ADD_NOTE_REQUEST)
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechToText(this)
        speechRecognizer.onBeginningSpeechFunction = { onSpeechStart() }
        speechRecognizer.onResultsFunction = { bundle ->  onSpeechRecognize(bundle) }

        speechRecognizer.build()
    }

    private fun onSpeechStart() {
        Toast.makeText(this, getString(R.string.on_beginning_speech_text), Toast.LENGTH_LONG).show()
    }

    private fun onSpeechRecognize(bundle: Bundle?) {
        micOff()
        val data = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return

        val content = data[0]
        requestNewNoteFromAudio(content)
    }

    private fun changeMicIconInMenu(res: Int) {
        bottomAppBar.menu.findItem(R.id.newAudioNote).icon = ContextCompat.getDrawable(this, res)
    }

    private fun micOff() {
        changeMicIconInMenu(R.drawable.ic_microphone_off)
    }

    private fun micOn() {
        Toast.makeText(this, getString(R.string.on_press_speech_text), Toast.LENGTH_LONG).show()
        changeMicIconInMenu(R.drawable.ic_microphone_on)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionsUtils.REQUEST_CODE_IMAGE && grantResults.isNotEmpty() && grantResults.size >= 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                pickImage()
            } else {
                showMessage(R.string.no_image_permissions_granted)
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CODE_AUDIO && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                micOn()
                speechRecognizer.startListening()
            } else {
                showMessage(R.string.no_image_permissions_granted)
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }

    private fun showMessage(res: Int) {
        val mainContent = findViewById<View>(android.R.id.content)
        Snackbar.make(mainContent, res, Snackbar.LENGTH_LONG).setAnchorView(bottomAppBar).show()
    }
}