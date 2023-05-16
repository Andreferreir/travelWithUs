package com.example.travelwithus

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.sql.Timestamp
import java.time.Instant
import java.util.*


class settings : AppCompatActivity() {
    private val GALLERY_REQUEST_CODE = 13
    private val CAMERA_REQUEST_CODE = 200
    private var photoReferenceCurrentUser: String? = null
    private lateinit var viewImage: ImageView
    private var currentUser: String? = null
    private var DocId: String? = null

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("settings")

        currentUser = FirebaseAuth.getInstance().currentUser!!.uid

        Log.e("user",Firebase.auth.currentUser!!.uid)
        findViewById<Button>(R.id.ApagarConta).setBackgroundColor(R.color.red)
        viewImage = findViewById(R.id.imageView)

        lifecycleScope.launch{
            updateSettings()
        }

        findViewById<ImageView>(R.id.imageView).setOnClickListener{
            requestPermission(this, GALLERY_REQUEST_CODE)
        }
        findViewById<Button>(R.id.button4).setOnClickListener {
            requestPermission(this, CAMERA_REQUEST_CODE)
        }

        findViewById<Button>(R.id.ApagarConta).setOnClickListener{
            val intent = Intent(this, signOutPopUp::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.novaPass).setOnClickListener{
            lifecycleScope.launch{
                val usersRef = Firebase.firestore.collection("users").whereEqualTo("id",currentUser)
                    .get().await()
                val email = usersRef.documents[0].get("email").toString()
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        findViewById<TextView>(R.id.textoPopup).setText("Redefina a password no email: ${email}")
                        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val popupView = inflater.inflate(R.layout.pass_confirm, null)
                        val wid = LinearLayout.LayoutParams.WRAP_CONTENT
                        val high = LinearLayout.LayoutParams.WRAP_CONTENT
                        val focus= true
                        val popupWindow = PopupWindow(popupView, wid, high, focus)
                        popupView.setOnTouchListener(object : View.OnTouchListener {
                            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                                popupWindow.dismiss()
                                return true
                            }
                        })
                    }
                }
            }
        }

        findViewById<Button>(R.id.clear).setOnClickListener{
            clearImage()
        }

        findViewById<Button>(R.id.firstname).setOnClickListener{
            var firstName = findViewById<EditText>(R.id.newEmail).text
            if (firstName.isNotEmpty()) {
                lifecycleScope.launch {
                    Firebase.firestore.collection("users").document(DocId!!)
                        .update(
                            "firstName",
                            firstName,
                            "updatedAt",
                            Timestamp.from(Instant.now())
                    ).await()
                }
            }
        }

        findViewById<Button>(R.id.firstname).setOnClickListener{
            var lastName = findViewById<EditText>(R.id.newEmail).text
            if (lastName.isNotEmpty()) {
                lifecycleScope.launch {
                    Firebase.firestore.collection("users").document(DocId!!)
                        .update(
                            "lastName",
                            lastName,
                            "updatedAt",
                            Timestamp.from(Instant.now())
                    ).await()
                }
            }
        }

        findViewById<Button>(R.id.newEmail).setOnClickListener{
            var email = findViewById<EditText>(R.id.newEmail).text
            if (email.isNotEmpty()){
                Firebase.auth.currentUser!!.updateEmail(email.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            lifecycleScope.launch {
                                Firebase.firestore.collection("users").document(DocId!!)
                                    .update(
                                        "email",
                                        email,
                                        "updatedAt",
                                        Timestamp.from(Instant.now())).await()
                            }
                        Log.d(TAG, "User email address updated.")
                    }
                }
            }
        }

        findViewById<Button>(R.id.atualizar).setOnClickListener{
            val db = Firebase.firestore
            lifecycleScope.launch {

                if(currentUser != null ){
                    photoReferenceCurrentUser = UUID.randomUUID().toString()

                    uploadImage(photoReferenceCurrentUser + ".jpg")

                    db.collection("users").document(DocId!!)
                        .update(
                            "photoReference",
                            photoReferenceCurrentUser,
                            "updatedAt",
                            Timestamp.from(Instant.now())
                        ).await()

                    Toast.makeText(applicationContext, "UPDATED", Toast.LENGTH_LONG)
                }else{
                    Toast.makeText(applicationContext, "press an user from the list to delete",Toast.LENGTH_LONG)
                }
            }
        }
    }

    suspend fun updateSettings(){
        val currentUser = FirebaseAuth.getInstance().currentUser!!.uid
        val usersRef = Firebase.firestore.collection("users").whereEqualTo("id",currentUser)
            .get().await()
        if(usersRef.documents.isNotEmpty() ){
            photoReferenceCurrentUser = usersRef.documents[0].get("photoReference").toString()
            DocId = usersRef.documents[0].get("DocId").toString()
            val nome = usersRef.documents[0].get("firstName").toString()
            val apelido = usersRef.documents[0].get("lastName").toString()
            val email = usersRef.documents[0].get("email").toString()
            findViewById<TextView>(R.id.firstname).setText(nome)
            findViewById<TextView>(R.id.lastname).setText(apelido)
            findViewById<TextView>(R.id.email).setText(email)
            loadImageFromServer(photoReferenceCurrentUser.toString())
        }
    }

    fun requestPermission(activity: Activity?, requestCode: Int) {
        if (requestCode == GALLERY_REQUEST_CODE) {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }
        if (requestCode == CAMERA_REQUEST_CODE) {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(android.Manifest.permission.CAMERA),
                requestCode
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun uploadImage(fileName: String) {
        val imageBitMap =  viewImage.drawToBitmap()
        val imageData = getFileDataFromDrawable(imageBitMap)
        val postURL = "http://simfctan.atwebpages.com/upload_file.php"
        val request = object : VolleyFileUploadRequest(
            Method.POST,
            postURL,
            Response.Listener {
                println("response is: $it")
            },
            Response.ErrorListener {
                println("error is: $it")
            }
        ) {
            override fun getByteData(): MutableMap<String, FileDataPart> {
                val params = HashMap<String, FileDataPart>()
                params["file"] = FileDataPart(fileName, imageData!!, "jpeg")
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    fun deleteImageFromServer(uuid: String) {
        if(uuid!=="") {
            Thread {
                val mImage: Bitmap?
                val mWebPath = "http://simfctan.atwebpages.com/delete_file.php?id=${uuid}"
                mImage = mLoad(mWebPath)
                var vi = findViewById<ImageView>(R.id.imageView)
                clearImage()
            }.start()
        }
    }

    fun clearImage() {
        var vi = findViewById<ImageView>(R.id.imageView)
        lifecycleScope.launch {
            vi.setImageBitmap(null)
        }
    }

    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun mLoad(string: String): Bitmap? {
        val url: URL = mStringToURL(string)!!
        val connection: HttpURLConnection?
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream: InputStream = connection.inputStream
            val bufferedInputStream = BufferedInputStream(inputStream)
            return BitmapFactory.decodeStream(bufferedInputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun mStringToURL(string: String): URL? {
        try {
            return URL(string)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return null
    }

    fun loadImageFromServer(uuid: String) {
        if(uuid !== "") {
            Thread {
                val mImage: Bitmap?
                val mWebPath = "http://simfctan.atwebpages.com/download_file.php?id=${uuid}"
                mImage = mLoad(mWebPath)
                val vi = findViewById<ImageView>(R.id.imageView)
                lifecycleScope.launch {
                    vi.setImageBitmap(mImage)
                }
            }.start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var granted = true
        if (grantResults.isNotEmpty()) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                }
            }
        }
        if (!granted) {
            Toast.makeText(this,  "Permission Denied", Toast.LENGTH_SHORT).show()
        }
        when ( requestCode ) {
            CAMERA_REQUEST_CODE ->{
                capturePhotoFromCamera()
                return
            }
            GALLERY_REQUEST_CODE -> {
                chooseImageFromGallery()
                return
            }
        }
    }
    private fun chooseImageFromGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent,GALLERY_REQUEST_CODE)
    }

    private fun capturePhotoFromCamera(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent,CAMERA_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("onActivityResult","requestCode: ${requestCode} resultCode: ${resultCode}")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE && data != null){
            viewImage.setImageBitmap(data.extras?.get("data") as Bitmap)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE && data != null){
            val uri: Uri? = data.data
            viewImage.setImageURI(uri)
        }
    }

}