package com.example.quicpos_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.DeletePostMutation
import com.example.ReportMutation
import java.net.URI

class SavedListAdapter(private val context: Activity, private var text: Array<String>, private var link: Array<String>, private var owner: Array<Boolean>) : ArrayAdapter<String>(context, R.layout.post_mini, text) {

    private val apolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://www.api.quicpos.com/query")
        .build()

    private val appVariables = AppVariables()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var listItem: View? = convertView
        if (listItem == null){
            listItem = LayoutInflater.from(context).inflate(R.layout.post_mini, parent, false)
        }

        val miniText = listItem?.findViewById(R.id.mini_text) as TextView
        val miniLink = listItem.findViewById(R.id.mini_link) as TextView
        val delete = listItem.findViewById(R.id.delete) as TextView
        miniLink.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link[position])))
        }

        delete.setOnClickListener {
            val splited = link[position].split("/")
            deletePost(splited[splited.size-1])
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

    fun deletePost(objectID: String){
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
                        displayAlert("Deleted", message = "Your post has been deleted! Refresh the application.")
                    }
                }
            })
    }

    private fun displayAlert(title: String, message: String){
        context.runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Ok", null)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }
}