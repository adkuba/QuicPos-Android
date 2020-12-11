package com.example.quicpos_android

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels

class Saved : AppCompatActivity() {

    val postIDModel: PostIDViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        postIDModel.changeUserID(null)

        supportActionBar?.title = "Saved"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val text = arrayOf(
                "Post1",
                "Post2"
        )
        val link = arrayOf(
                "https://www.google.com",
                "https://www.quicpos.com"
        )

        val savedListAdapter = SavedListAdapter(this, text, link)
        val listView: ListView = findViewById(R.id.saved_list)
        listView.adapter = savedListAdapter

        listView.setOnItemClickListener(){ adapterView, _, position, _ ->
            val itemAtPos = adapterView.getItemAtPosition(position)
            val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            println("Clicked:$itemAtPos id: $itemIdAtPos")

            val intent = Intent(this, SavedPost::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}