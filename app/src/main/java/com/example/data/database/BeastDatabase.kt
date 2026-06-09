package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.DbCartItem
import com.example.data.model.DbCoupon
import com.example.data.model.DbOrder
import com.example.data.model.DbReview
import com.example.data.model.DbFavorite
import com.example.data.model.DbStockAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface BeastDao {

    // Cart operations
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<DbCartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: DbCartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun removeCartItem(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Coupon operations
    @Query("SELECT * FROM coupons")
    fun getCoupons(): Flow<List<DbCoupon>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<DbCoupon>)

    @Query("UPDATE coupons SET status = :status WHERE code = :code")
    suspend fun updateCouponStatus(code: String, status: String)

    // Order operations
    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getOrders(): Flow<List<DbOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: DbOrder)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String)

    // Review operations
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY date DESC")
    fun getReviewsForProduct(productId: String): Flow<List<DbReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: DbReview)

    // Favorite operations
    @Query("SELECT * FROM favorites")
    fun getFavorites(): Flow<List<DbFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: DbFavorite)

    @Query("DELETE FROM favorites WHERE productId = :productId")
    suspend fun removeFavorite(productId: String)

    // Stock alert operations
    @Query("SELECT * FROM stock_alerts WHERE productId = :productId")
    fun getStockAlertsForProduct(productId: String): Flow<List<DbStockAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addStockAlert(alert: DbStockAlert)
}

@Database(
    entities = [
        DbCartItem::class,
        DbCoupon::class,
        DbOrder::class,
        DbReview::class,
        DbFavorite::class,
        DbStockAlert::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BeastDatabase : RoomDatabase() {
    abstract fun dao(): BeastDao
}
