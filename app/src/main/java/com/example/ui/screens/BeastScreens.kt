package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BeastViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.IntOffset

// Custom Programmatic Vector Logo reflecting actual brand request
@Composable
fun BeastLogo(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SecondaryCharcoal,
    stripeColor: Color = Color(0xFFFF0D0D),
    textColor: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = (w.coerceAtMost(h)) * 0.48f
            val center = Offset(w / 2f, h / 2f)
            
            val clipPath = androidx.compose.ui.graphics.Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center, r))
            }
            
            clipPath(clipPath) {
                rotate(degrees = 45f, pivot = center) {
                    val barW = w * 0.11f
                    val gap = w * 0.07f
                    val step = barW + gap
                    for (i in -5..5) {
                        val cx = w / 2f + i * step
                        drawRoundRect(
                            color = stripeColor,
                            topLeft = Offset(cx - barW / 2f, -h * 2f),
                            size = androidx.compose.ui.geometry.Size(barW, h * 5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.24f)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "BEAST",
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

// Helper to format timestamp
fun formatTimestamp(timestampStr: String): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM, yyyy - hh:mm a", Locale.getDefault())
        val date = Date(timestampStr.toLong())
        sdf.format(date)
    } catch (e: Exception) {
        "Recent order"
    }
}

// -------------------------------------------------------------
// GESTURE SENSITIVE AND INTERRUPTIBLE SPRING TRANSITION CONTAINER
// -------------------------------------------------------------
@Composable
fun GestureTransitionContainer(
    currentScreen: String,
    viewModel: BeastViewModel,
    content: @Composable (String) -> Unit
) {
    // We maintain a stack of visited screens so that we can pop/swipe back
    val backstack = remember { mutableStateListOf<String>() }

    // Synchronize currentScreen change with our displayed screen transition
    var displayedScreen by remember { mutableStateOf(currentScreen) }
    var previousScreen by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 0f means fully onscreen. 1.0f means fully offscreen/slid-right.
    val transitionProgress = remember { Animatable(0f) }

    LaunchedEffect(currentScreen) {
        if (currentScreen != displayedScreen) {
            // Manage backstack automatically
            if (currentScreen == "home") {
                backstack.clear()
            } else {
                if (backstack.isEmpty() || backstack.last() != displayedScreen) {
                    if (displayedScreen != "splash") {
                        backstack.add(displayedScreen)
                    }
                }
            }

            previousScreen = displayedScreen
            displayedScreen = currentScreen

            // Start spring slide-in transition: slide in from right (1.0f) to center (0f)
            transitionProgress.snapTo(1f)
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()

        LookaheadScope {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(displayedScreen) {
                        // Only allow swipe-back on non-splash and non-home screens
                        if (displayedScreen != "splash" && displayedScreen != "home" && backstack.isNotEmpty()) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    // Instantly freeze/stop any running slide animation to let user control it
                                    scope.launch { transitionProgress.stop() }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Dragging to the right (dragAmount > 0) expands the offset (transitionProgress toward 1f)
                                    val deltaFraction = dragAmount / widthPx
                                    scope.launch {
                                        val newVal = (transitionProgress.value + deltaFraction).coerceIn(0f, 1f)
                                        transitionProgress.snapTo(newVal)
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        if (transitionProgress.value > 0.40f) {
                                            // Swipe completed: animate offscreen and pop
                                            transitionProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                )
                                            )
                                            val prev = backstack.removeLastOrNull()
                                            if (prev != null) {
                                                viewModel.navigateTo(prev)
                                                // Reset progress to 0 for the newly focused screen
                                                transitionProgress.snapTo(0f)
                                                previousScreen = null
                                            }
                                        } else {
                                            // Swipe cancelled: spring back to onscreen
                                            transitionProgress.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        transitionProgress.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
            ) {
                // Render previous screen in the background if transitioning
                val prev = previousScreen
                if (prev != null && transitionProgress.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                // Parallax offset: slide slightly left as current screen slides right
                                IntOffset(
                                    x = ((-transitionProgress.value * 0.25f) * widthPx).toInt(),
                                    y = 0
                                )
                            }
                    ) {
                        content(prev)
                    }
                }

                // Render active screen sliding in or out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (transitionProgress.value * widthPx).toInt(),
                                y = 0
                            )
                        }
                        .shadow(
                            elevation = if (transitionProgress.value > 0f) 16.dp else 0.dp,
                            clip = false,
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    content(displayedScreen)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE PROFESSIONAL BUTTON WITH SPRING SCALE & HAPTICS ON PRESS
// -------------------------------------------------------------
@Composable
fun ProfessionalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = PrimaryLime,
    contentColor: Color = SecondaryCharcoal,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(30.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1.0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .scale(scale.value)
            .pointerInput(enabled) {
                if (enabled) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    scale.animateTo(
                                        targetValue = 0.95f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioNoBouncy
                                        )
                                    )
                                }
                            } else if (event.type == PointerEventType.Release) {
                                scope.launch {
                                    scale.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioNoBouncy
                                        )
                                    )
                                }
                                onClick()
                            }
                        }
                    }
                }
            }
            .clip(shape)
            .background(backgroundColor)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

