package com.example.camerapoc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.camerapoc.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var binding : ActivityMainBinding
    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_REQUEST = 2
    private var currentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapturePhoto.setOnClickListener {
            Log.e("==","===")
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            launchCameraIntent()
        }
    }

    private fun launchCameraIntent() {
        Log.e("launchCameraIntent==", currentPhotoPath)
        Toast.makeText(applicationContext, "launchCameraIntent =$currentPhotoPath", Toast.LENGTH_LONG).show()
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createFileInInternalStorage()
            } catch (ex: IOException) {
                Toast.makeText(applicationContext, "launchCameraIntent IOException =$currentPhotoPath", Toast.LENGTH_LONG).show()
                Log.e("launchCameraIntent IOException==", ex.message.toString())
                null
            }
            Log.e("photoFile==", photoFile.toString())
            photoFile?.also {
                try {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }catch (e: Exception) {
                    Log.e("photoFile Exception ==", e.message.toString())
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, filename: String) {
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
            // Image saved successfully
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error
        }
    }

    private fun createImageFile(): File {
        Log.e("createImageFile==", "==")
        Toast.makeText(applicationContext, "createImageFile", Toast.LENGTH_LONG).show()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun createImageFileInternal(): File {
        Log.e("createImageFileInternal==", "==")
        val cw = ContextWrapper(applicationContext)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val directory: File = cw.getDir("imageDir", MODE_PRIVATE)
        val storageDir = File(directory, timeStamp)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
            Log.e("createImageFileInternal apply==", currentPhotoPath)
        }
    }

    private fun createFileInInternalStorage(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        var fileName = "$timeStamp.jpg"
        val file = File(applicationContext.filesDir, fileName)
        try {
            file.createNewFile()
            currentPhotoPath = file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error
        }
        return file
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(applicationContext, "onActivityResult =$currentPhotoPath", Toast.LENGTH_LONG).show()
            Log.e("onActivityResult==", currentPhotoPath)
            val myBitmap = BitmapFactory.decodeFile(currentPhotoPath)
            binding.imgView.setImageBitmap(myBitmap)
        }
    }
}