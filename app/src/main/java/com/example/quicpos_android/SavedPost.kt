package com.example.quicpos_android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownServiceException

class SavedPost : AppCompatActivity() {

    private var postIDX = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_post)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Post"

        postIDX = intent.getIntExtra("POST_INDEX", -1)
    }

    override fun onStart() {
        super.onStart()

        val postUser: TextView = findViewById(R.id.userid_text)
        val postText: TextView = findViewById(R.id.post_text)
        val postDate: TextView = findViewById(R.id.date_text)
        val postStats: TextView = findViewById(R.id.stats_text)
        val postID: TextView = findViewById(R.id.post_id)


        val user = getString(R.string.post_user) + (Memory.posts[postIDX].userid ?: "0")
        val date = Memory.posts[postIDX].creationTime?.substring(0, 16) ?: getString(R.string.post_date)
        val stats = (Memory.posts[postIDX].views ?: 0).toString() + " views " + (Memory.posts[postIDX].shares ?: 0) + " shares"

        setImage()
        postUser.text = user
        postText.text = Memory.posts[postIDX].text
        postDate.text = date
        postStats.text = stats
        postID.text = Memory.posts[postIDX].ID?.substring(10, 34)
    }

    private fun setImage(){
        if (Memory.posts[postIDX].image != "" && Memory.posts[postIDX].image != null){

            //already downloaded
            if (Memory.posts[postIDX].imageBitmap != null){
                val imageView: ImageView = findViewById(R.id.post_image)
                imageView.setImageBitmap(Memory.posts[postIDX].imageBitmap)
                return
            }

            //download
            val result = lifecycleScope.async(Dispatchers.IO){
                try {
                    val url = URL("https://storage.googleapis.com/quicpos-images/" + Memory.posts[postIDX].image)
                    return@async BitmapFactory.decodeStream(url.openConnection().getInputStream())
                } catch (e: IOException){
                    return@async null
                } catch (e: UnknownServiceException){
                    return@async null
                } catch (e: MalformedURLException){
                    return@async null
                }
            }

            lifecycleScope.launch {
                try {
                    val bitmap = result.await() as Bitmap
                    val imageView: ImageView = findViewById(R.id.post_image)
                    imageView.setImageBitmap(bitmap)
                    Memory.posts[postIDX].imageBitmap = bitmap
                } catch (e: Exception){
                    displayAlert(title = "Error!", message = "Can't download post image!")
                }

            }
        } else {
            //CLEAR IMAGE
            val imageView: ImageView = findViewById(R.id.post_image)
            imageView.setImageResource(0)
        }
    }

    private fun displayAlert(title: String, message: String){
        this@SavedPost.runOnUiThread {
            val builder = AlertDialog.Builder(this@SavedPost)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Ok", null)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}