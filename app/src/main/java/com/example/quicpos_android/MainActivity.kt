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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.exception.ApolloException
import com.example.GetPostQuery
import com.example.GetUserQuery
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    var mode = "NORMAL"
    private val apolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://api.quicpos.com/query")
        .build()
    private val sharedPref: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
    private val defaultUserID = resources.getInteger(R.integer.default_userid)
    var userID = sharedPref.getInt(getString(R.string.saved_userid), defaultUserID)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

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

        //getUserID and then posts or only posts if userID exists
        if (userID == 0){
            getUser()
        } else {
            getPost()
        }
    }


    fun getPost(){
        println("USERID: $userID")
        lifecycleScope.launch {
            //TODO
            //save to post1 or post2
        }
    }

    private fun getUser(){
        lifecycleScope.launch {
            val response = try {
                apolloClient.query(GetUserQuery("kuba")).await()
            } catch (e: ApolloException) {
                println("Apollo error!")
                return@launch
            }

            val userid = response.data?.createUser
            if (userid == null || response.hasErrors()){
                println("Apollo error2!")
                return@launch
            }

            //save to store and var
            userID = userid
            with(sharedPref.edit()) {
                putInt(getString(R.string.saved_userid), userid)
                apply()
            }
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
