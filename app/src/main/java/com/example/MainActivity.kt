package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.WalletViewModel
import com.example.ui.components.AddWalletDialog
import com.example.ui.components.DepositDialog
import com.example.ui.components.GatewayDialog
import com.example.ui.components.NotificationDialog
import com.example.ui.components.SupportDialog
import com.example.ui.components.WithdrawDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.VipScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EWalletApp()
            }
        }
    }
}

@Composable
fun EWalletApp(viewModel: WalletViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allUserTransactions.collectAsStateWithLifecycle()
    val linkedWallets by viewModel.linkedWallets.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val depositNumber by viewModel.depositNumber.collectAsStateWithLifecycle()

    // Admin flows
    val pendingDeposits by viewModel.pendingDeposits.collectAsStateWithLifecycle()
    val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsStateWithLifecycle()
    val allSystemTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    // Dialog flags
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showGatewayDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    // Back handling
    BackHandler(enabled = drawerState.isOpen || isAdminMode || selectedTab != MainTab.HOME) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            isAdminMode -> viewModel.toggleAdminMode()
            selectedTab != MainTab.HOME -> viewModel.setTab(MainTab.HOME)
        }
    }

    // Unauthenticated state -> AuthScreen
    if (currentUserId == null) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // User logged in / registered
            }
        )
        return
    }

    // Admin Mode Screen
    if (isAdminMode) {
        AdminScreen(
            viewModel = viewModel,
            pendingDeposits = pendingDeposits,
            pendingWithdrawals = pendingWithdrawals,
            allTransactions = allSystemTransactions,
            allUsers = allUsers,
            currentDepositNumber = depositNumber,
            onBackToUserDashboard = { viewModel.toggleAdminMode() }
        )
        return
    }

    // User Navigation Drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Drawer Header User Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        val initial = (currentUser?.fullName?.trim()?.firstOrNull() ?: 'U').uppercaseChar()
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .border(1.5.dp, CyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial.toString(),
                                color = CyanAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "User",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = currentUser?.phoneNumber ?: "",
                                color = CyanAccent,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "ব্যালেন্স: ৳${"%.2f".format(currentUser?.balance ?: 0.0)}",
                                color = EmeraldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Drawer Items
                    DrawerMenuItem(
                        icon = Icons.Default.Home,
                        label = "মূল ড্যাশবোর্ড (Home)",
                        tint = CyanAccent,
                        onClick = {
                            viewModel.setTab(MainTab.HOME)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Add,
                        label = "ডিপোজিট করুন",
                        tint = PurpleAccent,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showDepositDialog = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "টাকা উত্তোলন",
                        tint = PinkAccent,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showWithdrawDialog = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.CreditCard,
                        label = "পেমেন্ট গেটওয়ে নির্দেশিকা",
                        tint = EmeraldAccent,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showGatewayDialog = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "ওয়ালেট ও লেনদেন",
                        tint = CyanAccent,
                        onClick = {
                            viewModel.setTab(MainTab.WALLET)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.MilitaryTech,
                        label = "ভিআইপি মেম্বারশিপ",
                        tint = OrangeAccent,
                        onClick = {
                            viewModel.setTab(MainTab.VIP)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.HelpOutline,
                        label = "কাস্টমার সাপোর্ট",
                        tint = EmeraldAccent,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showSupportDialog = true
                        }
                    )

                    // Admin Panel button in drawer
                    DrawerMenuItem(
                        icon = Icons.Default.AdminPanelSettings,
                        label = "এডমিন কন্ট্রোল প্যানেল",
                        tint = PurpleAccent,
                        badge = "${pendingDeposits.size} Pending",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.toggleAdminMode()
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Divider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    DrawerMenuItem(
                        icon = Icons.Default.ExitToApp,
                        label = "লগ আউট",
                        tint = PinkAccent,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.logout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        Pair(MainTab.HOME, "Home" to Icons.Default.Home),
                        Pair(MainTab.VIP, "VIP" to Icons.Default.Star),
                        Pair(MainTab.WALLET, "Wallet" to Icons.Default.AccountBalanceWallet),
                        Pair(MainTab.ACCOUNT, "Account" to Icons.Default.Person)
                    )

                    tabs.forEach { (tab, info) ->
                        val (label, icon) = info
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) CyanAccent else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyanAccent else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyanAccent.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    MainTab.HOME -> {
                        HomeScreen(
                            user = currentUser,
                            recentTransactions = recentTransactions,
                            linkedWallets = linkedWallets,
                            unreadNotificationCount = notifications.count { !it.isRead },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenNotifications = { showNotificationDialog = true },
                            onOpenDeposit = { showDepositDialog = true },
                            onOpenWithdraw = { showWithdrawDialog = true },
                            onOpenGateway = { showGatewayDialog = true },
                            onOpenAccounts = { showAddWalletDialog = true },
                            onOpenSupport = { showSupportDialog = true },
                            onViewAllTransactions = { viewModel.setTab(MainTab.WALLET) },
                            onOpenAdmin = { viewModel.toggleAdminMode() },
                            onDeleteWallet = { id -> viewModel.deleteLinkedWallet(id) }
                        )
                    }
                    MainTab.VIP -> {
                        VipScreen(
                            user = currentUser,
                            onDepositClick = { showDepositDialog = true }
                        )
                    }
                    MainTab.WALLET -> {
                        WalletScreen(
                            user = currentUser,
                            allTransactions = allTransactions,
                            onDepositClick = { showDepositDialog = true },
                            onWithdrawClick = { showWithdrawDialog = true }
                        )
                    }
                    MainTab.ACCOUNT -> {
                        AccountScreen(
                            user = currentUser,
                            linkedWallets = linkedWallets,
                            onOpenAccounts = { showAddWalletDialog = true },
                            onOpenSupport = { showSupportDialog = true },
                            onOpenAdmin = { viewModel.toggleAdminMode() },
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }

    // Deposit Dialog
    if (showDepositDialog) {
        DepositDialog(
            depositNumber = depositNumber,
            onDismiss = { showDepositDialog = false },
            onSubmit = { method, amount, senderPhone, trxId ->
                viewModel.submitDeposit(
                    method = method,
                    amount = amount,
                    senderNumber = senderPhone,
                    trxId = trxId,
                    onSuccess = {
                        showDepositDialog = false
                        Toast.makeText(context, "ডিপোজিট রিকোয়েস্ট সফল হয়েছে! এডমিন অনুমোদনের অপেক্ষা করুন।", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentBalance = currentUser?.balance ?: 0.0,
            linkedWallets = linkedWallets,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { method, amount, receiverPhone ->
                viewModel.submitWithdrawal(
                    method = method,
                    amount = amount,
                    receiverNumber = receiverPhone,
                    onSuccess = {
                        showWithdrawDialog = false
                        Toast.makeText(context, "উত্তোলন রিকোয়েস্ট সফল হয়েছে! প্রসেসিং চলছে।", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Add Wallet Dialog
    if (showAddWalletDialog) {
        AddWalletDialog(
            onDismiss = { showAddWalletDialog = false },
            onSubmit = { method, number, type ->
                viewModel.addLinkedWallet(method, number, type) {
                    showAddWalletDialog = false
                    Toast.makeText(context, "ওয়ালেট নম্বর সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Gateway Dialog
    if (showGatewayDialog) {
        GatewayDialog(
            depositNumber = depositNumber,
            onDismiss = { showGatewayDialog = false }
        )
    }

    // Support Dialog
    if (showSupportDialog) {
        SupportDialog(
            supportPhone = depositNumber,
            onDismiss = { showSupportDialog = false }
        )
    }

    // Notifications Dialog
    if (showNotificationDialog) {
        NotificationDialog(
            notifications = notifications,
            onDismiss = { showNotificationDialog = false },
            onMarkAllRead = { viewModel.markNotificationsRead() }
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = TextPrimary,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PinkAccent.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = PinkAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
