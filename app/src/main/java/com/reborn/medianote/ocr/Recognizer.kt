package com.reborn.medianote.ocr

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition


class Recognizer {
    companion object {
        fun processImage(img: ImageView, onSuccessCallback: (Text) -> Unit, onFailCallback: (Exception) -> Unit) {
            if (img.drawable == null) {
                return;
            }

            val bitmap = (img.drawable as BitmapDrawable).bitmap
            val image = InputImage.fromBitmap(bitmap, 0)

            val task = getTextRecognizer().process(image)
            task.addOnSuccessListener(onSuccessCallback).addOnFailureListener(onFailCallback)
        }

        fun processImage(bitmap: Bitmap, onSuccessCallback: (Text) -> Unit, onFailCallback: (Exception) -> Unit) {
            val image = InputImage.fromBitmap(bitmap, 0)

            val task = getTextRecognizer().process(image)
            task.addOnSuccessListener(onSuccessCallback).addOnFailureListener(onFailCallback)
        }

        private fun getTextRecognizer() : com.google.mlkit.vision.text.TextRecognizer {
            return TextRecognition.getClient()
        }
    }
}