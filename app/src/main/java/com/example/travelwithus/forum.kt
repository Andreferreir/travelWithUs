package com.example.travelwithus

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.time.Instant
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CustomDialogClass(context: Context) : Dialog(context) {
    val db = Firebase.firestore
    init {

        setCancelable(false)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_forum_add_post)
        val currentUser = FirebaseAuth.getInstance().currentUser!!.uid
        findViewById<Button>(R.id.btn_yes).setOnClickListener{
            var country = findViewById<TextView>(R.id.tvBody)
            var city =findViewById<TextView>(R.id.tvTitle)
            var post =findViewById<TextView>(R.id.tvpost)

            val Apost = hashMapOf(
                "idUserPosted" to currentUser,
                "place" to country,
                "post" to post,
                "createdAt" to Timestamp.from(Instant.now())
            )

            val newPost = db.collection("Forum").add(Apost)
            newPost.addOnCompleteListener {
                Toast.makeText(context,"Novo post adicionado",Toast.LENGTH_LONG).show()
            }
        }
    }
}