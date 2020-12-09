package com.example.quicpos_android

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*


class Creator : AppCompatActivity() {

    val PICK_IMAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creator)

        supportActionBar?.title = "Creator"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
        }
    }
}