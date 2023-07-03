package com.example.camerapoc

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipFileUtils {

    companion object {

        @Throws(IOException::class)
        fun saveZipFileToDownloads(context: Context, zipFileUri: Uri?, fileName: String?) {
            val inputStream = context.contentResolver.openInputStream(zipFileUri!!)
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputFile = File(downloadsDir, fileName)
            val outputStream: OutputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream!!.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            inputStream.close()
            Toast.makeText(context, "Zip file successfully saved", Toast.LENGTH_LONG).show()
        }

        fun makeZipFile(zipFilePath: File, imageFiles: Array<File>) {
            try {
                val outputStream = FileOutputStream(zipFilePath)
                val zipOutputStream = ZipOutputStream(BufferedOutputStream(outputStream))

                for (imageFile in imageFiles) {
                    val entry = ZipEntry(imageFile.name)
                    zipOutputStream.putNextEntry(entry)

                    val fileInputStream = FileInputStream(imageFile)
                    val bufferedInputStream = BufferedInputStream(fileInputStream)

                    val buffer = ByteArray(1024)
                    var read: Int
                    while (bufferedInputStream.read(buffer).also { read = it } != -1) {
                        zipOutputStream.write(buffer, 0, read)
                    }

                    bufferedInputStream.close()
                    fileInputStream.close()
                    zipOutputStream.closeEntry()
                }
                zipOutputStream.close()
                Log.e("==", "successfully saved file")
            } catch (e: IOException) {
                Log.e("IOException==", e.message.toString())
            }
        }
    }
}