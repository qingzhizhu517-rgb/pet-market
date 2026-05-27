package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PetTab
import com.example.viewmodel.ChatMessage

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    
    // Bottom Sheets or Interactive Popups State
    val selectedListing by viewModel.selectedListing.collectAsState()
    val selectedBreed by viewModel.selectedBreed.collectAsState()
    var isPublishDialogShowing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            PetAppHeader()
        },
        bottomBar = {
            PetBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            if (currentTab == PetTab.MARKET) {
                ExtendedFloatingActionButton(
                    text = { Text("发帖子", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "发布帖子") },
                    onClick = { isPublishDialogShowing = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag("add_pet_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // View switcher for tabs
            Crossfade(
                targetState = currentTab,
                animationSpec = spring(),
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    PetTab.MARKET -> PetMarketView(viewModel)
                    PetTab.WIKI -> PetBreedWikiView(viewModel)
                    PetTab.AI_CHAT -> PetAiChatView(viewModel)
                    PetTab.FAVORITES -> PetFavoritesView(viewModel)
                }
            }
        }
    }

    // Modal dialog for detailed pet listings
    selectedListing?.let { listing ->
        PetListingDetailsDialog(
            listing = listing,
            onDismiss = { viewModel.selectListing(null) },
            isFavorite = viewModel.isFavoriteFlow("Listing:${listing.id}").collectAsState(initial = false).value,
            onToggleFavorite = { viewModel.toggleFavorite("Listing:${listing.id}", "Listing") }
        )
    }

    // Modal dialog for breed encyclopedias
    selectedBreed?.let { breed ->
        BreedDetailsDialog(
            breed = breed,
            onDismiss = { viewModel.selectBreed(null) },
            isFavorite = viewModel.isFavoriteFlow("Breed:${breed.name}").collectAsState(initial = false).value,
            onToggleFavorite = { viewModel.toggleFavorite("Breed:${breed.name}", "Breed") }
        )
    }

    // Modal dialogue to create a new pet listing
    if (isPublishDialogShowing) {
        PublishListingDialog(
            onDismiss = { isPublishDialogShowing = false },
            onSubmit = { category, breed, name, age, gender, price, city, desc, phone, wechat ->
                viewModel.addNewListing(
                    name = name,
                    breedName = breed,
                    category = category,
                    age = age,
                    gender = gender,
                    priceText = price,
                    city = city,
                    description = desc,
                    phone = phone,
                    weChat = wechat
                )
                Toast.makeText(context, "发布成功！帖子已同步到集市中 🐾", Toast.LENGTH_SHORT).show()
                isPublishDialogShowing = false
            }
        )
    }
}

// === TOP APP BAR ===
@Composable
fun PetAppHeader() {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Pets,
                contentDescription = "App logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "宠物集市",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "让温暖与爱守护每一个毛孩子",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// === BOTTOM NAVIGATION BAR ===
@Composable
fun PetBottomNavigation(currentTab: PetTab, onTabSelected: (PetTab) -> Unit) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = currentTab == PetTab.MARKET,
            onClick = { onTabSelected(PetTab.MARKET) },
            icon = { Icon(Icons.Default.Storefront, contentDescription = "集市") },
            label = { Text("宝贝集市", fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("tab_market")
        )
        NavigationBarItem(
            selected = currentTab == PetTab.WIKI,
            onClick = { onTabSelected(PetTab.WIKI) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "百科") },
            label = { Text("品种百科", fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("tab_wiki")
        )
        NavigationBarItem(
            selected = currentTab == PetTab.AI_CHAT,
            onClick = { onTabSelected(PetTab.AI_CHAT) },
            icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI助手") },
            label = { Text("AI顾问", fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("tab_chart")
        )
        NavigationBarItem(
            selected = currentTab == PetTab.FAVORITES,
            onClick = { onTabSelected(PetTab.FAVORITES) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "收藏") },
            label = { Text("我的收藏", fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("tab_favorites")
        )
    }
}

// ==========================================
// VIEW 1: MARKETPLACE VIEW OR SCREEN
// ==========================================
@Composable
fun PetMarketView(viewModel: MainViewModel) {
    val listings by viewModel.filteredListings.collectAsState()
    val categoryFilter by viewModel.marketCategoryFilter.collectAsState()
    val searchQuery by viewModel.marketSearchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Search and filter row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setMarketSearchQuery(it) },
            placeholder = { Text("输入品种、城市、描述搜索主人...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setMarketSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Filters selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "All" to "全部 ✨",
                "Dog" to "狗狗 🐶",
                "Cat" to "猫咪 🐱",
                "Small" to "萌趣小宠 🐹"
            )
            categories.forEach { (key, title) ->
                val isSelected = categoryFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setMarketCategoryFilter(key) },
                    label = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // Listings grid list
        if (listings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = "空",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("没有找到符合条件的宠物宝贝", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("请试着清空搜索词来获取更多内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(listings, key = { it.id }) { listing ->
                    val isFav = viewModel.isFavoriteFlow("Listing:${listing.id}").collectAsState(initial = false).value
                    PetListingItemCard(
                        listing = listing,
                        isFavorite = isFav,
                        onCardClick = { viewModel.selectListing(listing) },
                        onFavoriteClick = { viewModel.toggleFavorite("Listing:${listing.id}", "Listing") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetListingItemCard(
    listing: PetListing,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pet_card_${listing.id}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Unsplash Async Image or beautiful mock placeholder
            SubcomposeAsyncImage(
                model = listing.imageUrl,
                contentDescription = listing.breedName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFECB3), Color(0xFFFFCC80))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFE0B2), Color(0xFFFFB74D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "error",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (listing.category) {
                                    "Cat" -> "猫咪 🐱"
                                    "Dog" -> "狗狗 🐶"
                                    else -> "小宠 🐹"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            // Category tag overlay
            Surface(
                shape = RoundedCornerShape(bottomEnd = 12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = when (listing.category) {
                        "Cat" -> "猫 🐱"
                        "Dog" -> "狗 🐶"
                        else -> "小宠 🐹"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bookmark Button
            IconButton(
                onClick = onFavoriteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏本宠",
                    tint = if (isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Price Tag and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.priceText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                // Listing Age badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = listing.age,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = listing.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            // City and user source information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "地点",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = listing.city,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (listing.isUserPosted) {
                    Text(
                        text = "自发",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// VIEW 2: BREED WIKI VIEW OR SCREEN
// ==========================================
@Composable
fun PetBreedWikiView(viewModel: MainViewModel) {
    val breeds by viewModel.filteredBreeds.collectAsState()
    val categoryFilter by viewModel.wikiCategoryFilter.collectAsState()
    val searchQuery by viewModel.wikiSearchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setWikiSearchQuery(it) },
            placeholder = { Text("搜索品种(如布偶、金毛)或品性标签...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setWikiSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Filters selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "All" to "全部百科 🐾",
                "Dog" to "纯种犬类 🐶",
                "Cat" to "高贵猫咪 🐱",
                "Small" to "珍稀小宠 🐹"
            )
            categories.forEach { (key, title) ->
                val isSelected = categoryFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setWikiCategoryFilter(key) },
                    label = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // Breeds vertical list
        if (breeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无品种数据，换个词试试吧 🐾", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(breeds, key = { it.name }) { breed ->
                    val isFav = viewModel.isFavoriteFlow("Breed:${breed.name}").collectAsState(initial = false).value
                    BreedWikiItemCard(
                        breed = breed,
                        isFavorite = isFav,
                        onCardClick = { viewModel.selectBreed(breed) },
                        onFavoriteClick = { viewModel.toggleFavorite("Breed:${breed.name}", "Breed") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedWikiItemCard(
    breed: BreedInfo,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("breed_card_${breed.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail image of breed
            SubcomposeAsyncImage(
                model = breed.imageUrl,
                contentDescription = breed.name,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = "error", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = breed.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = breed.englishName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏品种",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Short metadata pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = breed.origin,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = breed.lifespan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = breed.coatLength,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Temperament tag list max 2 tags to avoid overcrowding
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    breed.temperamentTags.take(3).forEach { tag ->
                        Text(
                            text = "• $tag",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 3: AI SCIENTIFIC PET CHAT VIEW SCREEN
// ==========================================
@Composable
fun PetAiChatView(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isAiGenerating.collectAsState()

    var chatTextInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to the latest message whenever it changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Suggested quick prompts cards
        Text(
            text = "✨ 快速问答小工具 (点击即发送)：",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "适合朝九晚五打工人的猫咪有哪些？" to "🐱 适合打工人的猫",
                "金毛犬进入成长关键期如何补钙？" to "🦴 金毛怎么补钙",
                "新手第一次带猫狗回家要准备什么？" to "🏠 新手养宠必备"
            )
            items(suggestions) { (fullQuery, title) ->
                Card(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.sendChatMessage(fullQuery)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), thickness = 1.dp)

        // Conversation list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI正在查阅专业百科进行诊断中...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Bottom horizontal chat input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatTextInput,
                onValueChange = { chatTextInput = it },
                placeholder = { Text("例：两月龄幼猫几天喂一顿？") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (chatTextInput.isNotBlank()) {
                            viewModel.sendChatMessage(chatTextInput)
                            chatTextInput = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Submit Send Button
            FloatingActionButton(
                onClick = {
                    if (chatTextInput.isNotBlank() && !isGenerating) {
                        viewModel.sendChatMessage(chatTextInput)
                        chatTextInput = ""
                        keyboardController?.hide()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送消息"
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val bubbleTextColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val bubbleAlignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = bubbleAlignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                // Agent robot avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Companion Bot",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = bubbleColor,
                border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                tonalElevation = if (isUser) 0.dp else 2.dp,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = bubbleTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User emoji/avatar representation
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User avatar",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 4: MY FAVORITES & BOOKMARKS LIST VIEW
// ==========================================
@Composable
fun PetFavoritesView(viewModel: MainViewModel) {
    val favorites by viewModel.favoritesList.collectAsState()
    val allListings by viewModel.filteredListings.collectAsState()
    val allBreeds = BreedData.breeds

    // Match favorite item keys
    val favoriteListings = remember(favorites, allListings) {
        val favIds = favorites.filter { it.resourceType == "Listing" }.map { it.id.substringAfter("Listing:") }
        allListings.filter { it.id.toString() in favIds }
    }

    val favoriteBreeds = remember(favorites) {
        val favNames = favorites.filter { it.resourceType == "Breed" }.map { it.id.substringAfter("Breed:") }
        allBreeds.filter { it.name in favNames }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Section A: Favorite Breeds
        item {
            Text(
                text = "⭐ 收藏的科学百科品种 (${favoriteBreeds.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (favoriteBreeds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "暂未收藏任何名犬猫咪百科。在百科页点击♥心形图标即可添加噢 ℹ️",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(favoriteBreeds, key = { "FavBreed:${it.name}" }) { breed ->
                val isFav = viewModel.isFavoriteFlow("Breed:${breed.name}").collectAsState(initial = true).value
                BreedWikiItemCard(
                    breed = breed,
                    isFavorite = isFav,
                    onCardClick = { viewModel.selectBreed(breed) },
                    onFavoriteClick = { viewModel.toggleFavorite("Breed:${breed.name}", "Breed") }
                )
            }
        }

        // Section B: Favorite Market Pets for adoption/purchase
        item {
            Text(
                text = "🏡 关注的集市小可爱 (${favoriteListings.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }

        if (favoriteListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "您还没收藏集市中的任何宝贝。在集市卡片右上角戳戳小红心将其留存吧 🐾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(favoriteListings) { listing ->
                val isFav = viewModel.isFavoriteFlow("Listing:${listing.id}").collectAsState(initial = true).value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectListing(listing) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubcomposeAsyncImage(
                        model = listing.imageUrl,
                        contentDescription = listing.breedName,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${listing.city} · ${listing.age} · ${listing.priceText}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFavorite("Listing:${listing.id}", "Listing") }
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "取消收藏",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL 1: FORM DIALOGUE TO PUBLISH PET
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishListingDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, breed: String, name: String, age: String, gender: String, price: String, city: String, desc: String, phone: String, wechat: String) -> Unit
) {
    var categoryInput by remember { mutableStateOf("Dog") } // "Dog" or "Cat"
    var breedInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("公") } // "公" or "母"
    var priceInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var wechatInput by remember { mutableStateOf("") }

    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isFormValid = breedInput.isNotBlank() &&
            nameInput.isNotBlank() &&
            ageInput.isNotBlank() &&
            priceInput.isNotBlank() &&
            cityInput.isNotBlank() &&
            descInput.isNotBlank() &&
            phoneInput.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header of dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐾 发布送养/出售宝贝",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close form")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Interactive inputs scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Category Selection segmented control row
                    Text("请选择分类 *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { categoryInput = "Dog" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (categoryInput == "Dog") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (categoryInput == "Dog") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("狗狗 🐶", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { categoryInput = "Cat" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (categoryInput == "Cat") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (categoryInput == "Cat") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("猫咪 🐱", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { categoryInput = "Small" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (categoryInput == "Small") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (categoryInput == "Small") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("小宠 🐹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Breed Input Text
                    OutlinedTextField(
                        value = breedInput,
                        onValueChange = { breedInput = it },
                        label = { Text("宠物具体品种 * (如: 金毛寻回犬、布偶、混血)") },
                        singleLine = true,
                        isError = hasAttemptedSubmit && breedInput.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Card Header/Title
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("卡片标题名称 * (如: 乖巧可爱3月龄金毛...)") },
                        singleLine = true,
                        isError = hasAttemptedSubmit && nameInput.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Age text
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("宠物年龄 * (如: 3个月)") },
                            singleLine = true,
                            isError = hasAttemptedSubmit && ageInput.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        // Gender text
                        Column(modifier = Modifier.weight(1f)) {
                            Text("宝贝性别 *", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = genderInput == "公",
                                    onClick = { genderInput = "公" },
                                    label = { Text("小男生 ♂️") }
                                )
                                FilterChip(
                                    selected = genderInput == "母",
                                    onClick = { genderInput = "母" },
                                    label = { Text("小女生 ♀️") }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Price tag input
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("标价门槛 * (如: 免费领养, ¥800)") },
                            singleLine = true,
                            isError = hasAttemptedSubmit && priceInput.isBlank(),
                            modifier = Modifier.weight(1.2f)
                        )
                        // City tag input
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("所在市/区县 *") },
                            singleLine = true,
                            isError = hasAttemptedSubmit && cityInput.isBlank(),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    // Mobile phone number input
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("联系电话 *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        isError = hasAttemptedSubmit && phoneInput.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // WeChat number
                    OutlinedTextField(
                        value = wechatInput,
                        onValueChange = { wechatInput = it },
                        label = { Text("联系微信号 (选填)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description text box
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("宝贝详细情况说明 * (驱虫疫苗情况,性格,寻找的主人要求等)") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        isError = hasAttemptedSubmit && descInput.isBlank()
                    )

                    if (hasAttemptedSubmit && !isFormValid) {
                        Text(
                            text = "提示：有星号 * 的必填栏目不可留空！",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (isFormValid) {
                            onSubmit(
                                categoryInput,
                                breedInput,
                                nameInput,
                                ageInput,
                                genderInput,
                                priceInput,
                                cityInput,
                                descInput,
                                phoneInput,
                                wechatInput
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_pet_button")
                ) {
                    Text("核对无误，立即发布 (发布后自动上传本地数据库)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// MODAL 2: PET LISTING DETAIL POPUP
// ==========================================
@Composable
fun PetListingDetailsDialog(
    listing: PetListing,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Full Hero Image
                    SubcomposeAsyncImage(
                        model = listing.imageUrl,
                        contentDescription = listing.breedName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Pets, contentDescription = "error", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                        }
                    )

                    // Action buttons overlays (Back, Target star)
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close details", tint = Color.White)
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏本宠",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }

                // Scrollable text content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = when (listing.category) {
                                    "Cat" -> "猫咪 🐱"
                                    "Dog" -> "狗狗 🐶"
                                    else -> "小宠 🐹"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        Text(
                            text = listing.priceText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = listing.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brief details boxes row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailBadgeItem(title = "品种", value = listing.breedName, modifier = Modifier.weight(1f))
                        DetailBadgeItem(title = "年龄", value = listing.age, modifier = Modifier.weight(1f))
                        DetailBadgeItem(title = "性别", value = if (listing.gender == "公") "小男生 ♂️" else "小女生 ♀️", modifier = Modifier.weight(1f))
                        DetailBadgeItem(title = "城市", value = listing.city, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "详细情况描述",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Bottom CTA details for phone call and wechat copy
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💬 联络主人送养方式：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Phone Dial Button
                        Button(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${listing.contactPhone}"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    // Use clipboard fallback if dialer fails
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PetPhone", listing.contactPhone))
                                    Toast.makeText(context, "号码已复制到剪贴贴板: ${listing.contactPhone}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("call_phone_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "一键联系", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("电话咨询", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Copy WeChat Button (If present)
                        if (listing.contactWeChat.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PetWeChat", listing.contactWeChat))
                                    Toast.makeText(context, "微信号已成功复制 🤝 赶紧添加好友吧: ${listing.contactWeChat}", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("copy_wechat_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制微信", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("复制微信号", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailBadgeItem(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ==========================================
// MODAL 3: SCIENTIFIC BREED DIALOGUE DETAIL
// ==========================================
@Composable
fun BreedDetailsDialog(
    breed: BreedInfo,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Photo with overlay
                    SubcomposeAsyncImage(
                        model = breed.imageUrl,
                        contentDescription = breed.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Pets, contentDescription = "error", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                        }
                    )

                    // Exit details & favorites
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close details", tint = Color.White)
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏品种",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }

                // Scroll Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = breed.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = breed.englishName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Location base chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "源生地: ${breed.origin}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "寿命: ${breed.lifespan}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breed details ratings bars
                    Text(
                        text = "🐾 维度品性成长雷达",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextRatingRow(label = "精力状况 (Energy Level)", starCount = breed.energyLevel)
                    TextRatingRow(label = "亲人友善度 (Friendliness)", starCount = breed.friendliness)
                    TextRatingRow(label = "毛发打理困难 (Grooming Need)", starCount = breed.groomingNeed)
                    TextRatingRow(label = "智力及服从性 (Intelligence)", starCount = breed.intelligence)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Behavioral temperaments
                    Text(
                        text = "💎 显性品性标签",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        breed.temperamentTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed general description
                    Text(
                        text = "📖 品种详细描述说明",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = breed.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feeding guide card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "喂养建议", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "💡 科学精细喂养指南 (科学膳食)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = breed.feedingGuide,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextRatingRow(label: String, starCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Row {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i <= starCount) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
