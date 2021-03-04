package com.reborn.medianote

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.text.Text
import com.reborn.medianote.model.note.NoteType
import com.reborn.medianote.model.utils.URIUtils
import com.reborn.medianote.ocr.Recognizer
import java.lang.Exception

enum class Mode {
    ADD,EDIT
}

class AddEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID         = "com.reborn.medianote.EXTRA_ID"
        const val EXTRA_NOTE_TITLE      = "com.reborn.medianote.EXTRA_TITLE"
        const val EXTRA_NOTE_CONTENT    = "com.reborn.medianote.EXTRA_CONTENT"
        const val EXTRA_NOTE_DELETE     = "com.reborn.medianote.EXTRA_DELETE"
        const val EXTRA_NOTE_POSITION   = "com.reborn.medianote.EXTRA_POSITION"
        const val EXTRA_NOTE_IMAGE      = "com.reborn.medianote.EXTRA_IMAGE"
        const val EXTRA_NOTE_AUDIO      = "com.reborn.medianote.EXTRA_AUDIO"
        const val EXTRA_NOTE_TYPE       = "com.reborn.medianote.EXTRA_TYPE"
    }

    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 0

    private lateinit var mode: Mode
    private lateinit var noteType: NoteType
    private var noteId: Int = -1
    private var notePosition: Int = -1

    private lateinit var noteTitleInput: EditText
    private lateinit var noteContentInput: EditText
    private lateinit var noteImageView: ImageView
    private lateinit var noteImageViewExpanded: ImageView
    private lateinit var noteImageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        noteTitleInput = findViewById(R.id.newNoteTitle)
        noteContentInput = findViewById(R.id.newNoteContent)
        noteImageView = findViewById(R.id.newNoteImage)
        noteImageViewExpanded = findViewById(R.id.expandedImage)

        setupNoteId()
        setupNotePosition()
        setupMode()
        setupNoteType()
        setupOnAudioMode()

        try {
            setupPhotoMode()
        } catch (exception: Exception) {

        }

        setupImageClick()

        when (mode) {
            Mode.ADD ->  {
                title = getString(R.string.add_note_activity_title)
            }

            Mode.EDIT -> {
                title = getString(R.string.edit_note_activity_title)
                setupInputs()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_addedit_note, menu)
        if (mode == Mode.ADD) {
            menu?.findItem(R.id.deleteNoteButton)?.isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.saveNoteButton -> {
                if (saveNoteAction()) {
                    finish()
                }

                true
            }
            R.id.deleteNoteButton -> {
                deleteNoteAction()
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupNoteId() {
        noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1)
    }

    private fun setupNotePosition() {
        notePosition = intent.getIntExtra(EXTRA_NOTE_POSITION, -1)
    }

    private fun setupMode() {
        mode = if (noteId == -1) Mode.ADD else Mode.EDIT
    }

    private fun setupInputs() {
        if (mode != Mode.EDIT) {
            return
        }

        noteTitleInput.setText(intent.getStringExtra(EXTRA_NOTE_TITLE))
        noteContentInput.setText(intent.getStringExtra(EXTRA_NOTE_CONTENT))
    }

    private fun setupNoteType() {
        when (intent.getStringExtra(EXTRA_NOTE_TYPE)) {
            null -> noteType = NoteType.NORMAL
            "normal" -> noteType = NoteType.NORMAL
            "audio" -> noteType = NoteType.AUDIO
            "image" -> noteType = NoteType.IMAGE
        }
    }

    private fun setupOnAudioMode() {
        if (noteType != NoteType.AUDIO) {
            return
        }

        val content = intent.getStringExtra(EXTRA_NOTE_CONTENT)
        noteContentInput.setText(content)
    }

    private fun setupPhotoMode() {
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_NOTE_IMAGE) ?: return

        noteType = NoteType.IMAGE
        noteImageUri = imageUri

        noteImageView.setImageURI(imageUri)
        noteImageViewExpanded.setImageURI(imageUri)
        noteImageView.visibility = View.VISIBLE

        if (mode == Mode.EDIT) { return }

        Recognizer.processImage(noteImageView, {
            onImageRecognitionSuccess(it)
        }, {
            onImageRecognitionError(it.toString())
        })
    }

    private fun onImageRecognitionSuccess(t: Text) {
        if (t.textBlocks.size <= 0) {
            onImageRecognitionError(getString(R.string.image_recognition_error))
            return
        }

        for (block in t.textBlocks) {
            noteContentInput.append(block.text + "\n")
        }
    }

    private fun onImageRecognitionError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    private fun saveNoteAction(): Boolean {
        val title = noteTitleInput.text.toString()
        val content = noteContentInput.text.toString()
        val type = noteType.type

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_title_or_content_error), Toast.LENGTH_LONG).show()
            return false
        }

        val data = Intent()
        if (noteId != -1) {
            data.putExtra(EXTRA_NOTE_ID, noteId)
        }

        data.putExtra(EXTRA_NOTE_TYPE, type)
        data.putExtra(EXTRA_NOTE_TITLE, title)
        data.putExtra(EXTRA_NOTE_CONTENT, content)

        var noteImagePath = ""
        try {
            noteImagePath = URIUtils.getRealPathFromUri(this, noteImageUri)
        } catch (exception: Exception) {
            Log.i(Log.DEBUG.toString(), exception.toString())
        }
        when (noteType) {
            NoteType.NORMAL -> {}
            NoteType.IMAGE -> data.putExtra(EXTRA_NOTE_IMAGE, noteImagePath)
            NoteType.AUDIO -> {}
        }

        data.putExtra(EXTRA_NOTE_POSITION, notePosition)
        setResult(Activity.RESULT_OK, data)
        return true
    }

    private fun deleteNoteAction() {
        val data = Intent()
        data.putExtra(EXTRA_NOTE_ID, noteId)
        data.putExtra(EXTRA_NOTE_POSITION, notePosition)
        data.putExtra(EXTRA_NOTE_DELETE, 1)

        setResult(Activity.RESULT_OK, data)
    }

    private fun setupImageClick() {
        if (noteType != NoteType.IMAGE) { return }
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        noteImageView.setOnClickListener {
            zoomNoteImage()
        }
    }

    private fun zoomNoteImage() {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        currentAnimator?.cancel()

        // Load the high-resolution "zoomed-in" image.
        val expandedImageView: ImageView = noteImageViewExpanded

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        noteImageView.getGlobalVisibleRect(startBoundsInt)
        findViewById<View>(R.id.noteEditContainer)
                .getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        noteImageView.alpha = 0f
        expandedImageView.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        currentAnimator = AnimatorSet().apply {
            play(ObjectAnimator.ofFloat(
                    expandedImageView,
                    View.X,
                    startBounds.left,
                    finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
            noteImageViewExpanded.setBackgroundResource(R.color.light_black)
        }

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        expandedImageView.setOnClickListener {
            currentAnimator?.cancel()
            noteImageViewExpanded.setBackgroundResource(R.color.transparent)

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        noteImageView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        noteImageView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }
}