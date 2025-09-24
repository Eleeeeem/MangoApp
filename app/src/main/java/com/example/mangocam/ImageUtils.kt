package com.example.mangoo

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

fun AppCompatActivity.uriToBitmap(uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }
}

fun Bitmap.toBase64(quality: Int = 85): String {
    return ByteArrayOutputStream().use { outputStream ->
        this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}