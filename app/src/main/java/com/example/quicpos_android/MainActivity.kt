package com.example.quicpos_android

import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    var mode = "NORMAL"

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
