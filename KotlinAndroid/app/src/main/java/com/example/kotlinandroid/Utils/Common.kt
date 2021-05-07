package com.example.kotlinandroid.Utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile.isDocumentUri
import java.lang.Exception
import java.net.URISyntaxException
import kotlin.jvm.Throws

object Common{
    @SuppressLint("NewApi")
    @Throws(URISyntaxException::class)
    fun getFilePath(context: Context, uri: Uri):String?
    {
        var uri=uri
        var selection:String?=null
        var selectionArgs:Array<String>?=null

        if(Build.VERSION.SDK_INT >=19 && isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocuments(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                uri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/publicdownloads"),
                    id.toLong()
                )
            } else if (isMediaDocumnet(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("image" == type)
                    uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else if ("video" == type)
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else if ("audio" == type)
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                selection = "_id=?"
                selectionArgs = arrayOf(split[1])
            }
        }
        if("content".equals(uri.scheme!!, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor?=null
            try{
                cursor = context.contentResolver.query(uri,projection,selection,selectionArgs,null)
                val collumn_index = cursor!!.getColumnIndex(MediaStore.Images.Media.DATA)
                if(cursor.moveToFirst())
                    return cursor.getString(collumn_index)
            }catch (e:Exception) {}
        } else if("file".equals(uri.scheme!!,ignoreCase = true))
            return uri.path
        return null
    }

    private fun isMediaDocumnet(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isDownloadsDocuments(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
}