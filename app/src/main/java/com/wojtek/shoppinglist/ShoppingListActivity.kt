package com.wojtek.shoppinglist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.service.autofill.OnClickAction
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.OnClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.snapshots
import com.wojtek.shoppinglist.model.ShoppingItem
import kotlinx.coroutines.flow.first
import java.util.LinkedList


class ShoppingListActivity : AppCompatActivity() {
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101//test

    val DB_ROOT_NAME = "shoppingLists"
    private val database: DatabaseReference =
        FirebaseDatabase.getInstance("https://shoppinglist-f5a05-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference()

    private lateinit var recyclerView: RecyclerView
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private val allUsers = HashMap<String, RegisterActivity.UserEntry>()

    val shoppingListForCurrentUser = LinkedList<ShoppingItem>()
    var shoppingListForUserDatabaseListener: ChildEventListener? = null


    private fun addShoppingItemToDatabase(
        name: String,
        addedByUUID: String,
        addedByEmail: String,
        targetListUUID: String
    ) {
        val newItem =
            ShoppingItem(name = name, addedByUUID = addedByUUID, addedByEmail = addedByEmail)
        Log.d(TAG, "Adding product $newItem")
        database.child(DB_ROOT_NAME).child(targetListUUID).push()
            .setValue(newItem) { databaseError, databaseReference ->
                if (databaseError == null) {
                    Log.d(TAG, "Successfully added item $newItem to database")
                } else {
                    Log.e(
                        TAG,
                        "Failure during adding item $newItem to database",
                        databaseError.toException()
                    )
                }
            }
    }

    private fun createShoppingListDatabaseListenerForUser(
        context: Context,
        uid: String
    ): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val shoppingItemOpt: ShoppingItem? = snapshot.getValue(ShoppingItem::class.java)
                if (shoppingItemOpt != null) {
                    shoppingItemOpt.itemKey = snapshot.key
                }
                shoppingListForCurrentUser.add(shoppingItemOpt!!)
                shoppingListAdapter.notifyItemInserted(shoppingListForCurrentUser.size - 1)
                if (shoppingItemOpt.addedByUUID != uid) {
                    //sendNotification()
                    sendNotification(shoppingItemOpt.addedByEmail.toString(), shoppingItemOpt.name.toString())
                    /*Toast.makeText(
                        context,
                        "Użytkownik ${shoppingItemOpt.addedByEmail} dodał do Twojej listy nową pozycję: ${shoppingItemOpt.name}",
                        Toast.LENGTH_LONG
                    ).show();
                    database.child(DB_ROOT_NAME).child(uid).child(shoppingItemOpt.itemKey!!)
                        .child("addedByUUID").setValue(uid)*/
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val indexOnShoppingListForCurrentUser =
                    shoppingListForCurrentUser.indexOfFirst { shoppingItem -> shoppingItem.itemKey!! == snapshot.key }
                if (indexOnShoppingListForCurrentUser != -1) {//-1 kiedy blad
                    shoppingListForCurrentUser.removeAt(indexOnShoppingListForCurrentUser)
                    shoppingListAdapter.notifyItemRemoved(indexOnShoppingListForCurrentUser)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
                //Log.ERROR
            }
        }
    }

    private fun setupDatabaseListener(context: Context) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.child("users").children
                for (user in users) {
                    allUsers.put(
                        user.key!!,
                        user.children.first().getValue(RegisterActivity.UserEntry::class.java)!!
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error during getting data from database", error.toException())
            }
        })
        val user = FirebaseAuth.getInstance().currentUser!!
        shoppingListForUserDatabaseListener =
            createShoppingListDatabaseListenerForUser(context, user.uid)
        database.child(DB_ROOT_NAME).child(user.uid)
            .addChildEventListener(shoppingListForUserDatabaseListener!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shopping_list)
        shoppingListAdapter = ShoppingListAdapter(shoppingListForCurrentUser)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = shoppingListAdapter
        setupDatabaseListener(this)
        createNotificationChannel()//test
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_shopping_list, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_position_other -> showAddProductToUserDialog()
            R.id.add_position -> showAddProductDialog()
            R.id.logout -> logout();
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        Log.d(TAG, "Logging out")
        val user = FirebaseAuth.getInstance().currentUser!!
        database.child(DB_ROOT_NAME).child(user.uid)
            .removeEventListener(shoppingListForUserDatabaseListener!!)
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        finish()
    }

    private fun showAddProductToUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.add_position_dialog_o, null)

        val spinnerOptions = dialogView.findViewById<Spinner>(R.id.user)
        val editTextInput = dialogView.findViewById<EditText>(R.id.product_name)

        val options = ArrayList<String>()
        for (user in allUsers.values) {
            options.add(user.email)
        }

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            options
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOptions.adapter = adapter
        val user = FirebaseAuth.getInstance().currentUser!!
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setTitle("Wprowadź dane")
            .setPositiveButton("OK") { dialog, which ->
                val selectedOption = spinnerOptions.selectedItem.toString()
                val userInput = editTextInput.text.toString()
                val uuidOfListForNewIem = findKeyByValue(allUsers, selectedOption)
                addShoppingItemToDatabase(
                    name = userInput,
                    addedByUUID = user.uid,
                    targetListUUID = uuidOfListForNewIem!!,
                    addedByEmail = selectedOption
                )
            }
            .setNegativeButton(
                "Anuluj"
            ) { dialog, which -> dialog.cancel() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun findKeyByValue(
        map: Map<String, RegisterActivity.UserEntry>,
        targetValue: String
    ): String? {
        return map.entries.find { it.value.email == targetValue }?.key
    }

    private fun showAddProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.add_position_dialog, null)

        val editTextInput = dialogView.findViewById<EditText>(R.id.product)
        val user = FirebaseAuth.getInstance().currentUser!!
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setTitle("Wprowadź dane")
            .setPositiveButton("OK") { dialog, which ->
                val userInput = editTextInput.text.toString()
                addShoppingItemToDatabase(userInput, user.uid,  user.email!!, user.uid)
            }
            .setNegativeButton(
                "Anuluj"
            ) { dialog, which -> dialog.cancel() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    companion object {
        const val TAG = "ShoppingListActivity"
    }

private fun createNotificationChannel(){//test
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        val name = "Notification title"
        val descriptionText = "Notification descriptionText"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID,name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
    private fun Intencja(): PendingIntent? {
        val intent = Intent(this, ShoppingListActivity::class.java)
        startActivity(intent)
        return null
    }
private fun sendNotification(UserName: String, Product: String){
    /*val intent = Intent(this, ShoppingListActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }*/
    //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
    //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, )
    //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0 , Intent(this, ShoppingListActivity::class.java) , PendingIntent.FLAG_UPDATE_CURRENT)
   // val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    //val pendingIntent: PendingIntent = PendingIntent.getActivity(this,100, Toast.makeText(this, "", Toast.LENGTH_LONG).show())
    val bitmap: Bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.notification)
    val bitmapLatgeIcon: Bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.notification)

    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.baseline_notifications_24)
        .setContentTitle("Uzytkownik: "+ UserName +" dodal nowe produkty ")//tytul
        .setContentText(Product)//opis
        //.setContentIntent(Intencja())
        //.setLargeIcon(bitmap)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    with(NotificationManagerCompat.from(this)){
        notify(notificationId, builder.build())
    }
}

}