package com.example.camerapoc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
//import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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

        binding.btnDelete.setOnClickListener {
            deleteImagesFromInternalMemory()
        }

        //getMetaDataFromAsset()
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
//                createFileInExternalStorage()
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

    private fun createFileInExternalStorage(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        var file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        currentPhotoPath = file.absolutePath
        return file
    }
    private fun setUpLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode === RESULT_OK) {
                setMetadataToImage(currentPhotoPath)
                val myBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                binding.imgView.setImageBitmap(myBitmap)
                getMetadata(currentPhotoPath)
            }
        }
    }

    private fun getMetadata(currentPhotoPath: String) {
        val exifInterface = ExifInterface(currentPhotoPath)
        val takenByDevice = exifInterface.getAttribute("TakenByDevice")
        val tagMake = exifInterface.getAttribute(ExifInterface.TAG_MAKE)
        val tagDesc = exifInterface.getAttribute("ImageDescription")
        Log.e("tagMake==", tagMake.toString())
        Log.e("takenByDevice==", takenByDevice.toString())
        Log.e("tagDesc==", tagDesc.toString())

        val tagWith = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
        val tagDimensionX = exifInterface.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION)
        Toast.makeText(applicationContext, "tagWith/tagDimensionX = $tagWith, $tagDimensionX", Toast.LENGTH_LONG).show()
    }

    private fun setMetadataToImage(currentPhotoPath: String) {
        val exifInterface = ExifInterface(currentPhotoPath)
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "Custom Make Model")
        exifInterface.setAttribute("TakenByDevice", "Redmi5g")
        exifInterface.setAttribute("ImageDescription", "Your image description")

        exifInterface.saveAttributes()

    }

    private fun getMetaDataFromAsset() {
        val inputStream = assets.open("images.jpg")
        val exifInterface = ExifInterface(inputStream)
        val imageType = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
        Log.e("type==", imageType.toString())
    }
    private fun deleteImagesFromInternalMemory() {
        val imageFile = File(currentPhotoPath)

        if (imageFile.exists()) {
            val deleted = imageFile.delete()

            if (deleted) {
                Log.e("deleteImagesFromInternalMemory==", "deleted")
            } else {
                Log.e("deleteImagesFromInternalMemory==", "not able to delete")
            }
        } else {
            Log.e("deleteImagesFromInternalMemory==", "file does not exist")
        }
    }
}