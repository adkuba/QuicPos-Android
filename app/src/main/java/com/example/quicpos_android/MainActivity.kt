package com.example.quicpos_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.GetPostQuery
import com.example.GetUserQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownServiceException


//post struct
data class Post(
        var ID: String?,
        var text: String,
        var userid: Int?,
        var image: String?,
        var imageBitmap: Bitmap?,
        var shares: Int?,
        var views: Int?,
        var creationTime: String?,
        var loading: Boolean?,
        var blocked: Boolean?,
        var ad: Boolean?
) {
    constructor(text: String) : this(
            ID = null,
            text = text,
            userid = null,
            image = null,
            imageBitmap = null,
            shares = null,
            views = null,
            creationTime = null,
            loading = null,
            blocked = null,
            ad = null
    )
}

//app variables
data class AppVariables(
        val password: String
) {
    constructor() : this(
            password = "kuba"
    )
}


class MainActivity : AppCompatActivity() {

    var mode = "NORMAL"
    private val apolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://www.api.quicpos.com/query")
        .build()
    private var sharedPref: SharedPreferences? = null
    var userID = 0

    var posts = arrayListOf(Post(text = "Loading..."), Post(text = "Loading..."))
    val appVariables = AppVariables()
    var adCounter = -2
    var index = 0

    var startTime = 0L
    var additionTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        //init vars
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        userID = sharedPref?.getInt(getString(R.string.saved_userid), 0)!!

        //saved
        val savedButton: ImageButton = findViewById(R.id.saved_button)
        savedButton.setOnClickListener{
            val intent = Intent(this, Saved::class.java)
            startActivity(intent)
        }

        //mode change
        val privacyButton: ImageButton = findViewById(R.id.privacy_button)
        privacyButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Mode change")
            if (mode == "NORMAL"){
                mode = "PRIVATE"
                builder.setMessage("Going to private mode. Random content, no user data collected.")
                privacyButton.setImageResource(R.drawable.lock)
            } else {
                mode = "NORMAL"
                builder.setMessage("Going to normal mode. Personalized content, user data collected.")
                privacyButton.setImageResource(R.drawable.lock_open)
            }
            builder.setPositiveButton("Ok", null)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        val nextButton: ImageButton = findViewById(R.id.next_button)
        nextButton.setOnClickListener {
            if (index != posts.size-2) {
                index += 1
                if (index == posts.size-2){
                    resumeTimer()
                }
                updatePostFragment()
            }
            else if (posts[posts.size-2].ID != null){
                reportView()
                posts.add(Post(text = "Loading..."))
                if (posts.size > 10){
                    posts.removeAt(0)
                }
                getPost()
                index = posts.size-2
                updatePostFragment()
                startTimer()
            }
        }

        val prevButton: ImageButton = findViewById(R.id.prev_button)
        prevButton.setOnClickListener {
            if (index>0){
                if (index == posts.size-2){
                    pauseTimer()
                }
                index -= 1
                updatePostFragment()
            }
        }

