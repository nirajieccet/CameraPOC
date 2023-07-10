package com.example.camerapoc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
//import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.example.camerapoc.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var binding : ActivityMainBinding
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var currentPhotoPath: String = ""

    private val REQUEST_CODE_PERMISSION = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        /*This is only for saving zip file to download folder*/
        //requestExternalStoragePermission()
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }

        setUpLauncher()

        binding.btnCapturePhoto.setOnClickListener {
            launchCameraIntent()
        }

        binding.btnDelete.setOnClickListener {
            deleteImagesFromInternalMemory()
        }

        //get all file and make a zip file and save it to download folder
        binding.btnZipAllImages.setOnClickListener {
            //getFilePathAndSaveItToDownloadFolder()
            shareImage()
        }

        //getMetaDataFromAsset()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                //getLocation()
                getLocationUsingFusedProvider()
            }
        }
    }

    private fun accessMediaLocation() {
        // Get the media Uri
        val mediaUri: Uri = currentPhotoPath.toUri()//MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // Perform any necessary queries or operations with the media Uri
        // For example, you can use a ContentResolver to query the media and retrieve its details

        // Once you have the original Uri, you can retrieve the geo location information using ExifInterface
        val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API 29) and above, use the original Uri obtained from the media
            val originalUri = MediaStore.setRequireOriginal(mediaUri)
            contentResolver.openInputStream(originalUri)?.let { ExifInterface(it) }
        } else {
            // For devices below Android 10, use the media Uri directly
            contentResolver.openInputStream(mediaUri)?.let { ExifInterface(it) }
        }

        // Read the geo location information from ExifInterface
        val latitude = exifInterface?.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val longitude = exifInterface?.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)

        // Handle the retrieved geo location information as needed
        Log.d("TAG==", "Latitude: $latitude, Longitude: $longitude")
    }

    /*This is only for saving zip file to download folder*/
    private fun requestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                33
            )
        }
    }
    private fun launchCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)//INTENT_ACTION_STILL_IMAGE_CAMERA give option to choose app for open camera
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createFileInInternalStorage()
                //createFileInExternalStorage()
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
                } catch (e: Exception) {
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
            Log.e("currentPhotoPath==", currentPhotoPath)
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
        Log.e("currentPhotoPath==", currentPhotoPath)
        return file
    }
    private fun setUpLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode === RESULT_OK) {
                setMetadataToImage(currentPhotoPath)
                val myBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                binding.imgView.setImageBitmap(myBitmap)
                getMetadata(currentPhotoPath)
                //accessMediaLocation()
            }
        }
    }

    private fun getMetadata(currentPhotoPath: String) {
        val exifInterface = ExifInterface(currentPhotoPath)
        printLog(exifInterface)
    }
    private fun printLog(exifInterface: ExifInterface) {
        Log.e("TAG_MAKE==", exifInterface.getAttribute(ExifInterface.TAG_MAKE).toString())
        Log.e("TAG_IMAGE_DESCRIPTION==", exifInterface.getAttribute("ImageDescription").toString())
        Log.e("TakenByDevice==", exifInterface.getAttribute("TakenByDevice").toString())

        Log.e("TAG_IMAGE_WIDTH==", exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH).toString())
        Log.e("TAG_PIXEL_X_DIMENSION==", exifInterface.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION).toString())
        Log.e("TAG_PIXEL_Y_DIMENSION==", exifInterface.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION).toString())
        Log.e("TAG_ORIENTATION==", exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).toString())
        Log.e("TAG_CAMERA_OWNER_NAME==", exifInterface.getAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME).toString())
        Log.e("TAG_GPS_ALTITUDE==", exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE).toString())

        Log.e("TAG_GPS_ALTITUDE_REF==", exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF).toString())
        Log.e("TAG_GPS_LATITUDE_REF==", exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).toString())
        Log.e("TAG_GPS_LATITUDE==", exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toString())
        Log.e("TAG_GPS_LONGITUDE==", exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).toString())
        Log.e("ORIENTATION_ROTATE_180==", exifInterface.getAttribute(ExifInterface.ORIENTATION_ROTATE_180.toString()).toString())
        Log.e("TAG_MODEL==", exifInterface.getAttribute(ExifInterface.TAG_MODEL).toString())
        Log.e("TAG_SOFTWARE==", exifInterface.getAttribute(ExifInterface.TAG_SOFTWARE).toString())
        Log.e("TAG_DATETIME==", exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString())
        Log.e("TAG_DEVICE_SETTING_DESCRIPTION==", exifInterface.getAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION).toString())
        Log.e("TAG_GPS_DATESTAMP==", exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP).toString())

        Log.e("TAG_GPS_IMG_DIRECTION==", exifInterface.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION).toString())
        Log.e("TAG_GPS_IMG_DIRECTION_REF==", exifInterface.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF).toString())
        Log.e("TAG_GPS_STATUS==", exifInterface.getAttribute(ExifInterface.TAG_GPS_STATUS).toString())

        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        Log.e("orientation==", exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).toString())
        val deviceOrientation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0 // Default to 0 degrees for undefined or unsupported orientation
        }
        Log.e("_MyCustomTag==", exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT).toString())
    }
    private fun setMetadataToImage(currentPhotoPath: String) {
        val exifInterface = ExifInterface(currentPhotoPath)

        val latLongObj = exifInterface.latLong
        if(latLongObj != null){
            Log.e("latLongObj[0]==", latLongObj[0].toString())
            Log.e("latLongObj[0]==", latLongObj[1].toString())
//            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latLongObj[0].toString())
//            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, latLongObj[0].toString())
        } else {
            Log.e("latLong==", "null")
        }

        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, getSampleJsonData().toString())

        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "Custom Make Model")
        exifInterface.setAttribute(ExifInterface.TAG_MODEL, "Custom "+Build.MANUFACTURER + " " + Build.MODEL)
        exifInterface.setAttribute("TakenByDevice", "Redmi5g")
        exifInterface.setAttribute("ImageDescription", "Custom Your image description")
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

    private fun getFilePathAndSaveItToDownloadFolder() {
        val directoryPath = applicationContext.filesDir
        val directory = File(directoryPath, "")

        if (directory.exists() && directory.isDirectory) {
            val imageFiles = directory.listFiles { file ->
                file.isFile && file.extension.toLowerCase() in listOf("jpg", "jpeg", "png", "gif")
            }
            if (!imageFiles.isNullOrEmpty()) {
                val zipFileName = "images.zip"
                val zipFilePath = File(directory, zipFileName)

                ZipFileUtils.makeZipFile(zipFilePath, imageFiles)
                try {
                    ZipFileUtils.saveZipFileToDownloads(
                        applicationContext,
                        zipFilePath.toUri(),
                        zipFileName
                    )
                } catch (e:Exception) {
                    Log.e("Exception==", e.message.toString())
                }
            } else {
                Log.e("==", "empty file")
            }
        } else {
            Log.e("==", "no files")
        }
    }

    private fun doubleToRationalString(value: Double): String? {
        val degrees = value.toLong()
        val minutesSeconds = (value - degrees) * 60.0
        val minutes = minutesSeconds.toLong()
        val seconds = Math.round((minutesSeconds - minutes) * 60.0)
        return "$degrees/1,$minutes/1,$seconds/1"
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.e("onLocationChanged==", "Latitude: $latitude, Longitude: $longitude")
            }

            override fun onProviderDisabled(provider: String) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun getLocationUsingFusedProvider() {
        if (isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
                return
            } else {
                val locationRequest = LocationRequest().setInterval(5000).setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                mFusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)
                            Log.e("locationResult",locationResult.toString())
                            for (location in locationResult.locations) {
                                Log.e("location.latitude",location.latitude.toString())
                                Log.e("location.longitude",location.longitude.toString())
                            }
                            // Things don't end here
                            // You may also update the location on your web app
                        }
                    },
                    Looper.getMainLooper()
                )
            }
        } else {
            Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun shareImage() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/jpeg"

        // Get the file URI using FileProvider
        val fileUri = FileProvider.getUriForFile(this, "$packageName.provider", File(currentPhotoPath))

        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

        // Grant permission to the receiving app
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    override fun onResume() {
        super.onResume()
//        getLocation()
        getLocationUsingFusedProvider()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.removeUpdates(locationListener)
        }
    }

    private fun getSampleJsonData(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", 1)
        jsonObject.put("title", "title")
        return jsonObject
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

}