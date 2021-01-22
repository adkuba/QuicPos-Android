package com.example.quicpos_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.CreatePostMutation
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import kotlin.collections.HashSet


class Creator : AppCompatActivity() {

    val PICK_IMAGE = 1
    var mainBitmap: Bitmap? = null
    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl("https://www.api.quicpos.com/query")
            .build()
    private var sharedPref: SharedPreferences? = null
    var userID = ""
    val appVariables = AppVariables()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creator)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPref = getSharedPreferences("QUICPOS", Context.MODE_PRIVATE)
        userID = sharedPref?.getString(getString(R.string.saved_userid), "")!!

        val galleryButton: ImageButton = findViewById(R.id.gallery_button)
        galleryButton.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent, PICK_IMAGE)
        }

        val sendButton: ImageButton = findViewById(R.id.send_button)
        sendButton.setOnClickListener {
            createPost()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_creator, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.rotate_image) {
            if (mainBitmap != null){
                val creatorImage: ImageView = findViewById(R.id.creator_image)
                val rotatedBitmap = mainBitmap?.let { rotateImage(it, 90f) }
                creatorImage.setImageBitmap(rotatedBitmap)
                mainBitmap = rotatedBitmap
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun createPost(){

        var encoded = ""
        val editText: EditText = findViewById(R.id.create_text)
        val text = editText.text.toString()

        if (text != ""){
            if (userID != ""){
                //get image
                if (mainBitmap != null) {
                    val out = ByteArrayOutputStream()
                    mainBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, out)
                    encoded = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray())
                }

                changeLoader()

                apolloClient
                        .mutate(CreatePostMutation(text = text, userId = userID, image = encoded, password = appVariables.password))
                        .enqueue(object: ApolloCall.Callback<CreatePostMutation.Data>() {
                            override fun onFailure(e: ApolloException) {
                                displayAlert(title = "Error", message = e.localizedMessage ?: "Can't execute create post mutation.")
                                changeLoader()
                            }

                            override fun onResponse(response: Response<CreatePostMutation.Data>) {
                                val objectID = response.data?.createPost?.iD?.split("\"")
                                if (objectID != null){
                                    //save to memory
                                    val myPosts = sharedPref?.getStringSet(getString(R.string.myposts), HashSet<String>())
                                    val copyMyPosts = myPosts?.toMutableSet()
                                    copyMyPosts?.add(objectID[1])
                                    val editor = sharedPref?.edit()
                                    editor?.putStringSet(getString(com.example.quicpos_android.R.string.myposts), copyMyPosts)
                                    editor?.apply()
                                    Memory.posts = ArrayList()


                                    //share
                                    val intent = Intent()
                                    intent.action = Intent.ACTION_SEND
                                    intent.putExtra(Intent.EXTRA_TEXT, "https://www.quicpos.com/post/" + objectID[1])
                                    intent.type = "text/plain"
                                    startActivity(Intent.createChooser(intent, "Share post"))
                                } else {
                                    println(response.data?.createPost)
                                    displayAlert(title = "Error", message = "Can't send post!")
                                }
                                changeLoader()
                            }

                        })

            } else {
                displayAlert(title = "Error", message = "Bad userID, reset app?")
            }
        } else {
            displayAlert(title = "Info", message = "Type text!")
        }
    }

    private fun displayAlert(title: String, message: String){
        this@Creator.runOnUiThread {
            val builder = AlertDialog.Builder(this@Creator)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Ok", null)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun changeLoader() {
        this@Creator.runOnUiThread {
            val progressBar: ProgressBar = findViewById(R.id.send_progress)
            val sendButton: ImageButton = findViewById(R.id.send_button)

            if (sendButton.visibility == View.VISIBLE){
                sendButton.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
                sendButton.visibility = View.VISIBLE

                val editText: EditText = findViewById(R.id.create_text)
                editText.text.clear()
                mainBitmap = null
                val imageView: ImageView = findViewById(R.id.creator_image)
                imageView.setImageResource(0)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Error")
                builder.setMessage("Empty data in selected item!")
                builder.setPositiveButton("Ok", null)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
                return
            }
            val inputStream: InputStream = contentResolver.openInputStream(data.data!!)!!
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
            val creatorImage: ImageView = findViewById(R.id.creator_image)
            creatorImage.setImageBitmap(bitmap)
            mainBitmap = bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


}