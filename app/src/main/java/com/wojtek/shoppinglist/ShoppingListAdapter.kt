package com.wojtek.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.wojtek.shoppinglist.model.ShoppingItem
import java.util.LinkedList

class ShoppingListAdapter(private val items: LinkedList<ShoppingItem>) :
    RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    private val database: DatabaseReference =
        FirebaseDatabase.getInstance("https://shoppinglist-f5a05-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val deleteItemButton: Button = itemView.findViewById(R.id.delete_item_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemNameTextView.text = item.name
        holder.deleteItemButton.setOnClickListener {
            database.child("shoppingLists").child(FirebaseAuth.getInstance().currentUser!!.uid).child(item.itemKey!!).removeValue()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

//user@gmail.com
