package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val originalPrice: Double?,
    val imageUrl: String,
    val category: String,
    val rating: Float,
    val quantityAvailable: Int,
    val storeName: String = "Beast Sports Official",
    val reviewsCount: Int = 18
)

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String
)

@Entity(tableName = "cart_items")
data class DbCartItem(
    @PrimaryKey val id: String, // Combination of productId_size
    val productId: String,
    val title: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int,
    val size: String
)

@Entity(tableName = "coupons")
data class DbCoupon(
    @PrimaryKey val code: String,
    val discountPercent: Int,
    val title: String,
    val status: String // "Claimed" or "Unclaimed"
)

@Entity(tableName = "orders")
data class DbOrder(
    @PrimaryKey val id: String,
    val itemsSummary: String,
    val totalPrice: Double,
    val date: String,
    val screenshotPath: String?, // For JazzCash/EasyPaisa manual payment screenshot
    val status: String, // "Verified", "Processing", "Shipped"
    val referralUsed: String?
)

@Entity(tableName = "reviews")
data class DbReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val username: String,
    val rating: Float,
    val comment: String,
    val imagePath: String? = null,
    val date: String
)

@Entity(tableName = "favorites")
data class DbFavorite(
    @PrimaryKey val productId: String
)

@Entity(tableName = "stock_alerts")
data class DbStockAlert(
    @PrimaryKey val id: String, // Combination of userId/email_productId
    val email: String,
    val productId: String
)

data class ReferralInfo(
    val uniqueCode: String,
    val count: Int,
    val loyaltyPoints: Int
)
