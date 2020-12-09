package com.example.quicpos_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.exception.ApolloException
import com.example.GetPostQuery
import com.example.GetUserQuery
import kotlinx.coroutines.launch
import org.w3c.dom.Text


//post struct
data class Post(
        var ID: String?,
        var text: String,
        var userid: Int?,
        var image: String?,
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
                updatePostFragment()
            }
            else if (posts[posts.size-2].ID != null){
                posts.add(Post(text = "Loading..."))
                if (posts.size > 10){
                    posts.removeAt(0)
                }
                getPost()
                index = posts.size-2
                updatePostFragment()
            }
        }

        val prevButton: ImageButton = findViewById(R.id.prev_button)
        prevButton.setOnClickListener {
            if (index>0){
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

            if (index == posts.size-2){
                updatePostFragment()
            }
        }
    }

    private fun updatePostFragment(){
        val postUser = findViewById<TextView>(R.id.userid_text)
        val postText = findViewById<TextView>(R.id.post_text)
        val postDate = findViewById<TextView>(R.id.date_text)
        val postStats = findViewById<TextView>(R.id.stats_text)

        val user = getString(R.string.post_user) + (posts[index].userid ?: "0")
        val date = posts[index].creationTime ?: getString(R.string.post_date)
        val stats = (posts[index].views ?: 0).toString() + " views " + (posts[index].shares ?: 0) + " shares"
        postUser.text = user
        postText.text = posts[index].text
        postDate.text = date
        postStats.text = stats
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
