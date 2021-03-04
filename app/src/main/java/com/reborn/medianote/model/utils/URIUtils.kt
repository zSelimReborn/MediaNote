package com.reborn.medianote.model.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class URIUtils {
    companion object {
        fun getRealPathFromUri(ctx: Context, uri: Uri): String {
            var cursor: Cursor? = ctx.contentResolver.query(uri, null, null, null, null)
            if (cursor == null) {return ""}

            cursor.moveToFirst()
            var documentId: String = cursor.getString(0)
            documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
            cursor.close()

            cursor = ctx.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null
                    , MediaStore.Images.Media._ID + " = ? ", arrayOf(documentId), null)
            if (cursor == null) {return ""}
            cursor.moveToFirst()
            val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            cursor.close()

            return path
        }
    }
}
