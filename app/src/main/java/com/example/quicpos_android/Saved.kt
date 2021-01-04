package com.example.quicpos_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.GetViewerPostQuery
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Set
import kotlin.collections.forEach
import kotlin.collections.toTypedArray

object Memory {
    var posts = ArrayList<Post>()
    var currentPostID: String? = null
    var userID = 0
    var sharedPref: SharedPreferences? = null
}

class Saved : AppCompatActivity() {

    private var sharedPref: SharedPreferences? = null
    private var postsNumber = 0
    private var postsTexts = ArrayList<String>()
    private var postsLinks = ArrayList<String>()

    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl("https://www.api.quicpos.com/query")
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        sharedPref = getSharedPreferences("QUICPOS", Context.MODE_PRIVATE)
        val postsids = sharedPref?.getStringSet(getString(R.string.myposts), HashSet<String>())
        postsNumber = postsids?.size!!

        val postsNumberText: TextView = findViewById(R.id.posts_number)
        val postsText = "$postsNumber posts"
        postsNumberText.text = postsText
        getPosts(postsids)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Saved"

        val listView: ListView = findViewById(R.id.saved_list)
        listView.setOnItemClickListener { adapterView, _, position, _ ->
            val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            Memory.currentPostID = Memory.posts[itemIdAtPos.toInt()].ID
            val intent = Intent(this, SavedPost::class.java)
            intent.putExtra("POST_INDEX", itemIdAtPos.toInt())
            startActivity(intent)
        }
    }

    private fun getPosts(postsids: Set<String>) {
        lifecycleScope.launch {
            var counter = 0
            postsids.forEach {
                var existingPost: Post? = null
                Memory.posts.forEach PostLoop@{ post ->
                    val objectID = post.ID?.split("\"")
                    if (objectID?.get(1) ?: "" == it) {
                        existingPost = post
                        return@PostLoop
                    }
                }
                if (existingPost == null){
                    val response = try {
                        apolloClient.query(GetViewerPostQuery(id = it))
                                .await()
                    } catch (e: ApolloException){
                        displayAlert("Error!", e.localizedMessage ?: "Can't execute post get query.")
                        return@launch
                    }

                    val postResponse = response.data?.viewerPost
                    if (postResponse == null){
                        displayAlert("Error!", "Post response is empty")
                        return@launch
                    }
                    if (response.hasErrors()){
                        displayAlert("Error!", response.errors?.map { error -> error.message }?.joinToString { "\n" } ?: "Post get response has errors.")
                        return@launch
                    }

                    //save to array
                    val post = Post(text = "Loading...")
                    post.ID = postResponse.iD
                    post.text = postResponse.text
                    post.userid = postResponse.userId
                    post.image = postResponse.image
                    post.blocked = postResponse.blocked
                    post.shares = postResponse.shares
                    post.views = postResponse.views
                    post.creationTime = postResponse.creationTime

                    if (!postResponse.blocked){
                        counter++
                        postsTexts.add(postResponse.text)
                        postsLinks.add("https://www.quicpos.com/stats/$it")
                        Memory.posts.add(post)
                    }

                } else {
                    if (!existingPost?.blocked!!){
                        counter++
                        postsTexts.add(existingPost?.text!!)
                        postsLinks.add("https://www.quicpos.com/stats/" + existingPost?.ID!!.split("\"")[1])
                    }
                }

                val savedListAdapter = SavedListAdapter(this@Saved, postsTexts.toTypedArray(), postsLinks.toTypedArray())
                val listView: ListView = findViewById(R.id.saved_list)
                listView.adapter = savedListAdapter
            }
            updateNumber(counter)
        }
    }

    private fun displayAlert(title: String, message: String){
        this@Saved.runOnUiThread {
            val builder = AlertDialog.Builder(this@Saved)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Ok", null)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun updateNumber(newNumber: Int){
        this@Saved.runOnUiThread {
            postsNumber = newNumber
            val postsNumberText: TextView = findViewById(R.id.posts_number)
            val postsText = "$postsNumber posts"
            postsNumberText.text = postsText
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}