package com.example.travelwithus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class signOutPopUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_out_pop_up)
        findViewById<Button>(R.id.ok_button).setOnClickListener{
            FirebaseAuth.getInstance().currentUser!!.delete()

            val user = Firebase.auth.currentUser!!
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(applicationContext,"user deleted",Toast.LENGTH_LONG)
                    }
                }
            val intent = Intent(this, logIn::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.cancel_button).setOnClickListener{
            finish()
        }
    }
}