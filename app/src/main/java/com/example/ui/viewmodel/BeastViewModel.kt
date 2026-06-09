package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.BeastDatabase
import com.example.data.model.*
import com.example.data.repository.BeastRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class BeastViewModel(application: Application) : AndroidViewModel(application) {

    private val database: BeastDatabase = Room.databaseBuilder(
        application,
        BeastDatabase::class.java,
        "beast_sports_db"
    ).build()

    private val repository = BeastRepository(
        dao = database.dao(),
        // These can be configured via environment variables (.env / BuildConfig) if needed
        shopifyToken = "",
        shopifyUrl = "",
        supabaseKey = "",
        supabaseUrl = ""
    )

    // UI State flows
    val products = repository.productsFlow
    val categories = repository.getCategories()
    val cartItems = repository.cartItems
    val coupons = repository.coupons
    val orders = repository.orders
    val favorites = repository.favorites
    val referralInfo = repository.referralFlow

    // Search and filtering state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val filteredProducts = combine(products, searchQuery, selectedCategory) { prodList, query, cat ->
        var list = prodList
        if (!cat.isNullOrEmpty()) {
            list = list.filter { it.category.equals(cat, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Detail Page State
    private val _activeProduct = MutableStateFlow<Product?>(null)
    val activeProduct = _activeProduct.asStateFlow()

    private val _pdpSize = MutableStateFlow("L")
    val pdpSize = _pdpSize.asStateFlow()

    private val _pdpQuantity = MutableStateFlow(1)
    val pdpQuantity = _pdpQuantity.asStateFlow()

    // Interactive 3D Mock Drag state
    private val _pdpRotationState = MutableStateFlow(0f)
    val pdpRotationState = _pdpRotationState.asStateFlow()

    // Coupon Checkout discounts
    private val _appliedCoupon = MutableStateFlow<DbCoupon?>(null)
    val appliedCoupon = _appliedCoupon.asStateFlow()

    // Active screen tracking
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen = _currentScreen.asStateFlow()

    // Subtotal, Discount, & Total
    val cartSummary = cartItems.map { items ->
        val subtotal = items.sumOf { it.price * it.quantity }
        val discount = _appliedCoupon.value?.let { coupon ->
            subtotal * (coupon.discountPercent / 100.0)
        } ?: 0.0
        val total = subtotal - discount
        Triple(subtotal, discount, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    init {
        viewModelScope.launch {
            repository.seedCouponsIfNeeded()
            // Pull initial listings from Shopify Storefront GraphQL
            repository.fetchProductsFromShopify()
        }
    }

    // Navigation and screen steering
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setActiveProduct(product: Product) {
        _activeProduct.value = product
        _pdpSize.value = "L"
        _pdpQuantity.value = 1
        _pdpRotationState.value = 0f
    }

    fun setPdpSize(size: String) {
        _pdpSize.value = size
    }

    fun adjustPdpQuantity(increase: Boolean) {
        if (increase) {
            _pdpQuantity.value += 1
        } else {
            if (_pdpQuantity.value > 1) {
                _pdpQuantity.value -= 1
            }
        }
    }

    fun rotatePdpProduct(delta: Float) {
        _pdpRotationState.value = (_pdpRotationState.value + delta + 360f) % 360f
    }

    // Local Db Mutations
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(productId)
        }
    }

    fun addToCartFromPdp() {
        val prod = _activeProduct.value ?: return
        viewModelScope.launch {
            repository.addToCart(prod, _pdpSize.value, _pdpQuantity.value)
        }
    }

    fun adjustCartItemQuantity(id: String, increment: Boolean) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(id, increment)
        }
    }

    fun applyCouponCode(code: String): String {
        return try {
            viewModelScope.launch {
                val list = coupons.first()
                val match = list.firstOrNull { it.code.trim().equals(code.trim(), ignoreCase = true) }
                if (match != null) {
                    if (match.status == "Claimed") {
                        _appliedCoupon.value = match
                    } else {
                        // Claim on the fly as requested by Supabase claims
                        repository.claimCoupon(match.code)
                        _appliedCoupon.value = match.copy(status = "Claimed")
                    }
                }
            }
            "Coupon code applied successfully!"
        } catch (e: Exception) {
            "Coupon is invalid or expired."
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
    }

    fun claimCouponCenter(code: String) {
        viewModelScope.launch {
            repository.claimCoupon(code)
        }
    }

    fun registerStockAlertFromPdp(email: String, onRegistered: () -> Unit) {
        val prod = _activeProduct.value ?: return
        viewModelScope.launch {
            repository.registerStockAlert(prod.id, email)
            onRegistered()
        }
    }

    fun getReviewsForActiveProduct(): Flow<List<DbReview>> {
        val prod = _activeProduct.value ?: return emptyFlow()
        return repository.getProductReviews(prod.id)
    }

    fun submitProductReview(rating: Float, comment: String, selfie: File?, onComplete: () -> Unit) {
        val prod = _activeProduct.value ?: return
        viewModelScope.launch {
            repository.submitReview(
                productId = prod.id,
                username = "Ayan Official", // Matches email reference user
                rating = rating,
                comment = comment,
                screenshotFile = selfie
            )
            onComplete()
        }
    }

    // Submit Order checkout manual screenshots (JazzCash)
    fun processCheckout(screenshotFile: File?, referralUsed: String?, onComplete: (DbOrder) -> Unit) {
        viewModelScope.launch {
            val items = cartItems.first()
            val total = cartSummary.value.third
            val order = repository.submitOrder(items, total, screenshotFile, referralUsed)
            
            // Flush applied coupon
            _appliedCoupon.value = null
            onComplete(order)

            // Auto transition orders state to simulate Shopify Webhook trigger to Supabase edge function admin check
            launch {
                kotlinx.coroutines.delay(12000) // Verifying transition
                repository.simulateAdminVerification(order.id, "Verified")
                kotlinx.coroutines.delay(12000) // Processing to Shipped transition
                repository.simulateAdminVerification(order.id, "Shipped")
            }
        }
    }
}

class BeastViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BeastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BeastViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
