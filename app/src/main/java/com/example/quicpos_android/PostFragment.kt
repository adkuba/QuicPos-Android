package com.example.quicpos_android


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.ReportMutation
import com.example.ShareMutation

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PostFragment : Fragment() {

    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl("https://www.api.quicpos.com/query")
            .build()
    private val appVariables = AppVariables()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_postl, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reportButton: Button = view.findViewById(R.id.report_button)
        reportButton.setOnClickListener {
            //println(postIDModel.getPostID())
            reportPost()
        }

        val shareButton: Button = view.findViewById(R.id.share_button)
        shareButton.setOnClickListener {
            println(Memory.currentPostID)
            sharePost()
        }
    }

    private fun sharePost(){
        if (Memory.currentPostID != null && Memory.userID != ""){
            val objectID = Memory.currentPostID!!.split("\"")

            apolloClient
                    .mutate(ShareMutation(userID = Memory.userID, postID = objectID[1], password = appVariables.password))
                    .enqueue(object: ApolloCall.Callback<ShareMutation.Data>() {
                        override fun onFailure(e: ApolloException) {
                            displayAlert("Error!", message = e.localizedMessage ?: "Can't execute share mutation.")
                        }

                        override fun onResponse(response: Response<ShareMutation.Data>) {
                            if (response.data?.share != true){
                                displayAlert("Error!", message = "Bad share return! Contact us to resolve the issue.")
                            } else {
                                //save post
                                val myPosts = Memory.sharedPref?.getStringSet(getString(R.string.myposts), HashSet<String>())
                                val copyMyPosts = myPosts?.toMutableSet()
                                copyMyPosts?.add(objectID[1])
                                val editor = Memory.sharedPref?.edit()
                                editor?.putStringSet(getString(R.string.myposts), copyMyPosts)
                                editor?.apply()

                                //share
                                val intent = Intent()
                                intent.action = Intent.ACTION_SEND
                                intent.putExtra(Intent.EXTRA_TEXT, "https://www.quicpos.com/post/" + objectID[1])
                                intent.type = "text/plain"
                                startActivity(Intent.createChooser(intent, "Share post"))
                            }
                        }

                    })
        }
    }

    private fun reportPost(){
        if (Memory.currentPostID != null && Memory.userID != "") {
            val objectID = Memory.currentPostID!!.split("\"")

            apolloClient
                    .mutate(ReportMutation(userID = Memory.userID, postID = objectID[1]))
                    .enqueue(object: ApolloCall.Callback<ReportMutation.Data>() {
                        override fun onFailure(e: ApolloException) {
                            displayAlert("Error!", message = e.localizedMessage ?: "Can't execute report mutation.")
                        }

                        override fun onResponse(response: Response<ReportMutation.Data>) {
                            if (response.data?.report != true){
                                displayAlert("Error!", message = "Bad report return! Contact us to resolve the issue.")
                            } else {
                                displayAlert("Report", message = "Thank you! Our team will review this post.")
                            }
                        }
                    })
        }
    }

    private fun displayAlert(title: String, message: String){
        activity?.runOnUiThread {
            val builder = activity?.let { AlertDialog.Builder(it) }
            if (builder != null) {
                builder.setTitle(title)
                builder.setMessage(message)
                builder.setPositiveButton("Ok", null)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        }

    }

}