package com.example.data.repository

import android.util.Log
import com.example.data.database.BeastDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class BeastRepository(
    private val dao: BeastDao,
    private val shopifyToken: String = "",
    private val shopifyUrl: String = "",
    private val supabaseKey: String = "",
    private val supabaseUrl: String = ""
) {
    private val tag = "BeastRepository"
    private val okHttpClient = OkHttpClient()

    // Seed data
    private val initialProducts = listOf(
        Product(
            id = "shopify_p_1",
            title = "Grey Casual shoe",
            description = "Crafted for effortless daily motion. Made with breathable lightweight fabric, synthetic leather collar trim, and responsive athletic cushioning for all-day comfort.",
            price = 120.0,
            originalPrice = 160.0,
            imageUrl = "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&q=80", // Premium sneaker held look alike
            category = "Men's footwears",
            rating = 4.8f,
            quantityAvailable = 15,
            reviewsCount = 18
        ),
        Product(
            id = "shopify_p_2",
            title = "Men's outfit",
            description = "Minimalist performance bomber jacket designed for cool morning routines. Breathable windblock layer, warm insulating lining, and discrete security pocket placement.",
            price = 210.0,
            originalPrice = 280.0,
            imageUrl = "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=800&q=80", // Model in classy jacket
            category = "Men's outfit",
            rating = 4.9f,
            quantityAvailable = 8,
            reviewsCount = 24
        ),
        Product(
            id = "shopify_p_3",
            title = "Woman's outfit",
            description = "Comfort-stretch dynamic athletic top paired with performance tights. Tailored seams, sweat-wicking knit material, and smooth high-waisted bands for natural training flexibility.",
            price = 145.0,
            originalPrice = null,
            imageUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=800&q=80", // Woman outfit model
            category = "woman's outfit",
            rating = 4.7f,
            quantityAvailable = 20,
            reviewsCount = 12
        ),
        Product(
            id = "shopify_p_4",
            title = "Elite Carbon Shoe",
            description = "LIMIT OVERRIDE. Our standard-setting competition shoe with responsive carbon-fiber propulsion plate and ultra-grade grip rubber. (CURRENTLY OUT OF STOCK - Trigger standard restock notification alert)",
            price = 260.0,
            originalPrice = 310.0,
            imageUrl = "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=800&q=80", // Out of stock carbon shoes
            category = "Men's footwears",
            rating = 5.0f,
            quantityAvailable = 0, // Out of stock on purpose
            reviewsCount = 56
        ),
        Product(
            id = "shopify_p_5",
            title = "Gym Performance Tee",
            description = "Ultralight aerodynamic fit training tee with anti-odor silver ion construction. Quick dry micro-weave fabric layout to sustain rigorous intensity conditions.",
            price = 45.0,
            originalPrice = 60.0,
            imageUrl = "https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=800&q=80",
            category = "Men's outfit",
            rating = 4.5f,
            quantityAvailable = 35,
            reviewsCount = 9
        )
    )

    private val categories = listOf(
        Category("cat_1", "Men's outfit", "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=300&q=80"),
        Category("cat_2", "woman's outfit", "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=300&q=80"),
        Category("cat_3", "Men's footwears", "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=300&q=80")
    )

    private val mutableProducts = MutableStateFlow<List<Product>>(initialProducts)
    val productsFlow: Flow<List<Product>> = mutableProducts.asStateFlow()

    fun getCategories(): List<Category> = categories

    // 1. Shopify Storefront GraphQL Query Integration
    // Real-world Shopify GraphQL Query structures ready for production connecting:
    suspend fun fetchProductsFromShopify(): Boolean = withContext(Dispatchers.IO) {
        if (shopifyToken.isEmpty() || shopifyUrl.isEmpty()) {
            Log.d(tag, "Shopify token/URL not provided. Displaying pre-seeded premium local catalog.")
            return@withContext true
        }

        val graphqlQuery = """
            query {
              products(first: 20) {
                edges {
                  node {
                    id
                    title
                    description
                    images(first: 1) {
                      edges {
                        node {
                          url
                        }
                      }
                    }
                    priceRange {
                      minVariantPrice {
                        amount
                        currencyCode
                      }
                    }
                    compareAtPriceRange {
                      minVariantPrice {
                        amount
                      }
                    }
                    totalInventory
                  }
                }
              }
            }
        """.trimIndent()

        try {
            val jsonBody = JSONObject().put("query", graphqlQuery)
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(shopifyUrl)
                .addHeader("X-Shopify-Storefront-Access-Token", shopifyToken)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    Log.d(tag, "Shopify GraphQL Response: $responseStr")
                    // Parse response & map dynamically to Products list
                    val parsed = parseShopifyGraphQLResponse(responseStr)
                    if (parsed.isNotEmpty()) {
                        mutableProducts.value = parsed
                        return@withContext true
                    }
                } else {
                    Log.e(tag, "Shopify API Error: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Shopify client: ${e.message}", e)
        }
        return@withContext false
    }

    private fun parseShopifyGraphQLResponse(responseStr: String): List<Product> {
        val list = mutableListOf<Product>()
        try {
            val obj = JSONObject(responseStr)
            val data = obj.optJSONObject("data") ?: return emptyList()
            val productsNode = data.optJSONObject("products") ?: return emptyList()
            val edges = productsNode.optJSONArray("edges") ?: return emptyList()

            for (i in 0 until edges.length()) {
                val edge = edges.getJSONObject(i)
                val node = edge.getJSONObject("node")

                val id = node.getString("id")
                val title = node.getString("title")
                val desc = node.optString("description", "")
                
                val imagesNode = node.optJSONObject("images")
                val imgEdges = imagesNode?.optJSONArray("edges")
                val imageUrl = if (imgEdges != null && imgEdges.length() > 0) {
                    imgEdges.getJSONObject(0).getJSONObject("node").getString("url")
                } else {
                    "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&q=80"
                }

                val priceRange = node.optJSONObject("priceRange")
                val minPriceObj = priceRange?.optJSONObject("minVariantPrice")
                val price = minPriceObj?.optDouble("amount", 120.0) ?: 120.0

                val compareAt = node.optJSONObject("compareAtPriceRange")
                val minCompareObj = compareAt?.optJSONObject("minVariantPrice")
                val comparePrice = minCompareObj?.optDouble("amount", 0.0)
                val originalPrice = if (comparePrice != null && comparePrice > 0.0) comparePrice else null

                val totalInventory = node.optInt("totalInventory", 10)

                list.add(
                    Product(
                        id = id,
                        title = title,
                        description = desc,
                        price = price,
                        originalPrice = originalPrice,
                        imageUrl = imageUrl,
                        category = "Men's footwears", // Default Category
                        rating = 4.7f,
                        quantityAvailable = totalInventory
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing Shopify GraphQL: ${e.message}", e)
        }
        return list
    }

    // 2. Local Database Reactive Feeds via Room
    val cartItems: Flow<List<DbCartItem>> = dao.getCartItems()
    val coupons: Flow<List<DbCoupon>> = dao.getCoupons()
    val orders: Flow<List<DbOrder>> = dao.getOrders()
    val favorites: Flow<List<DbFavorite>> = dao.getFavorites()

    // Setup initial promo coupons (Status: Claimed/Unclaimed linked to local database)
    suspend fun seedCouponsIfNeeded() {
        dao.getCoupons().first().let { currentList ->
            if (currentList.isEmpty()) {
                val defaultCoupons = listOf(
                    DbCoupon("BEAST30", 30, "Claim 30% Off on your first Premium Active-Gear Purchase!", "Unclaimed"),
                    DbCoupon("LIMEPOWER", 15, "Power up with 15% discount across the whole lineup", "Unclaimed"),
                    DbCoupon("LIQUIDGLASS", 20, "Enjoy 20% Off shoes using the Liquid Glass promotion", "Claimed"),
                    DbCoupon("FITNESSFIT", 10, "10% Off workout shirts", "Unclaimed")
                )
                dao.insertCoupons(defaultCoupons)
            }
        }
    }

    // Interactive details / simulated Supabase/Shopify database manipulations

    // Add back-and-forth states for local favorites
    suspend fun toggleFavorite(productId: String) {
        val current = dao.getFavorites().first()
        if (current.any { it.productId == productId }) {
            dao.removeFavorite(productId)
        } else {
            dao.addFavorite(DbFavorite(productId))
        }
    }

    // Handle Cart manipulations
    suspend fun addToCart(product: Product, size: String, quantity: Int = 1) {
        val idString = "${product.id}_$size"
        val existing = dao.getCartItems().first().firstOrNull { it.id == idString }
        if (existing != null) {
            dao.insertCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            dao.insertCartItem(
                DbCartItem(
                    id = idString,
                    productId = product.id,
                    title = product.title,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    quantity = quantity,
                    size = size
                )
            )
        }
    }

    suspend fun updateCartItemQuantity(id: String, increment: Boolean) {
        val item = dao.getCartItems().first().firstOrNull { it.id == id } ?: return
        if (increment) {
            dao.insertCartItem(item.copy(quantity = item.quantity + 1))
        } else {
            if (item.quantity > 1) {
                dao.insertCartItem(item.copy(quantity = item.quantity - 1))
            } else {
                dao.removeCartItem(id)
            }
        }
    }

    suspend fun removeCartItem(id: String) {
        dao.removeCartItem(id)
    }

    suspend fun claimCoupon(code: String) {
        dao.updateCouponStatus(code, "Claimed")
    }

    // Manual Payment Verification Logic (referenced in PRD 4)
    // When user submits order, they upload a receipt screenshot.
    // In our local room database + Supabase mockup, we insert the Order entity
    // and trigger an automatic countdown state to simulate an Admin verify,
    // which transitions the order status from "Processing" -> "Verified" -> "Shipped".
    suspend fun submitOrder(
        items: List<DbCartItem>,
        totalPrice: Double,
        screenshotFile: File?,
        referralCode: String?
    ): DbOrder {
        val orderId = "BST-" + UUID.randomUUID().toString().take(6).uppercase()
        val itemsSummary = items.joinToString { "${it.title} (${it.size} x${it.quantity})" }
        
        // Save file locally as absolute path
        val screenshotPath = screenshotFile?.absolutePath

        val newOrder = DbOrder(
            id = orderId,
            itemsSummary = itemsSummary,
            totalPrice = totalPrice,
            date = System.currentTimeMillis().toString(),
            screenshotPath = screenshotPath,
            status = "Processing", // Initial status
            referralUsed = referralCode
        )

        dao.insertOrder(newOrder)
        dao.clearCart()

        // Sync with Supabase Orders Table if keys are present
        syncOrderWithSupabase(newOrder)

        // Generate points for referral if valid
        if (!referralCode.isNullOrEmpty()) {
            referralState.value = referralState.value.copy(
                loyaltyPoints = referralState.value.loyaltyPoints + 150,
                count = referralState.value.count + 1
            )
        }

        return newOrder
    }

    private suspend fun syncOrderWithSupabase(order: DbOrder) = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext
        }
        try {
            val json = JSONObject()
                .put("order_id", order.id)
                .put("items_summary", order.itemsSummary)
                .put("total_price", order.totalPrice)
                .put("status", order.status)
                .put("referral_code", order.referralUsed)
                .put("screenshot_url", order.screenshotPath ?: "")

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/orders")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(tag, "Supabase Order Sync Response: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Supabase Sync failed: ${e.message}")
        }
    }

    // Trigger Admin verification simulation
    suspend fun simulateAdminVerification(orderId: String, status: String) {
        dao.updateOrderStatus(orderId, status)
    }

    // Stock alert handler (Notify Me) (referenced in PRD 4)
    fun getStockAlerts(productId: String): Flow<List<DbStockAlert>> {
        return dao.getStockAlertsForProduct(productId)
    }

    suspend fun registerStockAlert(productId: String, email: String) {
        val alertId = "${email}_$productId"
        val alert = DbStockAlert(id = alertId, email = email, productId = productId)
        dao.addStockAlert(alert)

        // Insert to Supabase DB if keys present
        insertStockAlertToSupabase(alert)
    }

    private suspend fun insertStockAlertToSupabase(alert: DbStockAlert) = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) return@withContext
        try {
            val json = JSONObject()
                .put("user_id", alert.email)
                .put("product_id", alert.productId)

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/stock_alerts")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(tag, "Supabase Stock Alert Response: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Supabase stock alert insert failed: ${e.message}")
        }
    }

    // Yelp-style Buyer review feedback submission with comments & mock photo URLs
    fun getProductReviews(productId: String): Flow<List<DbReview>> {
        return dao.getReviewsForProduct(productId)
    }

    suspend fun submitReview(productId: String, username: String, rating: Float, comment: String, screenshotFile: File?) {
        val review = DbReview(
            productId = productId,
            username = username,
            rating = rating,
            comment = comment,
            imagePath = screenshotFile?.absolutePath,
            date = System.currentTimeMillis().toString()
        )
        dao.insertReview(review)
    }

    // Referral state configuration
    private val referralState = MutableStateFlow(ReferralInfo("BEAST-M97H1", 2, 350))
    val referralFlow: Flow<ReferralInfo> = referralState.asStateFlow()
}
