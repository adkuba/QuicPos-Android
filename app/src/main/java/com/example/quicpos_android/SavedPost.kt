package com.example.quicpos_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SavedPost : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_post)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Post"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}