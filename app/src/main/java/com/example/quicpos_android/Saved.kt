package com.example.quicpos_android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.GetViewerPostQuery
import kotlinx.coroutines.launch
import java.util.ArrayList

class Saved : AppCompatActivity() {

    private var sharedPref: SharedPreferences? = null
    private var postsNumber = 0
    private var posts = ArrayList<Post>()
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
        listView.setOnItemClickListener(){ adapterView, _, position, _ ->
            val itemAtPos = adapterView.getItemAtPosition(position)
            val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            println("Clicked:$itemAtPos id: $itemIdAtPos")

            //TODO how to set text in post?

            val intent = Intent(this, SavedPost::class.java)
            startActivity(intent)
        }
    }

    private fun getPosts(postsids: Set<String>) {
        lifecycleScope.launch {
            postsids.forEach {
                val response = try {
                    apolloClient.query(GetViewerPostQuery(id = it))
                            .await()
                } catch (e: ApolloException){
                    //TODO error
                    return@launch
                }

                val postResponse = response.data?.viewerPost
                if (postResponse == null){
                    //TODO error
                    return@launch
                }
                if (response.hasErrors()){
                    //TODO ERROR
                    return@launch
                }

                //save to array
                val post = Post(text = "Loading...")
                post.ID = postResponse.iD
                post.text = postResponse.text
                post.userid = postResponse.userId
                post.image = postResponse.image
                post.shares = postResponse.shares
                post.views = postResponse.views
                post.creationTime = postResponse.creationTime

                postsTexts.add(postResponse.text)
                postsLinks.add("https://www.quicpos.com/stats/$it")
                posts.add(post)

                val savedListAdapter = SavedListAdapter(this@Saved, postsTexts.toTypedArray(), postsLinks.toTypedArray())
                val listView: ListView = findViewById(R.id.saved_list)
                listView.adapter = savedListAdapter
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}