package com.example.camerapoc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.camerapoc.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class MainActivity : ComponentActivity() {

    private lateinit var binding : ActivityMainBinding
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null

    private val CAMERA_PERMISSION_REQUEST = 2
    private var currentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpLauncher()

        binding.btnCapturePhoto.setOnClickListener {
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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createFileInInternalStorage()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
            photoFile?.also {
                try {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher!!.launch(takePictureIntent)
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
        }
        return file
    }
    private fun setUpLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode === RESULT_OK) {
                val myBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                binding.imgView.setImageBitmap(myBitmap)
            }
        }
    }

}