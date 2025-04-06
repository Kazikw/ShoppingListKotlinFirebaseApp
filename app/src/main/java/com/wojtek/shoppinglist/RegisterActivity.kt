package com.wojtek.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private val database: DatabaseReference =
        FirebaseDatabase.getInstance("https://shoppinglist-f5a05-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register_layout)
        val registerButton = findViewById<Button>(R.id.register_button)
        registerButton.setOnClickListener {
            val newLogin = findViewById<EditText>(R.id.newUserLogin)
            val newPassword = findViewById<EditText>(R.id.newUserPassword)

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Hasło jest zbyt krótkie", Toast.LENGTH_SHORT).show()
            } else {
                if (newLogin.text.isNotEmpty() && newPassword.text.toString().isNotEmpty()) {
                    Log.d(TAG, "Registering user $newLogin")
                    register(newLogin.text.toString(), newPassword.text.toString())
                } else {
                    Toast.makeText(this, "Wprowadź adres e-mail oraz hasło", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun register(email: String, password: String){
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Registering user $email succeed")
                    val user = FirebaseAuth.getInstance().currentUser
                    saveUserToDatabase(user!!.email!!, user.uid)
                    showShoppingList()
                }
                else {
                    Log.e(TAG, "Registering user $email failed", task.exception)
                    Toast.makeText(this, "Adres $email jest użyty", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showShoppingList() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        finish()
    }

    private fun saveUserToDatabase(email: String, uuid: String) {
        val entry = UserEntry(email)
        database.child("/users").child(uuid).push()
            .setValue(entry, object: DatabaseReference.CompletionListener {
                override fun onComplete(
                    databaseError: DatabaseError?,
                    databaseReference: DatabaseReference
                ) {
                    if (databaseError == null) {
                        Log.d(TAG, "Successfully added item $entry to database")
                    } else {
                        Log.e(TAG, "Adding $entry to database failed", databaseError.toException())
                    }
                }
            })
    }

    data class UserEntry(val email: String = "")

    companion object {
        const val TAG = "RegisterActivity"
    }
}
