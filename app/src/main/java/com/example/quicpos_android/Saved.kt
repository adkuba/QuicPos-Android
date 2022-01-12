package com.example.quicpos_android

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.DeletePostMutation
import com.example.GetViewerPostQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownServiceException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Set
import kotlin.collections.forEach
import kotlin.collections.toTypedArray

object Memory {
    var posts = ArrayList<Post>()
    var currentPostID: String? = null
    var currentPostUser: String? = null
    var userID = ""
    var sharedPref: SharedPreferences? = null
}

class Saved : AppCompatActivity(), OnPostDeleteListener {

    private var sharedPref: SharedPreferences? = null
    private var postsNumber = 0
    private var postsTexts = ArrayList<String>()
    private var postsLinks = ArrayList<String>()
    private var postsOwner = ArrayList<Boolean>()
    private var postsImages = ArrayList<Bitmap?>()
    private var savedListAdapter: SavedListAdapter? = null
    private var postsids = HashSet<String>()
    private var appVariables = AppVariables()

    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl("http://akuba.pl/api/quicpos/query")
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        sharedPref = getSharedPreferences("QUICPOS", Context.MODE_PRIVATE)
        postsids = sharedPref?.getStringSet(getString(R.string.myposts), HashSet<String>()) as HashSet<String>
        postsNumber = postsids.size

        val postsNumberText: TextView = findViewById(R.id.posts_number)
        val postsText = "$postsNumber posts"
        postsNumberText.text = postsText
        getPosts(postsids)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shared"
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

                    if (postResponse.userId != ""){
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
                            if (postResponse.userId == Memory.userID){
                                postsLinks.add("https://www.quicpos.com/pay/$it")
                                postsImages.add(getImage(postResponse.image))
                                postsOwner.add(true)
                            } else {
                                postsLinks.add("https://www.quicpos.com/stats/$it")
                                postsImages.add(getImage(postResponse.image))
                                postsOwner.add(false)
                            }
                            Memory.posts.add(post)
                        }
                    }
                } else {
                    if (!existingPost?.blocked!!){
                        counter++
                        postsTexts.add(existingPost?.text!!)
                        if (existingPost?.userid == Memory.userID){
                            postsLinks.add("https://www.quicpos.com/pay/" + existingPost?.ID!!.split("\"")[1])
                            postsImages.add(getImage(existingPost?.image ?: ""))
                            postsOwner.add(true)
                        } else {
                            postsLinks.add("https://www.quicpos.com/stats/" + existingPost?.ID!!.split("\"")[1])
                            postsImages.add(getImage(existingPost?.image ?: ""))
                            postsOwner.add(false)
                        }
                    }
                }

                savedListAdapter = SavedListAdapter(this@Saved, postsTexts.toTypedArray(), postsLinks.toTypedArray(), postsOwner.toTypedArray(), postsImages.toTypedArray())
                savedListAdapter!!.mListener = this@Saved
                val listView: ListView = findViewById(R.id.saved_list)
                listView.adapter = savedListAdapter!!
            }
            updateNumber(counter)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_shared, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.user_info) {
            displayAlert("Info", "${Memory.userID} your ID. Save it to delete posts after uninstall. Contact: admin@tline.site")
        }
        return super.onOptionsItemSelected(item)
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

    fun deletePost(position: Int, id: String){
        this@Saved.runOnUiThread {
            postsTexts.removeAt(position)
            postsLinks.removeAt(position)
            postsImages.removeAt(position)
            postsOwner.removeAt(position)
            savedListAdapter = SavedListAdapter(this@Saved, postsTexts.toTypedArray(), postsLinks.toTypedArray(), postsOwner.toTypedArray(), postsImages.toTypedArray())
            savedListAdapter!!.mListener = this@Saved
            val listView: ListView = findViewById(R.id.saved_list)
            listView.adapter = savedListAdapter!!
            if (position < Memory.posts.size){
                Memory.posts.removeAt(position)
            }

            val copyMyPosts = postsids.toMutableSet()
            copyMyPosts.remove(id)
            val editor = sharedPref?.edit()
            editor?.putStringSet(getString(R.string.myposts), copyMyPosts)
            editor?.apply()
            postsids = copyMyPosts.toHashSet()
        }
        updateNumber(postsNumber-1)
        displayAlert("Deleted", message = "Your post has been deleted!")
    }

    private suspend fun getImage(imageID: String): Bitmap?{

        if (imageID != "") {
            //download
            val result = lifecycleScope.async(Dispatchers.IO){
                try {
                    val url = URL("https://storage.googleapis.com/quicpos-images/$imageID")
                    return@async BitmapFactory.decodeStream(url.openConnection().getInputStream())
                } catch (e: IOException){
                    return@async null
                } catch (e: UnknownServiceException){
                    return@async null
                } catch (e: MalformedURLException){
                    return@async null
                }
            }

            try {
                return result.await() as Bitmap
            } catch (e: Exception){
                displayAlert(title = "Error!", message = "Can't download post image!")
            }
        }
        return null
    }

    fun deletePostMutation(objectID: String, position: Int) {
        apolloClient
                .mutate(DeletePostMutation(userID = Memory.userID, postID = objectID, password = appVariables.password))
                .enqueue(object: ApolloCall.Callback<DeletePostMutation.Data>() {
                    override fun onFailure(e: ApolloException) {
                        displayAlert("Error!", message = e.localizedMessage ?: "Can't execute delete mutation.")
                    }

                    override fun onResponse(response: Response<DeletePostMutation.Data>) {
                        if (response.data?.removePost != true){
                            displayAlert("Error!", message = "Bad delete return! Contact us to resolve the issue.")
                        } else {
                            deletePost(position, objectID)
                        }
                    }
                })
    }

    override fun onPostDelete(position: Int, id: String) {
        this@Saved.runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete")
            builder.setMessage("Do you really want to delete this post?")
            builder.setNegativeButton("No", null)
            builder.setPositiveButton("Yes", DialogInterface.OnClickListener{ _, _ ->
                deletePostMutation(id, position)
            })
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    override fun onPostClick(position: Int) {
        this@Saved.runOnUiThread {
            Memory.currentPostID = Memory.posts[position].ID
            val intent = Intent(this, SavedPost::class.java)
            intent.putExtra("POST_INDEX", position)
            startActivity(intent)
        }
    }
}