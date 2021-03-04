package com.reborn.medianote.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.*

class SpeechToText(
        ctx: Context?
) {

    private var speechIntent: Intent = makeSpeechIntent()
    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)

    var onBeginningSpeechFunction: () -> Unit = {}
    var onResultsFunction: (Bundle?) -> Unit = {}
    var onReadyForSpeechFunction: (Bundle?) -> Unit = {}
    var onRmsChangedFunction: (Float) -> Unit = {}
    var onBufferReceivedFunction: (ByteArray?) -> Unit = {}
    var onPartialResultsFunction: (Bundle?) -> Unit = {}
    var onEventFunction: (Int, Bundle?) -> Unit = { _: Int, _: Bundle? -> }
    var onEndOfSpeechFunction: () -> Unit = {}
    var onErrorFunction: (Int) -> Unit = {}


    private fun makeSpeechIntent(): Intent {
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        return speechIntent
    }

    fun destroy() {
        speechRecognizer.destroy()
    }

    fun build() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onReadyForSpeechFunction(params)
            }

            override fun onRmsChanged(rmsdB: Float) {
                onRmsChangedFunction(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                onBufferReceivedFunction(buffer)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                onPartialResultsFunction(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                onEventFunction(eventType, params)
            }

            override fun onBeginningOfSpeech() {
                onBeginningSpeechFunction()
            }

            override fun onEndOfSpeech() {
                onEndOfSpeechFunction()
            }

            override fun onError(error: Int) {
                onErrorFunction(error)
            }

            override fun onResults(results: Bundle?) {
                onResultsFunction(results)
            }
        })
    }

    fun startListening() {
        speechRecognizer.startListening(speechIntent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun get(): SpeechRecognizer {
        return speechRecognizer
    }
}