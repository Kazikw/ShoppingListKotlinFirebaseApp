package com.wojtek.shoppinglist.model


// itemKey is key in database
data class ShoppingItem(var itemKey: String? = null, val name: String? = "", val addedByUUID: String? = null, val addedByEmail: String? = null)
