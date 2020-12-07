package com.example.quicpos_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.net.URI

class SavedListAdapter(private val context: Activity, private val text: Array<String>, private val link: Array<String>) : ArrayAdapter<String>(context, R.layout.post_mini, text) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var listItem: View? = convertView
        if (listItem == null){
            listItem = LayoutInflater.from(context).inflate(R.layout.post_mini, parent, false)
        }

        val miniText = listItem?.findViewById(R.id.mini_text) as TextView
        val miniLink = listItem.findViewById(R.id.mini_link) as TextView
        miniLink.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link[position])))
        }

        val linkText = "Stats"
        miniText.text = text[position]
        miniLink.text = linkText

        return listItem
    }
}