        //getUserID and then posts or only posts if userID exists
        if (userID == 0){
            getUser()
        } else {
            //get 2 posts
            getPost()
            getPost()
        }
    }

    override fun onPause() {
        super.onPause()
        if (index == posts.size-2){
            pauseTimer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (index == posts.size-2){
            resumeTimer()
        }
    }

    private fun reportView() {
        println(stopTimer())
    }

    fun getPost(){
        //println("USERID: $userID")

        var normalMode = true
        if (mode == "PRIVATE") {
            normalMode = false
        }

        var ad = false
        if (adCounter % 20 == 0){
            ad = true
        }

        lifecycleScope.launch {
            val response = try {
                apolloClient.query(GetPostQuery(userID, normalMode, appVariables.password, ad)).await()
            } catch (e: ApolloException) {
                var index = posts.size-1
                if (posts[posts.size-2].ID == null){
                    index = posts.size-2
                }
                posts[index] = Post(text = e.localizedMessage ?: "Error! Error with executing post query.")
                updatePostFragment()
                return@launch
            }

            adCounter += 1

            var index = posts.size-1
            if (posts[posts.size-2].ID == null){
                index = posts.size-2
            }

            val postResponse = response.data?.post
            if (postResponse == null){
                posts[index] = Post(text = "Error! Empty post response.")
                updatePostFragment()
                return@launch
            }
            if (response.hasErrors()) {
                posts[index] = Post(text = response.errors?.map { error -> error.message }?.joinToString { "\n" } ?: "Error! Post get response has errors.")
                updatePostFragment()
                return@launch
            }

            //save to post1 or post2
            posts[index].ID = postResponse.iD
            posts[index].text = postResponse.text
            posts[index].userid = postResponse.userId
            posts[index].image = postResponse.image
            posts[index].shares = postResponse.shares
            posts[index].views = postResponse.views
            posts[index].creationTime = postResponse.creationTime
            posts[index].ad = ad

            //if updating visible post
            if (index == posts.size-2){
                updatePostFragment()
                startTimer()
            }
        }
    }

    private fun startTimer(){
        additionTime = 0
        startTime = SystemClock.elapsedRealtime()
    }

    private fun pauseTimer(){
        additionTime += SystemClock.elapsedRealtime() - startTime
    }

    private fun resumeTimer(){
        startTime = SystemClock.elapsedRealtime()
    }

    private fun stopTimer(): Double{
        val elapsedTime = SystemClock.elapsedRealtime() - startTime + additionTime
        return elapsedTime.toDouble() / 1000
    }

    private fun updatePostFragment(){
        val postUser = findViewById<TextView>(R.id.userid_text)
        val postText = findViewById<TextView>(R.id.post_text)
        val postDate = findViewById<TextView>(R.id.date_text)
        val postStats = findViewById<TextView>(R.id.stats_text)

        var user = getString(R.string.post_user) + (posts[index].userid ?: "0")
        if (posts[index].ad == true){
            user = getString(R.string.post_ad_user) + (posts[index].userid ?: "0")
        }
        val date = posts[index].creationTime?.substring(0, 16) ?: getString(R.string.post_date)
        val stats = (posts[index].views ?: 0).toString() + " views " + (posts[index].shares ?: 0) + " shares"

        setImage()

        if (postUser != null && postText != null && postDate != null && postStats != null){
            postUser.text = user
            postText.text = posts[index].text
            postDate.text = date
            postStats.text = stats
        }
    }

    private fun setImage(){
        if (posts[index].image != "" && posts[index].image != null){

            println("IMAGE PRESENT" + posts[index].image)

            //already downloaded
            if (posts[index].imageBitmap != null){
                println("ALREADY DOWNLOADED")
                val imageView: ImageView = findViewById(R.id.post_image)
                imageView.setImageBitmap(posts[index].imageBitmap)
                return
            }

            //download
            //TODO errors printing
            val result = lifecycleScope.async(Dispatchers.IO){
                try {
                    val url = URL("https://storage.googleapis.com/quicpos-images/" + posts[index].image)
                    return@async BitmapFactory.decodeStream(url.openConnection().getInputStream())
                } catch (e: IOException){
                    println("Error")
                } catch (e: UnknownServiceException){
                    println("Error2")
                } catch (e: MalformedURLException){
                    println("Error3")
                }
            }

            lifecycleScope.launch {
                println("NEED TO DOWNLOAD")
                val savingIndex = index
                //TODO check for null
                val bitmap = result.await() as Bitmap
                if (savingIndex == index){
                    val imageView: ImageView = findViewById(R.id.post_image)
                    imageView.setImageBitmap(bitmap)
                }
                posts[savingIndex].imageBitmap = bitmap
            }
        } else {
            //CLEAR IMAGE
            val imageView: ImageView = findViewById(R.id.post_image)
            imageView.setImageBitmap(null)
        }
    }

    private fun getUser(){
        lifecycleScope.launch {
            val response = try {
                apolloClient.query(GetUserQuery(appVariables.password)).await()
            } catch (e: ApolloException) {
                posts[posts.size-2] = Post(text = e.localizedMessage ?: "Error! Error with executing user query.")
                updatePostFragment()
                return@launch
            }

            val userid = response.data?.createUser
            if (userid == null){
                posts[posts.size-2] = Post(text = "Error! Empty user response.")
                updatePostFragment()
                return@launch
            }
            if (response.hasErrors()){
                posts[posts.size-2] = Post(text = response.errors?.map { error -> error.message }?.joinToString { "\n" } ?: "Error! User get response has errors.")
                updatePostFragment()
                return@launch
            }

            //save to store and var
            userID = userid
            with(sharedPref?.edit()) {
                this?.putInt(getString(R.string.saved_userid), userid)
                this?.apply()
            }
            //get 2 posts
            getPost()
            getPost()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.creator) {
            val intent = Intent(this, Creator::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}

private class DownloadImageTask(var bmImage: ImageView) : AsyncTask<String?, Void?, Bitmap?>() {
    override fun doInBackground(vararg urls: String?): Bitmap? {
        val urldisplay = urls[0]
        var mIcon11: Bitmap? = null
        try {
            val `in`: InputStream = URL(urldisplay).openStream()
            mIcon11 = BitmapFactory.decodeStream(`in`)
        } catch (e: Exception) {
            println(e.message)
            e.printStackTrace()
        }
        return mIcon11
    }

    override fun onPostExecute(result: Bitmap?) {
        bmImage.setImageBitmap(result)
    }

}
