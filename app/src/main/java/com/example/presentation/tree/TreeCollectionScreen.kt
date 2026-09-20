package com.example.presentation.tree

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.net.URLEncoder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.tree.CodeRedemptionResult
import com.example.domain.tree.TreeCatalog
import com.example.domain.tree.TreeCatalogItem
import com.example.presentation.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeCollectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRedeemDialog by remember { mutableStateOf(false) }
    var selectedTreeDetail by remember { mutableStateOf<TreeCatalogItem?>(null) }
    var rawInputCode by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }
    var codeErrorMessage by remember { mutableStateOf<String?>(null) }
    var unlockedSuccessTree by remember { mutableStateOf<TreeCatalogItem?>(null) }

    val ownedTrees by viewModel.ownedTrees
    val activeTreeId by viewModel.activeTreeId

    val context = LocalContext.current
    var inlineInputCode by remember { mutableStateOf("") }
    var inlineErrorMessage by remember { mutableStateOf<String?>(null) }
    var isInlineRedeeming by remember { mutableStateOf(false) }

    fun openWhatsApp(message: String) {
        try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val url = "https://wa.me/918595520745?text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openInstagram() {
        try {
            val url = "https://www.instagram.com/lakshay_visuals/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("tree_collection_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tree Collection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ownedTrees.size} of ${TreeCatalog.ALL_TREES.size} unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            rawInputCode = ""
                            codeErrorMessage = null
                            showRedeemDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("redeem_code_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Redeem Code",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Info Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tap any unlocked species to select it as your active tree for all focus sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Grid of 8 Tree species
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("trees_grid")
            ) {
                items(TreeCatalog.ALL_TREES) { tree ->
                    val isOwned = ownedTrees.contains(tree.id)
                    val isActive = activeTreeId == tree.id

                    TreeCatalogCard(
                        tree = tree,
                        isOwned = isOwned,
                        isActive = isActive,
                        onSelect = {
                            if (isOwned) {
                                viewModel.selectActiveTree(tree.id)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Active tree set to ${tree.name}")
                                }
                            } else {
                                selectedTreeDetail = tree
                            }
                        },
                        onInfoClick = {
                            selectedTreeDetail = tree
                        }
                    )
                }

                // UNLOCK A TREE Section
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("unlock_a_tree_section")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UNLOCK A TREE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter your secret code to unlock a premium tree species for your forest.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = inlineInputCode,
                                onValueChange = {
                                    inlineInputCode = it
                                    inlineErrorMessage = null
                                },
                                placeholder = { Text("Enter unlock code") },
                                singleLine = true,
                                isError = inlineErrorMessage != null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("unlock_code_input")
                            )

                            if (inlineErrorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = inlineErrorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val codeToRedeem = inlineInputCode.trim()
                                    if (codeToRedeem.isNotBlank()) {
                                        isInlineRedeeming = true
                                        inlineErrorMessage = null
                                        viewModel.redeemUnlockCode(codeToRedeem) { result ->
                                            isInlineRedeeming = false
                                            when (result) {
                                                is CodeRedemptionResult.Success -> {
                                                    inlineInputCode = ""
                                                    val unlocked = TreeCatalog.findById(result.treeId)
                                                    unlockedSuccessTree = unlocked
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Success! ${unlocked.name} unlocked.")
                                                    }
                                                }
                                                is CodeRedemptionResult.Error -> {
                                                    inlineErrorMessage = result.message
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isInlineRedeeming && inlineInputCode.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("redeem_code_action_button")
                            ) {
                                if (isInlineRedeeming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("REDEEM CODE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // DON'T HAVE A CODE? Section
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dont_have_a_code_section")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "DON'T HAVE A CODE?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock premium and special trees directly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // GET CODE ON WHATSAPP Button
                            Button(
                                onClick = {
                                    openWhatsApp("Hi Lakshay, I want to unlock a tree in FocusForest.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("get_code_whatsapp_button")
                            ) {
                                Text(
                                    text = "GET CODE ON WHATSAPP",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // GET CODE ON INSTAGRAM Button
                            OutlinedButton(
                                onClick = {
                                    openInstagram()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE1306C)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("get_code_instagram_button")
                            ) {
                                Text(
                                    text = "GET CODE ON INSTAGRAM",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE1306C)
                                )
                            }
                        }
                    }
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    com.example.presentation.ads.InlineBannerAd(
                        viewModel = viewModel,
                        placement = com.example.ads.AdPlacement.FOREST_BOTTOM,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    // Code Redemption Dialog
    if (showRedeemDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRedeeming) showRedeemDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Redeem Tree Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your secret unlock code to add a premium tree species to your collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = rawInputCode,
                        onValueChange = {
                            rawInputCode = it
                            codeErrorMessage = null
                        },
                        label = { Text("Unlock Code") },
                        placeholder = { Text("e.g. SAKU-7F3K-92MX") },
                        singleLine = true,
                        isError = codeErrorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (codeErrorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = codeErrorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tip: Codes look like SAKU-XXXX-XXXX, GOLD-XXXX-XXXX, etc.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRedeeming = true
                        codeErrorMessage = null
                        viewModel.redeemUnlockCode(rawInputCode) { result ->
                            isRedeeming = false
                            when (result) {
                                is CodeRedemptionResult.Success -> {
                                    showRedeemDialog = false
                                    unlockedSuccessTree = TreeCatalog.findById(result.treeId)
                                }
                                is CodeRedemptionResult.Error -> {
                                    codeErrorMessage = result.message
                                }
                            }
                        }
                    },
                    enabled = !isRedeeming && rawInputCode.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_redeem_button")
                ) {
                    if (isRedeeming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Redeem", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRedeemDialog = false },
                    enabled = !isRedeeming,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Success Unlocked Celebration Dialog
    unlockedSuccessTree?.let { tree ->
        AlertDialog(
            onDismissRequest = { unlockedSuccessTree = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(tree.palette.primaryFoliage.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = tree.palette.primaryFoliage,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "${tree.name} Unlocked!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tree.poeticDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Set as active tree now?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectActiveTree(tree.id)
                        unlockedSuccessTree = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("${tree.name} is now your active tree!")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tree.palette.primaryFoliage
                    )
                ) {
                    Text("Set as Active", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { unlockedSuccessTree = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Tree Detail / Unlock Info Dialog
    selectedTreeDetail?.let { tree ->
        val isOwned = ownedTrees.contains(tree.id)
        val isActive = activeTreeId == tree.id

        AlertDialog(
            onDismissRequest = { selectedTreeDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    tree.palette.foliageHighlight,
                                    tree.palette.primaryFoliage
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = tree.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = tree.titleDescription,
                        style = MaterialTheme.typography.labelLarge,
                        color = tree.palette.primaryFoliage,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tree.poeticDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isOwned) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Requires an unlock code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tree-Specific WhatsApp Purchase / Unlock Button
                        Button(
                            onClick = {
                                openWhatsApp("Hi Lakshay, I want to unlock the ${tree.name} in FocusForest.")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_tree_whatsapp_button")
                        ) {
                            Text(
                                text = "UNLOCK ${tree.name.uppercase()} ON WHATSAPP",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Instagram Button
                        OutlinedButton(
                            onClick = {
                                openInstagram()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE1306C)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("get_code_instagram_button_detail")
                        ) {
                            Text(
                                text = "GET CODE ON INSTAGRAM",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE1306C)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isOwned) {
                    Button(
                        onClick = {
                            viewModel.selectActiveTree(tree.id)
                            selectedTreeDetail = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("${tree.name} set as active tree")
                            }
                        },
                        enabled = !isActive,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isActive) "Currently Active" else "Select Tree", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            selectedTreeDetail = null
                            rawInputCode = ""
                            codeErrorMessage = null
                            showRedeemDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enter Code", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedTreeDetail = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun TreeCatalogCard(
    tree: TreeCatalogItem,
    isOwned: Boolean,
    isActive: Boolean,
    onSelect: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isOwned -> MaterialTheme.colorScheme.outlineVariant
        else -> Color.Transparent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isActive) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelect() }
            .testTag("tree_card_${tree.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Emblem / Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isOwned) {
                                listOf(tree.palette.foliageHighlight, tree.palette.primaryFoliage)
                            } else {
                                listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isOwned) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = tree.name,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tree Title
            Text(
                text = tree.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Short Description
            Text(
                text = tree.titleDescription,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOwned) tree.palette.primaryFoliage else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Pill
            when {
                isActive -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                isOwned -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Locked",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
