package com.example.quicpos_android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.ReportMutation
import com.example.ViewMutation

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PostFragment : Fragment() {

    val postIDModel: PostIDViewModel by activityViewModels()
    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl("https://www.api.quicpos.com/query")
            .build()

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
            println(postIDModel.getPostID())
            reportPost()
        }
    }

    private fun reportPost(){
        if (postIDModel.getPostID() != null) {
            val objectID = postIDModel.getPostID()!!.split("\"")

            apolloClient
                    .mutate(ReportMutation(userID = postIDModel.getUserID() ?: -1, postID = objectID[1]))
                    .enqueue(object: ApolloCall.Callback<ReportMutation.Data>() {
                        override fun onFailure(e: ApolloException) {
                            displayAlert("Error!", message = e.localizedMessage ?: "Can't execute mutation.")
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