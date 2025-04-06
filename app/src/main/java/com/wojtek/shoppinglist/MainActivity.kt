package com.wojtek.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.login_layout)

        val userInput = findViewById<EditText>(R.id.editTextUsername)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = userInput.text.toString()
            val password = passwordInput.text.toString()
            login(username, password);
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            startShoppingListActivity()
        }
    }

    private fun startShoppingListActivity() {
        val intent = Intent(this, ShoppingListActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.register -> startActivity(Intent(this, RegisterActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                baseContext,
                "Logowanie nie powiodło się",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Pomyślnie zalogowano użytkownika $email")
                    startShoppingListActivity()
                } else {
                    Log.w(TAG, "Logowanie nie powiodło się", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Niepoprawne dane logowania",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
    companion object {
        const val TAG = "MainnActivity"
    }
}