package com.example.quicpos_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.DeletePostMutation

interface OnPostDeleteListener {
    fun onPostDelete(position: Int, id: String)
    fun onPostClick(position: Int)
}

class SavedListAdapter(private val context: Activity, private var text: Array<String>, private var link: Array<String>, private var owner: Array<Boolean>) : ArrayAdapter<String>(context, R.layout.post_mini, text) {

    private val apolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://www.api.quicpos.com/query")
        .build()

    var mListener: OnPostDeleteListener? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var listItem: View? = convertView
        if (listItem == null){
            listItem = LayoutInflater.from(context).inflate(R.layout.post_mini, parent, false)
        }

        val miniText = listItem?.findViewById(R.id.mini_text) as TextView
        val miniLink = listItem.findViewById(R.id.mini_link) as Button
        val delete = listItem.findViewById(R.id.delete) as Button
        miniLink.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link[position])))
        }
        miniText.setOnClickListener {
            mListener?.onPostClick(position)
        }

        delete.setOnClickListener {
            val splited = link[position].split("/")
            mListener?.onPostDelete(position, splited[splited.size-1])
        }

        var linkText = "Stats"
        if (owner[position]){
            linkText = "Promote"
            delete.visibility = View.VISIBLE
        }
        miniText.text = text[position]
        miniLink.text = linkText

        return listItem
    }
}