// -------------------------------------------------------------
// MAIN NAV CONTAINER WITH SPRING FLOATING BOTTOM PILL-BAR
// -------------------------------------------------------------
@Composable
fun MainLayoutContainer(viewModel: BeastViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartList by viewModel.cartItems.collectAsState(emptyList())

    val totalItemsInCart = cartList.sumOf { it.quantity }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            GestureTransitionContainer(
                currentScreen = currentScreen,
                viewModel = viewModel
            ) { screenToRender ->
                when (screenToRender) {
                    "splash" -> SplashScreen(onSplashFinished = { viewModel.navigateTo("home") })
                    "home" -> HomeScreen(viewModel, innerPadding)
                    "search" -> SearchScreen(viewModel, innerPadding)
                    "pdp" -> ProductDetailScreen(viewModel, innerPadding)
                    "cart" -> CartScreen(viewModel, innerPadding)
                    "coupons" -> CouponCenterScreen(viewModel, innerPadding)
                    "checkout" -> CheckoutScreen(viewModel, innerPadding)
                    "profile" -> ProfileScreen(viewModel, innerPadding)
                    "invite" -> InviteAndEarnScreen(viewModel, innerPadding)
                    "reviews" -> ReviewsScreen(viewModel, innerPadding)
                }
            }

            if (currentScreen != "splash") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding() // Navigation Bar padding!
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    FloatingSpringBottomBar(
                        currentTab = currentScreen,
                        badgeCount = totalItemsInCart,
                        onTabSelected = { screen ->
                            viewModel.navigateTo(screen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingSpringBottomBar(
    currentTab: String,
    badgeCount: Int,
    onTabSelected: (String) -> Unit
) {
    // Elegant Capsule Float Container, mimicking reference pic
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                spotColor = Color.Black.copy(alpha = 0.3f),
                ambientColor = Color.Black.copy(alpha = 0.15f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp)),
        color = SecondaryCharcoal,
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation tabs
            BottomBarItem(
                tabName = "home",
                label = "Home",
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                currentTab = currentTab,
                onSelected = onTabSelected
            )

            BottomBarItem(
                tabName = "cart",
                label = "Cart",
                icon = Icons.Outlined.ShoppingCart,
                selectedIcon = Icons.Filled.ShoppingCart,
                currentTab = currentTab,
                badgeCount = badgeCount,
                onSelected = onTabSelected
            )

            BottomBarItem(
                tabName = "coupons",
                label = "Offers",
                icon = Icons.Outlined.CardMembership,
                selectedIcon = Icons.Filled.CardMembership,
                currentTab = currentTab,
                onSelected = onTabSelected
            )

            BottomBarItem(
                tabName = "profile",
                label = "Profile",
                icon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person,
                currentTab = currentTab,
                onSelected = onTabSelected
            )
        }
    }
}

@Composable
fun RowScope.BottomBarItem(
    tabName: String,
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    currentTab: String,
    badgeCount: Int = 0,
    onSelected: (String) -> Unit
) {
    val isSelected = currentTab == tabName || (tabName == "home" && (currentTab == "pdp" || currentTab == "search" || currentTab == "reviews")) || (tabName == "cart" && currentTab == "checkout") || (tabName == "profile" && currentTab == "invite")  
    
    // Custom spring specification derived from: stiffness = 400f and physical damping = 30f
    // Natural Frequency = sqrt(stiffness) = sqrt(400) = 20
    // Critical Damping = 2 * Natural Frequency = 40
    // Damping Ratio = Physical Damping / Critical Damping = 30 / 40 = 0.75f
    val navSpringSpec = spring<Float>(
        stiffness = 400f,
        dampingRatio = 0.75f
    )

    // Spring physics animation for expanding selected tab width and background visibility
    val weightState by animateFloatAsState(
        targetValue = if (isSelected) 1.5f else 1.0f,
        animationSpec = navSpringSpec,
        label = "weight"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = navSpringSpec,
        label = "bgAlpha"
    )

    Box(
        modifier = Modifier
            .weight(weightState)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryLime.copy(alpha = backgroundAlpha))
            .clickable { onSelected(tabName) },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = if (isSelected) selectedIcon else icon,
                    contentDescription = label,
                    tint = if (isSelected) SecondaryCharcoal else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                // Cart count badge
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(14.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expand labeled pill using AnimatedVisibility with scale and smooth horizontal clip transitions
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start) + scaleIn(),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    color = SecondaryCharcoal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 1: BRAND SPLASH SCREEN
// -------------------------------------------------------------
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(0f) }
    
    // Auto timeout to home screen after lovely logo transition
    LaunchedEffect(key1 = true) {
        // Parallel launch transitions
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        kotlinx.coroutines.delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SecondaryCharcoal), // Sophisticated deep slate dark brand splash
        contentAlignment = Alignment.Center
    ) {
        // Abstract Liquid Glass glowing backgrounds on canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryLime.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.8f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            // Circular branding custom logo with parallel red stripes as requested
            BeastLogo(
                modifier = Modifier
                    .size(160.dp)
                    .rotate(rotation.value)
                    .shadow(32.dp, CircleShape, spotColor = Color.Red.copy(alpha = 0.4f))
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                backgroundColor = SecondaryCharcoal,
                stripeColor = Color(0xFFFF0D0D),
                textColor = Color.White,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BEAST SPORTS",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MINIMALIST LUXURY ACTIVEWEAR",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}


// -------------------------------------------------------------
// SCREEN 2: HOME SCREEN (PRODUCTS + PRO-TIP VIDEOS + LISTS)
// -------------------------------------------------------------
@Composable
fun HomeScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val categories = viewModel.categories
    val filteredProducts by viewModel.filteredProducts.collectAsState(emptyList())
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favorites.collectAsState(emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(), // Notch safe top padding
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)
    ) {
        // 1. Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branded Title with Subtitle stacked below
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BeastLogo(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color.Red.copy(alpha = 0.2f)),
                        backgroundColor = SecondaryCharcoal,
                        stripeColor = Color(0xFFFF0D0D),
                        textColor = Color.White,
                        fontSize = 7.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "BEAST",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                letterSpacing = (-1).sp,
                                color = SecondaryCharcoal
                            )
                            Text(
                                text = ".",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryLime
                            )
                        }
                        Text(
                            text = "SPORTS COLLECTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = SecondaryCharcoal.copy(alpha = 0.4f)
                        )
                    }
                }

                // Header interactive shortcuts on the right
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo("search") },
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Navigate to Search",
                            tint = SecondaryCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo("cart") },
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = "Quick Cart",
                            tint = SecondaryCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Interactive Search & Predictive Slider Input
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
                    .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(0.08f))
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .clickable { viewModel.navigateTo("search") } // Directs behavior predictive
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) searchQuery else "What are you looking for?",
                        color = MutedText,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Slider filter",
                        tint = PrimaryLime,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Limited Offer Promotion Banner Card
        item {
            PromoBanner(
                onShopClick = {
                    // Set slip-ons product and route straight to detailed PDP
                    val slipOnItem = filteredProducts.firstOrNull { it.id == "shopify_p_1" }
                    if (slipOnItem != null) {
                        viewModel.setActiveProduct(slipOnItem)
                        viewModel.navigateTo("pdp")
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Scrollable Category Tags
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCharcoal
                )
                TextButton(onClick = { viewModel.navigateTo("search") }) {
                    Text(
                        text = "See all",
                        color = PrimaryLime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(categories) { cat ->
                    val isCatSelected = selectedCat == cat.name
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .height(60.dp)
                            .shadow(
                                if (isCatSelected) 8.dp else 2.dp,
                                RoundedCornerShape(30.dp)
                            )
                            .clickable {
                                if (isCatSelected) {
                                    viewModel.selectCategory(null)
                                } else {
                                    viewModel.selectCategory(cat.name)
                                    viewModel.navigateTo("search")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCatSelected) PrimaryLime else Color.White
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category image circular frame
                            AsyncImage(
                                model = cat.imageUrl,
                                contentDescription = cat.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.name.split(" ").firstOrNull() ?: cat.name,
                                color = if (isCatSelected) Color.White else SecondaryCharcoal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Pro-Tip Training Videos (Sports section)
        item {
            Text(
                text = "Beast Pro-Tips Training",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCharcoal,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalProTipSection(onTipClick = { tipTitle ->
                viewModel.navigateTo("invite") // Explaining training mechanics
            })
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. New Arrivals (Product grid layout mimicking left reference screen)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Arrival",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCharcoal
                )
                TextButton(onClick = { viewModel.navigateTo("search") }) {
                    Text(
                        text = "See all",
                        color = PrimaryLime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Map column listings cleanly to achieve smooth nesting scroll performance
        items(filteredProducts.chunked(2)) { chunk ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (product in chunk) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProductGridItem(
                            product = product,
                            isFavorite = favorites.any { it.productId == product.id },
                            onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                            onClick = {
                                viewModel.setActiveProduct(product)
                                viewModel.navigateTo("pdp")
                            }
                        )
                    }
                }
                if (chunk.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Custom promo banner matching "Limited Offer"
@Composable
fun PromoBanner(onShopClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(190.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(30.dp),
                clip = false,
                spotColor = Color.Black.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryCharcoal)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Info Section
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Premium Sleek Tag
                    Box(
                        modifier = Modifier
                            .background(PrimaryLime, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW SEASON",
                            color = SecondaryCharcoal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Column {
                        Text(
                            text = "Liquid Iron Series",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Limited Edition Compression",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Shop Now button
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(PrimaryLime)
                            .clickable { onShopClick() }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shop Now",
                            color = SecondaryCharcoal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = "Go",
                            tint = SecondaryCharcoal,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Athlete Image Section with smooth horizontal gradient overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&w=400&q=80",
                        contentDescription = "Model model",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Blend transition
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(SecondaryCharcoal, Color.Transparent),
                                    startX = 0.0f,
                                    endX = 140.0f
                                )
                            )
                    )
                }
            }
        }
    }
}

// Pro-Tip Section
@Composable
fun HorizontalProTipSection(onTipClick: (String) -> Unit) {
    val proTips = listOf(
        Pair("Propel Sprints with correct foot placement", "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&fit=crop&w=400&q=80"),
        Pair("Avoid footwear shin-splints on road tracks", "https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=400&q=80"),
        Pair("Core balance adjustments under high weight", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=400&q=80")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(proTips) { (title, img) ->
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .height(115.dp)
                    .clickable { onTipClick(title) }
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = img,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dark dim overlay for readable layout
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                    // Play icon in center
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play instruction video",
                        tint = PrimaryLime,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

// Product Grid display item matching left reference grid item
@Composable
fun ProductGridItem(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(30.dp))
        ) {
            // Core image
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
            )

            // Favorited heart icon floating top-right
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.88f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Fav",
                    tint = if (isFavorite) HeartRed else SecondaryCharcoal,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Simple rating float indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = StarYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = product.rating.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Product title & pricing below card exactly like reference image (e.g. $120 listed underneath)
        Text(
            text = product.title,
            color = SecondaryCharcoal,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$${product.price.toInt()}",
                color = PrimaryLime,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            if (product.originalPrice != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$${product.originalPrice.toInt()}",
                    color = MutedText.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    style = TextStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                )
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 3: PREDICTIVE SEARCH / CATEGORIES SEARCH CHIPS
// -------------------------------------------------------------
@Composable
fun SearchScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favorites.collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Search toolbar Row with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("home") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SecondaryCharcoal
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("What are you looking for?") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryLime,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Category selection chips
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Filters:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryCharcoal)
            Spacer(modifier = Modifier.width(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val list = listOf("All", "Men's outfit", "woman's outfit", "Men's footwears")
                items(list) { catItem ->
                    val isSelected = (catItem == "All" && selectedCat == null) || (catItem == selectedCat)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryLime else Color.White)
                            .clickable {
                                if (catItem == "All") viewModel.selectCategory(null) else viewModel.selectCategory(catItem)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = catItem,
                            color = if (isSelected) Color.White else SecondaryCharcoal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Results listing grid
        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RunningWithErrors,
                        contentDescription = "No product matches",
                        tint = MutedText,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No athletic gear matched search",
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCharcoal
                    )
                    Text(
                        text = "Try switching category filters or keywords.",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 110.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { item ->
                    ProductGridItem(
                        product = item,
                        isFavorite = favorites.any { it.productId == item.id },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                        onClick = {
                            viewModel.setActiveProduct(item)
                            viewModel.navigateTo("pdp")
                        }
                    )
                }
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 4: PRODUCT DETAIL SCREEN (PDP - INTERACTIVE VIEW)
// -------------------------------------------------------------
@Composable
fun ProductDetailScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val activeProduct by viewModel.activeProduct.collectAsState()
    val sizeSelect by viewModel.pdpSize.collectAsState()
    val qtySelect by viewModel.pdpQuantity.collectAsState()
    val rotationAngle by viewModel.pdpRotationState.collectAsState()
    val favorites by viewModel.favorites.collectAsState(emptyList())

    var isFollowingStore by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockEmail by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    val product = activeProduct ?: return

    val isFav = favorites.any { it.productId == product.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Parent container scroll handles details and comments layout safely
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // 1. Details Toolbar Row (Mimicking reference PDP screen header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("home") },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SecondaryCharcoal
                    )
                }

                Text(
                    text = "Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCharcoal
                )

                IconButton(
                    onClick = { viewModel.navigateTo("cart") },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = "Cart",
                        tint = SecondaryCharcoal
                    )
                }
            }

            // 2. Interactive PDP Image display with custom rotate motion gesture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(310.dp)
                    .shadow(12.dp, RoundedCornerShape(30.dp))
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
            ) {
                // Large product image with horizontal swipe-to-rotate interaction labels mapping standard 3D rotate
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle) // Interactive rotation!
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    viewModel.rotatePdpProduct(dragAmount * 0.4f)
                                }
                            )
                        }
                )

                // 3D rotation hint tooltip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Swipe to rotate 3D view",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Heart favourite floating container
                IconButton(
                    onClick = { viewModel.toggleFavorite(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Fav product",
                        tint = if (isFav) HeartRed else SecondaryCharcoal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider indicators dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { idx ->
                    val isAct = idx == ((rotationAngle.toInt() / 90) % 4)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (isAct) 16.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isAct) SecondaryCharcoal else Color.LightGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Product Info Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = product.category,
                    color = MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.title,
                        color = SecondaryCharcoal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = { viewModel.toggleFavorite(product.id) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) HeartRed else SecondaryCharcoal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Official Store Header (Mimicking "Velora Store" -> Beast official)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(0.04f))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Store avatar custom vector drawing shape circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AccentPillLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsBasketball,
                                contentDescription = "Store",
                                tint = PrimaryLime
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.storeName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SecondaryCharcoal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Official",
                                    tint = PrimaryLime,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Official Seller Station",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }
                    }

                    // Following black pill button
                    Button(
                        onClick = { isFollowingStore = !isFollowingStore },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowingStore) PrimaryLime else SecondaryCharcoal
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isFollowingStore) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isFollowingStore) "Following" else "+ Follow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Size select swatch + Qty selection header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select size",
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCharcoal,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "QTY",
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCharcoal,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // S, M, L, XL size choices
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sizes = listOf("S", "M", "L", "XL")
                        sizes.forEach { size ->
                            val isChosen = sizeSelect == size
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(2.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) PrimaryLime else Color.White)
                                    .clickable { viewModel.setPdpSize(size) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = size,
                                    color = if (isChosen) Color.White else SecondaryCharcoal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Stepper quantity adjust button
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .shadow(2.dp, RoundedCornerShape(22.dp))
                            .background(Color.White, RoundedCornerShape(22.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.adjustPdpQuantity(false) }) {
                            Text(text = "-", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SecondaryCharcoal)
                        }
                        Text(
                            text = qtySelect.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SecondaryCharcoal,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = { viewModel.adjustPdpQuantity(true) }) {
                            Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SecondaryCharcoal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Description Block
                Text(
                    text = "Description",
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCharcoal,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.description,
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rating buyer feedback trigger redirects review page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo("reviews") }
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.RateReview, contentDescription = null, tint = PrimaryLime)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Customer Reviews (${product.reviewsCount})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SecondaryCharcoal
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${product.rating} / 5.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = SecondaryCharcoal
                        )
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MutedText)
                    }
                }
            }
        }

        // Out of Stock "Notify Me" trigger (PRD 4)
        if (product.quantityAvailable == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(0.1f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.NotificationImportant, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Out of Stock! Register restock notification below.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        }

        // PDP Sticky floating action bar matching right reference pic
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total price",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${(product.price * qtySelect).toInt()}",
                        color = SecondaryCharcoal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (product.quantityAvailable > 0) {
                    // Solid Add to Cart green pill button enclosing shopping bag icon
                    ProfessionalButton(
                        onClick = {
                            viewModel.addToCartFromPdp()
                            Toast.makeText(ctx, "Added ${qtySelect}x to Cart!", Toast.LENGTH_SHORT).show()
                        },
                        backgroundColor = PrimaryLime,
                        contentColor = SecondaryCharcoal,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(180.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = SecondaryCharcoal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add to Cart",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCharcoal
                            )
                        }
                    }
                } else {
                    // Out of stock trigger inserts stock_alerts db tracking
                    Button(
                        onClick = { showRestockDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryCharcoal),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(180.dp)
                    ) {
                        Text(
                            text = "Notify Me",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for restock alert subscription
    if (showRestockDialog) {
        AlertDialog(
            onDismissRequest = { showRestockDialog = false },
            title = { Text("Notify Me Restock Alert", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "We will register your email to trigger a push notification via FCM once Shopify restocks.",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restockEmail,
                        onValueChange = { restockEmail = it },
                        placeholder = { Text("Enter your email address") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restockEmail.isNotEmpty()) {
                            viewModel.registerStockAlertFromPdp(restockEmail) {
                                Toast.makeText(ctx, "Alert Registered successfully!", Toast.LENGTH_SHORT).show()
                                showRestockDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime)
                ) {
                    Text("Register Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// -------------------------------------------------------------
// SCREEN 5: CART SCREEN (LINE ITEMS & COUPONS APPLY)
// -------------------------------------------------------------
@Composable
fun CartScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val items by viewModel.cartItems.collectAsState(emptyList())
    val promoCoupon by viewModel.appliedCoupon.collectAsState()
    val summary by viewModel.cartSummary.collectAsState()

    var couponInput by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    val (subtotal, discount, total) = summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("home") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "My Cart",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCharcoal
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = "Empty cart",
                        tint = MutedText,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Cart resembles a clean void",
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCharcoal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { viewModel.navigateTo("home") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime)
                    ) {
                        Text("Add sports gear")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Cart Line list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SecondaryCharcoal
                                )
                                Text(
                                    text = "Size: ${item.size}",
                                    fontSize = 12.sp,
                                    color = MutedText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$${item.price.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryLime,
                                    fontSize = 13.sp
                                )
                            }

                            // Subtraction Addition controls
                            Row(
                                modifier = Modifier
                                    .background(LightGray, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.adjustCartItemQuantity(item.id, false) }) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text(item.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { viewModel.adjustCartItemQuantity(item.id, true) }) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Promo Coupon Code Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Enter Coupon Code",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SecondaryCharcoal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it },
                            placeholder = { Text("E.g. BEAST30") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryLime
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (couponInput.isNotEmpty()) {
                                    val feedback = viewModel.applyCouponCode(couponInput)
                                    Toast.makeText(ctx, feedback, Toast.LENGTH_SHORT).show()
                                    couponInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryCharcoal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply")
                        }
                    }

                    if (promoCoupon != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Applied Code: ${promoCoupon?.code} (${promoCoupon?.discountPercent}% Off)",
                                color = PrimaryLime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.removeCoupon() }) {
                                Text("Remove", color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Pricing summaries before checkout triggers
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 110.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = MutedText, fontSize = 14.sp)
                        Text("$${subtotal.toInt()}", fontWeight = FontWeight.Bold, color = SecondaryCharcoal)
                    }
                    if (discount > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount coupon applied", color = PrimaryLime, fontSize = 14.sp)
                            Text("-$${discount.toInt()}", fontWeight = FontWeight.Bold, color = PrimaryLime)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SecondaryCharcoal)
                        Text("$${total.toInt()}", fontWeight = FontWeight.Black, fontSize = 19.sp, color = SecondaryCharcoal)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.navigateTo("checkout") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Proceed to Checkout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 6: COUPON CENTER SCREEN
// -------------------------------------------------------------
@Composable
fun CouponCenterScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val couponList by viewModel.coupons.collectAsState(emptyList())
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Beast Savings Hub",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SecondaryCharcoal
        )
        Text(
            text = "Claim discount coupons stored on Supabase to slash checkout prices.",
            fontSize = 12.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (couponList.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryLime)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(couponList) { coupon ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (coupon.status == "Claimed") Color.White else AccentPillLight
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .background(SecondaryCharcoal, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = coupon.code,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = coupon.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SecondaryCharcoal
                                )
                                Text(
                                    text = "${coupon.discountPercent}% Instant Cut on Sports Apparel",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Claim State button
                            Button(
                                onClick = {
                                    if (coupon.status == "Unclaimed") {
                                        viewModel.claimCouponCenter(coupon.code)
                                        Toast.makeText(ctx, "Coupon claimed successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (coupon.status == "Claimed") LightGray else PrimaryLime
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (coupon.status == "Claimed") "Claimed" else "Claim",
                                    color = if (coupon.status == "Claimed") MutedText else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 7: CHECKOUT SCREEN (MANUAL PAYMENT RECEIVED SCREENSHOT)
// -------------------------------------------------------------
@Composable
fun CheckoutScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val summary by viewModel.cartSummary.collectAsState()
    val ctx = LocalContext.current

    val (subtotal, discount, total) = summary

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var referralInput by remember { mutableStateOf("") }

    // Manual Upload simulated state (JazzCash/EasyPaisa)
    var isScreenshotUploaded by remember { mutableStateOf(false) }
    var isUploadingReceipt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("cart") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Manual Checkout Payments",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCharcoal
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Address block description
            item {
                Text(
                    text = "Delivery Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SecondaryCharcoal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Full Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = referralInput,
                        onValueChange = { referralInput = it },
                        label = { Text("Referrer's unique code (points back)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Pay channel guidelines
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentPillLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PK Manual Transact Instructions",
                            fontWeight = FontWeight.ExtraBold,
                            color = SecondaryCharcoal,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Send exact PKR equivalent (1 USD ~ 278 PKR) to our mobile wallets below:\n" +
                                   "   - JazzCash Wallet: 0300-1234567 (Ayan Official)\n" +
                                   "   - EasyPaisa Wallet: 0315-7654321 (Beast Corp)\n" +
                                   "2. Take a clear screenshot of successful transfer receipt.\n" +
                                   "3. Upload snapshot below for verification database storage.",
                            fontSize = 12.sp,
                            color = SecondaryCharcoal.copy(0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Screenshot uploader widget (PRD 4)
            item {
                Text(
                    text = "Invoice Screenshot Upload",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SecondaryCharcoal
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(
                            width = 2.dp,
                            color = if (isScreenshotUploaded) PrimaryLime else Color.LightGray,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            isUploadingReceipt = true
                            // Simulate uploader processing
                            isScreenshotUploaded = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingReceipt) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryLime)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Simulating file image capture...", fontSize = 11.sp, color = MutedText)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(2000)
                                isUploadingReceipt = false
                                isScreenshotUploaded = true
                            }
                        }
                    } else if (isScreenshotUploaded) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryLime,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "JazzCash_Receipt_BST_998.png",
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCharcoal,
                                fontSize = 13.sp
                            )
                            Text(
                                "Tap to rebuild or re-select",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = PrimaryLime,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Select Payment Screenshot",
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCharcoal,
                                fontSize = 13.sp
                            )
                            Text(
                                "Supports JPEG, PNG up to 10MB",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }
                    }
                }
            }

            // Checkout Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Product subtotal:", fontSize = 12.sp, color = MutedText)
                            Text("$${subtotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Applied Coupon discount:", fontSize = 12.sp, color = PrimaryLime)
                            Text("-$${discount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryLime)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$${total.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SecondaryCharcoal)
                        }
                    }
                }
            }

            // Submit Button
            item {
                ProfessionalButton(
                    onClick = {
                        if (fullName.isEmpty() || phoneNumber.isEmpty() || deliveryAddress.isEmpty()) {
                            Toast.makeText(ctx, "Please complete delivery information", Toast.LENGTH_SHORT).show()
                        } else if (!isScreenshotUploaded) {
                            Toast.makeText(ctx, "Manual Payment Receipt Screenshot is required!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Proceed submission
                            val dummyReceipt = File(ctx.cacheDir, "simulated_invoice_receipt.png")
                            dummyReceipt.createNewFile()

                            viewModel.processCheckout(dummyReceipt, referralInput) { registeredOrder ->
                                Toast.makeText(ctx, "Order placed successfully! Verified simulation triggered.", Toast.LENGTH_LONG).show()
                                viewModel.navigateTo("profile")
                            }
                        }
                    },
                    backgroundColor = PrimaryLime,
                    contentColor = SecondaryCharcoal,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Submit Order for Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SecondaryCharcoal)
                }
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 8: PROFILE SCREEN (VERIFIED STAGES ORDER TIMELINE)
// -------------------------------------------------------------
@Composable
fun ProfileScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val orders by viewModel.orders.collectAsState(emptyList())
    val referralInfo by viewModel.referralInfo.collectAsState(ReferralInfo("", 0, 0))
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // User Meta header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryLime, PrimaryLime.copy(alpha = 0.85f))
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, SecondaryCharcoal.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AO",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = SecondaryCharcoal
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Ayan Official",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = SecondaryCharcoal
                )
                Text(
                    text = "ayanofficial03@gmail.com",
                    fontSize = 13.sp,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Unique referral Click-to-copy launcher button card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Referral Code Info",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MutedText
                    )
                    Text(
                        text = referralInfo.uniqueCode,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = PrimaryLime
                    )
                }
                Button(
                    onClick = {
                        viewModel.navigateTo("invite")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryCharcoal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Launch Referral", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Track Sports Orders",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = SecondaryCharcoal
        )
        Text(
            text = "Shows manual JazzCash state verification timeline updates.",
            fontSize = 12.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MutedText.copy(alpha = 0.6f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No recorded orders in tracking backlog",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCharcoal.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order Target: ${order.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                // Map Status tags cleanly
                                val badgeColor = when (order.status) {
                                    "Processing" -> Color(0xFFF57C00) // Orange
                                    "Verified" -> Color(0xFF1976D2)   // Blue
                                    else -> PrimaryLime
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = order.status,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Placed: " + formatTimestamp(order.date),
                                fontSize = 11.sp,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = order.itemsSummary,
                                fontSize = 12.sp,
                                color = SecondaryCharcoal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            // Timeline Stages
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TimelineStep("1. Paid", true)
                                TimelineStep("2. Verified", order.status == "Verified" || order.status == "Shipped")
                                TimelineStep("3. Shipped", order.status == "Shipped")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStep(label: String, isFinished: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isFinished) PrimaryLime else Color.LightGray, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isFinished) FontWeight.Bold else FontWeight.Normal,
            color = if (isFinished) SecondaryCharcoal else MutedText
        )
    }
}


// -------------------------------------------------------------
// SCREEN 9: INVITE & EARN SCREEN (LOYALTY SYSTEM)
// -------------------------------------------------------------
@Composable
fun InviteAndEarnScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val referralInfo by viewModel.referralInfo.collectAsState(ReferralInfo("", 0, 0))
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("profile") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Invite & Loyalty System",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCharcoal
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large illustration logo frame on canvas
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(AccentPillLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = PrimaryLime,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Share & Claim Beast Points",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = SecondaryCharcoal
        )
        Text(
            text = "Get friends to insert your code at manual checkouts on Supabase and earn 150 points instantly per shopper!",
            fontSize = 12.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Loyalty Stats Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Invites", fontSize = 11.sp, color = MutedText)
                    Text(
                        referralInfo.count.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = SecondaryCharcoal
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Earned Points", fontSize = 11.sp, color = MutedText)
                    Text(
                        "${referralInfo.loyaltyPoints} PTS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryLime
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Copy referral code box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryCharcoal)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOUR UNIQUE INVITE CODE",
                    color = Color.White.copy(0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = referralInfo.uniqueCode,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(onClick = {
                        Toast.makeText(ctx, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryLime)
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// SCREEN 10: REVIEWS FEEDBACK SYSTEM
// -------------------------------------------------------------
@Composable
fun ReviewsScreen(viewModel: BeastViewModel, innerPadding: PaddingValues) {
    val activeProduct by viewModel.activeProduct.collectAsState()
    val reviewsList by viewModel.getReviewsForActiveProduct().collectAsState(emptyList())
    val ctx = LocalContext.current

    val product = activeProduct ?: return

    var buyerRating by remember { mutableStateOf(5f) }
    var reviewComment by remember { mutableStateOf("") }
    var isVerifiedMockUpload by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo("pdp") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Buyer Feedback Reviews",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCharcoal
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // New Review Submission Builder Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Write Verified Review",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SecondaryCharcoal
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Star selector click rates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(5) { starIdx ->
                                val active = starIdx < buyerRating
                                IconButton(onClick = { buyerRating = (starIdx + 1).toFloat() }) {
                                    Icon(
                                        imageVector = if (active) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = StarYellow,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Input fields
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            placeholder = { Text("What did you enjoy about the fit, comfort or aesthetic?") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mock Photo attached picker (PRD reviews)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Background)
                                    .clickable { isVerifiedMockUpload = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = PrimaryLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isVerifiedMockUpload) "Photo Picked" else "Attach Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCharcoal
                                )
                            }

                            Button(
                                onClick = {
                                    if (reviewComment.isNotEmpty()) {
                                        val dummyPhoto = if (isVerifiedMockUpload) {
                                            val f = File(ctx.cacheDir, "review_photo.png")
                                            f.createNewFile()
                                            f
                                        } else null

                                        viewModel.submitProductReview(buyerRating, reviewComment, dummyPhoto) {
                                            Toast.makeText(ctx, "Review published!", Toast.LENGTH_SHORT).show()
                                            reviewComment = ""
                                            isVerifiedMockUpload = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Publish Feedback", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Historical items headline
            item {
                Text(
                    text = "Most Recent Verified Purchases",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SecondaryCharcoal
                )
            }

            // Dynamic list combining database storage reviews
            if (reviewsList.isEmpty()) {
                item {
                    // Standard Yelp placeholders seeded
                    DefaultReviewPlaceholder("Ayan Khan", 5f, "Excellent off-white luxury canvas! The 30dp curves feel extremely distinct and snug around coordinates. Totally worth the price tag!")
                    Spacer(modifier = Modifier.height(10.dp))
                    DefaultReviewPlaceholder("Zia Haider", 4.5f, "Very fast delivery in Pakistan! The EasyPaisa verification was confirmed in under 5 minutes. Strongly recommend.")
                }
            } else {
                items(reviewsList) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(PrimaryLime, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            r.username.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = r.username, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Row {
                                    repeat(5) { idx ->
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = if (idx < r.rating) StarYellow else Color.LightGray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = r.comment, fontSize = 13.sp, color = SecondaryCharcoal)
                            
                            if (r.imagePath != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                // Draw attached mock photo border
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(AccentPillLight, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = PrimaryLime)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultReviewPlaceholder(name: String, rating: Float, comment: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row {
                    repeat(5) { idx ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (idx < rating) StarYellow else Color.LightGray,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = comment, fontSize = 12.sp, color = SecondaryCharcoal)
        }
    }
}
