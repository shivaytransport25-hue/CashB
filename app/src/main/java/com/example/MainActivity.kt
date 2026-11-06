package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.BusinessProfile
import com.example.data.model.CashBook
import com.example.data.model.CashTransaction
import com.example.data.model.Party
import com.example.data.repository.CashBookRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.AccountTransferDialog
import com.example.ui.components.BookEditorDialog
import com.example.ui.components.BookSwitcherSheet
import com.example.ui.components.BusinessProfileDialog
import com.example.ui.components.CustomDateRangeDialog
import com.example.ui.components.PartyEditorDialog
import com.example.ui.components.StatementExportDialog
import com.example.ui.components.ThemeSelectionDialog
import com.example.ui.components.VoucherEntrySheet
import com.example.ui.screens.CashBookRegisterScreen
import com.example.ui.screens.FinancialAuditScreen
import com.example.ui.screens.OperatorAppLockScreen
import com.example.ui.screens.PartyLedgerScreen
import com.example.ui.screens.ProfileAvatarImage
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalThemeController
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideShivayTheme
import com.example.ui.theme.ThemeAppearanceMode
import com.example.util.StatementExportHelper
import com.example.util.configureAppCheck
import com.example.viewmodel.CashBookViewModel
import com.example.viewmodel.FinancialAuditViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check initialization with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge layout
        enableEdgeToEdge()

        setContent {
            AppNavigation()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    ProvideShivayTheme {
        val themeController = LocalThemeController.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ }
                )
            } else {
                MainLedgerAppContent(
                    currentUserId = user.uid,
                    userEmail = user.email ?: user.displayName ?: "Cloud Synced",
                    currentTheme = themeController.themeMode,
                    appearanceMode = themeController.appearanceMode,
                    isDarkTheme = themeController.isDarkMode,
                    useDynamicColor = themeController.useDynamicColor,
                    onUpdateTheme = themeController.onSelectThemeMode,
                    onSelectAppearanceMode = themeController.onSelectAppearanceMode,
                    onToggleDarkTheme = themeController.onToggleDarkMode,
                    onToggleDynamicColor = themeController.onToggleDynamicColor
                )
            }
        }
    }
}

@Composable
fun MainLedgerAppContent(
    currentUserId: String,
    userEmail: String,
    currentTheme: AppThemeMode,
    appearanceMode: ThemeAppearanceMode = ThemeAppearanceMode.SYSTEM,
    isDarkTheme: Boolean,
    useDynamicColor: Boolean = false,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onSelectAppearanceMode: (ThemeAppearanceMode) -> Unit = {},
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val cashBookViewModel: CashBookViewModel = viewModel(
        key = "cb_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CashBookViewModel(CashBookRepository(db), currentUserId)
            }
        }
    )

    val auditViewModel: FinancialAuditViewModel = viewModel(key = "audit_$currentUserId")

    val uiState by cashBookViewModel.uiState.collectAsStateWithLifecycle()
    val auditState by auditViewModel.auditState.collectAsStateWithLifecycle()

    val prefs = remember(context) {
        context.getSharedPreferences("shivay_cashbook_prefs", Context.MODE_PRIVATE)
    }

    var isAppLocked by rememberSaveable {
        val lockOn = prefs.getBoolean("app_lock_enabled", false)
        val hasPass = !prefs.getString("operator_password", "").isNullOrBlank()
        mutableStateOf(lockOn && hasPass)
    }

    LaunchedEffect(Unit) {
        val savedProfile = loadBusinessProfileFromPrefs(prefs)
        cashBookViewModel.updateBusinessProfile(savedProfile)
        cashBookViewModel.clearMessages()
    }

    fun persistAndUpdateProfile(updatedProfile: BusinessProfile) {
        saveBusinessProfileToPrefs(prefs, updatedProfile)
        cashBookViewModel.updateBusinessProfile(updatedProfile)
    }

    if (isAppLocked && uiState.businessProfile.appLockEnabled && uiState.businessProfile.operatorPassword.isNotBlank()) {
        OperatorAppLockScreen(
            profile = uiState.businessProfile,
            onUnlockSuccess = { isAppLocked = false }
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Cash Book, 1 = Party Khata, 2 = Audit, 3 = Settings

    // Handle system back button when on secondary tabs
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    // Dialog / BottomSheet States
    var showVoucherSheet by remember { mutableStateOf(false) }
    var voucherSheetType by remember { mutableStateOf(CashTransaction.TYPE_CASH_IN) }
    var editingTransaction by remember { mutableStateOf<CashTransaction?>(null) }
    var preselectedPartyForVoucher by remember { mutableStateOf<Party?>(null) }
    var prefilledSettlementAmount by remember { mutableDoubleStateOf(0.0) }

    var showBookSwitcher by remember { mutableStateOf(false) }
    var showBookEditor by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<CashBook?>(null) }

    var showPartyEditor by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<Party?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showContraTransferDialog by remember { mutableStateOf(false) }
    var showBusinessProfileDialog by remember { mutableStateOf(false) }
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!jsonString.isNullOrBlank()) {
                    cashBookViewModel.restoreFromJson(jsonString)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to read backup: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.actionBannerMessage) {
        val msg = uiState.errorMessage ?: uiState.actionBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            cashBookViewModel.clearMessages()
        }
    }

    val summaryForExport = if (uiState.filterState.hasActiveFilters) {
        uiState.filteredBookSummary
    } else {
        uiState.activeBookSummary
    }
    val filterDesc = if (uiState.filterState.hasActiveFilters) {
        "Filtered (${uiState.filterState.datePreset.label})"
    } else {
        "All Entries"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CommercialRegisterTopBar(
                activeBookName = uiState.activeBook?.name ?: "Main Office Cash Book",
                bookCount = uiState.cashBooks.size.coerceAtLeast(1),
                userEmail = userEmail,
                profilePhotoPath = uiState.businessProfile.profilePhotoPath,
                appearanceMode = appearanceMode,
                isDarkTheme = isDarkTheme,
                onSwitchBookClick = { showBookSwitcher = true },
                onExportClick = { showExportDialog = true },
                onThemeClick = { showThemeDialog = true },
                onSettingsClick = { selectedTab = 3 },
                onToggleDarkMode = {
                    val nextAppearance = when (appearanceMode) {
                        ThemeAppearanceMode.LIGHT -> ThemeAppearanceMode.DARK
                        ThemeAppearanceMode.DARK -> ThemeAppearanceMode.SYSTEM
                        ThemeAppearanceMode.SYSTEM -> ThemeAppearanceMode.LIGHT
                    }
                    onSelectAppearanceMode(nextAppearance)
                },
                onSignOutClick = {
                    signOut(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {},
                        scope = scope
                    )
                }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Cash Book Register"
                            )
                        },
                        label = {
                            Text(
                                text = "Cash Book",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_cashbook")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Party Khata Ledger"
                            )
                        },
                        label = {
                            Text(
                                text = "Party Ledger",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_parties")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Audit & Forecast"
                            )
                        },
                        label = {
                            Text(
                                text = "Audit",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_audit")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings & Profile"
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "ledger_tab_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetTab ->
            when (targetTab) {
                0 -> {
                    CashBookRegisterScreen(
                        uiState = uiState,
                        onCashInClick = {
                            editingTransaction = null
                            preselectedPartyForVoucher = null
                            prefilledSettlementAmount = 0.0
                            voucherSheetType = CashTransaction.TYPE_CASH_IN
                            showVoucherSheet = true
                        },
                        onCashOutClick = {
                            editingTransaction = null
                            preselectedPartyForVoucher = null
                            prefilledSettlementAmount = 0.0
                            voucherSheetType = CashTransaction.TYPE_CASH_OUT
                            showVoucherSheet = true
                        },
                        onContraTransferClick = {
                            showContraTransferDialog = true
                        },
                        onEditTransactionClick = { tx ->
                            editingTransaction = tx
                            preselectedPartyForVoucher = null
                            voucherSheetType = tx.type
                            showVoucherSheet = true
                        },
                        onDeleteTransactionClick = { tx ->
                            cashBookViewModel.deleteTransaction(tx.id)
                        },
                        onSearchQueryChange = cashBookViewModel::updateSearchQuery,
                        onDatePresetChange = cashBookViewModel::updateDatePreset,
                        onCustomDateRangeClick = { showCustomDateRangeDialog = true },
                        onEntryTypeFilterChange = cashBookViewModel::updateEntryTypeFilter,
                        onPaymentModeFilterChange = cashBookViewModel::updatePaymentModeFilter,
                        onCategoryFilterChange = cashBookViewModel::updateCategoryFilter,
                        onPartyFilterChange = cashBookViewModel::updatePartyFilter,
                        onClearFilters = cashBookViewModel::clearAllFilters,
                        onExportStatementClick = { showExportDialog = true },
                        onSeedSampleVouchers = cashBookViewModel::seedSampleBusinessEntries,
                        onToggleHideBalance = {
                            val currentHidden = uiState.businessProfile.hideBalanceOnHome
                            persistAndUpdateProfile(uiState.businessProfile.copy(hideBalanceOnHome = !currentHidden))
                        }
                    )
                }
                1 -> {
                    PartyLedgerScreen(
                        uiState = uiState,
                        onAddPartyClick = {
                            editingParty = null
                            showPartyEditor = true
                        },
                        onEditPartyClick = { party ->
                            editingParty = party
                            showPartyEditor = true
                        },
                        onDeletePartyClick = cashBookViewModel::deleteParty,
                        onViewPartyTransactions = { party ->
                            cashBookViewModel.updatePartyFilter(party.id)
                            selectedTab = 0
                        },
                        onSettlePartyClick = { party, defaultType, defaultAmount ->
                            editingTransaction = if (defaultAmount > 0.0) {
                                CashTransaction(
                                    type = defaultType,
                                    amount = defaultAmount,
                                    remark = "Account settlement - ${party.name}",
                                    category = if (defaultType == CashTransaction.TYPE_CASH_IN) {
                                        "Party Recovery"
                                    } else {
                                        "Supplier Payment"
                                    },
                                    paymentMode = "Bank Transfer",
                                    partyId = party.id,
                                    partyName = party.name,
                                    transactionDate = System.currentTimeMillis()
                                )
                            } else {
                                null
                            }
                            preselectedPartyForVoucher = party
                            voucherSheetType = defaultType
                            showVoucherSheet = true
                        },
                        onEditTransactionClick = { tx ->
                            editingTransaction = tx
                            preselectedPartyForVoucher = null
                            voucherSheetType = tx.type
                            showVoucherSheet = true
                        },
                        onDeleteTransactionClick = { tx ->
                            cashBookViewModel.deleteTransaction(tx.id)
                        },
                        onExportPartyStatement = { party ->
                            try {
                                val partyTxs = uiState.allTransactions.filter { it.partyId == party.id }
                                val pdfFile = StatementExportHelper.exportPartyStatementPdf(
                                    context = context,
                                    profile = uiState.businessProfile,
                                    party = party,
                                    partyTransactions = partyTxs,
                                    currencySymbol = uiState.activeBookSummary.currencySymbol
                                )
                                StatementExportHelper.shareExportedFile(
                                    context = context,
                                    file = pdfFile,
                                    mimeType = "application/pdf",
                                    chooserTitle = "Share Party Statement PDF (${party.name})"
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to generate statement: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onSharePartyReminder = { party, netBalance ->
                            val pos = if (netBalance > 0) {
                                "Receivable balance of ${StatementExportHelper.formatCurrency(netBalance, "₹")} Dr (You'll Get)"
                            } else {
                                "Payable balance of ${StatementExportHelper.formatCurrency(kotlin.math.abs(netBalance), "₹")} Cr (You'll Give)"
                            }
                            val reminderText = "Dear ${party.name},\n" +
                                "This is a statement update from ${uiState.businessName}.\n" +
                                "Your current ledger position is: $pos.\n" +
                                "Contact: ${uiState.businessProfile.phone}\n" +
                                "Please review and confirm. Thank you!"
                            StatementExportHelper.shareTextMessage(
                                context = context,
                                text = reminderText,
                                chooserTitle = "Share Khata Reminder via WhatsApp / SMS"
                            )
                        }
                    )
                }
                2 -> {
                    FinancialAuditScreen(
                        uiState = uiState,
                        auditState = auditState,
                        onRunFullAudit = {
                            auditViewModel.runComprehensiveAudit(uiState)
                        },
                        onAskCustomQuery = { query ->
                            auditViewModel.askCustomAuditQuery(query, uiState)
                        },
                        onEditBusinessProfileClick = {
                            showBusinessProfileDialog = true
                        },
                        onPrintStatementClick = {
                            try {
                                val pdfFile = StatementExportHelper.exportToPdfFile(
                                    context = context,
                                    summary = summaryForExport,
                                    rows = uiState.transactionsWithBalance,
                                    filterLabel = filterDesc,
                                    businessName = uiState.businessName,
                                    gstin = uiState.businessGstin
                                )
                                StatementExportHelper.printPdfFile(
                                    context = context,
                                    pdfFile = pdfFile,
                                    jobName = "${uiState.businessName} - ${summaryForExport.bookName} Statement"
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Print error: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onExportBackupClick = {
                            try {
                                val jsonFile = StatementExportHelper.exportFullBackupJson(
                                    context = context,
                                    state = uiState,
                                    profile = uiState.businessProfile
                                )
                                StatementExportHelper.shareExportedFile(
                                    context = context,
                                    file = jsonFile,
                                    mimeType = "application/json",
                                    chooserTitle = "Save / Share Full CashBook JSON Backup"
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Backup error: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
                3 -> {
                    SettingsScreen(
                        profile = uiState.businessProfile,
                        userEmail = userEmail,
                        currentTheme = currentTheme,
                        appearanceMode = appearanceMode,
                        isDarkTheme = isDarkTheme,
                        useDynamicColor = useDynamicColor,
                        onSaveProfile = { updatedProfile ->
                            persistAndUpdateProfile(updatedProfile)
                        },
                        onSelectTheme = onUpdateTheme,
                        onSelectAppearanceMode = onSelectAppearanceMode,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onToggleDynamicColor = onToggleDynamicColor,
                        onLockAppNow = { isAppLocked = true },
                        onExportPdf = { showExportDialog = true },
                        onExportCsv = { showExportDialog = true },
                        onExportBackupJson = {
                            try {
                                val file = StatementExportHelper.exportFullBackupJson(
                                    context = context,
                                    state = uiState,
                                    profile = uiState.businessProfile
                                )
                                StatementExportHelper.shareExportedFile(
                                    context = context,
                                    file = file,
                                    mimeType = "application/json",
                                    chooserTitle = "Export Full CashBook JSON Backup"
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to generate backup: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onImportBackupJson = {
                            try {
                                backupPickerLauncher.launch("application/json")
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Cannot open file picker: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onSignOut = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }

    // Voucher Entry / Edit Sheet
    if (showVoucherSheet) {
        val txForSheet = editingTransaction
        val isRealExistingVoucher = txForSheet != null && txForSheet.id.isNotBlank()
        VoucherEntrySheet(
            initialType = voucherSheetType,
            existingTransaction = txForSheet,
            currencySymbol = uiState.activeBookSummary.currencySymbol,
            parties = uiState.parties,
            preselectedParty = preselectedPartyForVoucher,
            customCashInCategories = uiState.businessProfile.customCashInCategories,
            customCashOutCategories = uiState.businessProfile.customCashOutCategories,
            defaultPaymentMode = uiState.businessProfile.defaultPaymentMode,
            onDismiss = {
                showVoucherSheet = false
                editingTransaction = null
                preselectedPartyForVoucher = null
            },
            onSave = { type, amount, remark, category, paymentMode, partyId, partyName, txDate ->
                if (isRealExistingVoucher && txForSheet != null) {
                    cashBookViewModel.updateTransaction(
                        txForSheet.copy(
                            type = type,
                            amount = amount,
                            remark = remark,
                            category = category,
                            paymentMode = paymentMode,
                            partyId = partyId,
                            partyName = partyName,
                            transactionDate = txDate
                        )
                    )
                } else {
                    cashBookViewModel.addTransaction(
                        type = type,
                        amount = amount,
                        remark = remark,
                        category = category,
                        paymentMode = paymentMode,
                        partyId = partyId,
                        partyName = partyName,
                        transactionDate = txDate
                    )
                }
            },
            onDelete = if (isRealExistingVoucher) {
                { id -> cashBookViewModel.deleteTransaction(id) }
            } else {
                null
            },
            onPrintVoucherReceipt = if (isRealExistingVoucher) {
                { tx ->
                    try {
                        val file = StatementExportHelper.exportSingleVoucherReceiptPdf(
                            context = context,
                            businessName = uiState.businessName,
                            businessGstin = uiState.businessGstin,
                            businessAddress = uiState.businessProfile.address,
                            bookName = uiState.activeBookSummary.bookName,
                            currencySymbol = uiState.activeBookSummary.currencySymbol,
                            tx = tx
                        )
                        StatementExportHelper.shareExportedFile(
                            context = context,
                            file = file,
                            mimeType = "application/pdf",
                            chooserTitle = "Share / Print Voucher Receipt PDF"
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to generate Receipt PDF: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                null
            },
            onReverseVoucher = if (isRealExistingVoucher) {
                { tx -> cashBookViewModel.reverseTransaction(tx) }
            } else {
                null
            },
            onQuickCreateParty = { name, phone, role ->
                cashBookViewModel.createParty(name, phone, role, "")
            }
        )
    }

    // Contra Account-to-Account Transfer Dialog
    if (showContraTransferDialog) {
        AccountTransferDialog(
            books = uiState.cashBooks,
            activeBookId = uiState.activeBook?.id,
            onDismiss = { showContraTransferDialog = false },
            onCreateNewBankBook = {
                editingBook = null
                showBookEditor = true
            },
            onConfirmTransfer = { fromBookId, toBookId, amount, paymentMode, remark ->
                cashBookViewModel.transferBetweenBooks(
                    fromBookId = fromBookId,
                    toBookId = toBookId,
                    amount = amount,
                    paymentMode = paymentMode,
                    remark = remark
                )
            }
        )
    }

    // Business Profile Editor Dialog
    if (showBusinessProfileDialog) {
        BusinessProfileDialog(
            currentProfile = uiState.businessProfile,
            onDismiss = { showBusinessProfileDialog = false },
            onSave = { updatedProfile ->
                persistAndUpdateProfile(updatedProfile)
            }
        )
    }

    // Custom Date Range Dialog
    if (showCustomDateRangeDialog) {
        CustomDateRangeDialog(
            initialStartDate = uiState.filterState.customStartDate,
            initialEndDate = uiState.filterState.customEndDate,
            onDismiss = { showCustomDateRangeDialog = false },
            onApplyRange = { start, end ->
                cashBookViewModel.updateCustomDateRange(start, end)
            }
        )
    }

    // Cash Book Switcher Sheet
    if (showBookSwitcher) {
        BookSwitcherSheet(
            books = uiState.cashBooks,
            activeBookId = uiState.activeBook?.id,
            onSelectBook = cashBookViewModel::selectCashBook,
            onCreateBookClick = {
                editingBook = null
                showBookEditor = true
            },
            onEditBookClick = { book ->
                editingBook = book
                showBookEditor = true
            },
            onDeleteBookClick = { book ->
                cashBookViewModel.deleteCashBook(book.id)
            },
            onDismiss = { showBookSwitcher = false }
        )
    }

    // Cash Book Create / Edit Dialog
    if (showBookEditor) {
        val bookToEdit = editingBook
        BookEditorDialog(
            existingBook = bookToEdit,
            onDismiss = {
                showBookEditor = false
                editingBook = null
            },
            onSave = { name, description, currencySymbol ->
                if (bookToEdit != null) {
                    cashBookViewModel.updateCashBook(bookToEdit, name, description, currencySymbol)
                } else {
                    cashBookViewModel.createCashBook(name, description, currencySymbol)
                }
            }
        )
    }

    // Party Create / Edit Dialog
    if (showPartyEditor) {
        val partyToEdit = editingParty
        PartyEditorDialog(
            existingParty = partyToEdit,
            onDismiss = {
                showPartyEditor = false
                editingParty = null
            },
            onSave = { name, phone, role, notes ->
                if (partyToEdit != null) {
                    cashBookViewModel.updateParty(partyToEdit, name, phone, role, notes)
                } else {
                    cashBookViewModel.createParty(name, phone, role, notes)
                }
            }
        )
    }

    // PDF / Excel CSV / Print / JSON Backup Statement Export Dialog
    if (showExportDialog) {
        StatementExportDialog(
            summary = summaryForExport,
            entryCount = uiState.transactionsWithBalance.size,
            onExportPdf = {
                try {
                    val file = StatementExportHelper.exportToPdfFile(
                        context = context,
                        summary = summaryForExport,
                        rows = uiState.transactionsWithBalance,
                        filterLabel = filterDesc,
                        businessName = uiState.businessName,
                        gstin = uiState.businessGstin
                    )
                    StatementExportHelper.shareExportedFile(
                        context = context,
                        file = file,
                        mimeType = "application/pdf",
                        chooserTitle = "Share / Print A4 Statement PDF"
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Failed to generate PDF: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onPrintPdf = {
                try {
                    val file = StatementExportHelper.exportToPdfFile(
                        context = context,
                        summary = summaryForExport,
                        rows = uiState.transactionsWithBalance,
                        filterLabel = filterDesc,
                        businessName = uiState.businessName,
                        gstin = uiState.businessGstin
                    )
                    StatementExportHelper.printPdfFile(
                        context = context,
                        pdfFile = file,
                        jobName = "${uiState.businessName} - ${summaryForExport.bookName} Statement"
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Failed to print PDF: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onExportCsv = {
                try {
                    val file = StatementExportHelper.exportToCsvFile(
                        context = context,
                        summary = summaryForExport,
                        rows = uiState.transactionsWithBalance,
                        filterLabel = filterDesc,
                        businessName = uiState.businessName
                    )
                    StatementExportHelper.shareExportedFile(
                        context = context,
                        file = file,
                        mimeType = "text/csv",
                        chooserTitle = "Export Excel Register CSV"
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Failed to generate CSV: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onExportBackupJson = {
                try {
                    val file = StatementExportHelper.exportFullBackupJson(
                        context = context,
                        state = uiState,
                        profile = uiState.businessProfile
                    )
                    StatementExportHelper.shareExportedFile(
                        context = context,
                        file = file,
                        mimeType = "application/json",
                        chooserTitle = "Export Full CashBook JSON Backup"
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Failed to generate backup: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onImportBackupJson = {
                try {
                    backupPickerLauncher.launch("application/json")
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Cannot open file picker: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            isDarkTheme = isDarkTheme,
            appearanceMode = appearanceMode,
            useDynamicColor = useDynamicColor,
            onDismiss = { showThemeDialog = false },
            onSelectTheme = onUpdateTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onSelectAppearanceMode = onSelectAppearanceMode,
            onToggleDynamicColor = onToggleDynamicColor
        )
    }
}

@Composable
private fun CommercialRegisterTopBar(
    activeBookName: String,
    bookCount: Int,
    userEmail: String,
    profilePhotoPath: String = "",
    appearanceMode: ThemeAppearanceMode,
    isDarkTheme: Boolean,
    onSwitchBookClick: () -> Unit,
    onExportClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onToggleDarkMode: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Book Selector Button with Operator Profile Photo or Shivay Emblem Logo
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                    .clickable(onClick = onSwitchBookClick)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("switch_cashbook_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatarImage(
                    photoPath = profilePhotoPath,
                    sizeDp = 30
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeBookName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SYNCED ($bookCount BOOKS) • $userEmail",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Switch Cash Book",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            val modeIcon = when (appearanceMode) {
                ThemeAppearanceMode.LIGHT -> Icons.Default.LightMode
                ThemeAppearanceMode.DARK -> Icons.Default.DarkMode
                ThemeAppearanceMode.SYSTEM -> Icons.Default.SettingsBrightness
            }

            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.testTag("topbar_dark_mode_toggle")
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = "Display Mode: ${appearanceMode.displayName} (Tap to switch Light / Dark / System)",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onThemeClick,
                modifier = Modifier.testTag("topbar_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change Theme / Dynamic Color / Black & White Mode",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("topbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings, Profile Photo & Security Lock",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onExportClick,
                modifier = Modifier.testTag("topbar_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export PDF or Excel Statement",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun loadBusinessProfileFromPrefs(prefs: android.content.SharedPreferences): BusinessProfile {
    val def = BusinessProfile()
    val customInRaw = prefs.getString("custom_cash_in_categories", "") ?: ""
    val customOutRaw = prefs.getString("custom_cash_out_categories", "") ?: ""
    return BusinessProfile(
        businessName = prefs.getString("biz_name", def.businessName) ?: def.businessName,
        ownerName = prefs.getString("owner_name", def.ownerName) ?: def.ownerName,
        operatorUsername = prefs.getString("operator_username", def.operatorUsername) ?: def.operatorUsername,
        operatorPassword = prefs.getString("operator_password", def.operatorPassword) ?: def.operatorPassword,
        profilePhotoPath = prefs.getString("profile_photo_path", def.profilePhotoPath) ?: def.profilePhotoPath,
        phone = prefs.getString("biz_phone", def.phone) ?: def.phone,
        email = prefs.getString("biz_email", def.email) ?: def.email,
        address = prefs.getString("biz_address", def.address) ?: def.address,
        gstin = prefs.getString("biz_gstin", def.gstin) ?: def.gstin,
        pan = prefs.getString("biz_pan", def.pan) ?: def.pan,
        currencySymbol = prefs.getString("biz_currency", def.currencySymbol) ?: def.currencySymbol,
        financialYear = prefs.getString("biz_fy", def.financialYear) ?: def.financialYear,
        appLockEnabled = prefs.getBoolean("app_lock_enabled", def.appLockEnabled),
        hideBalanceOnHome = prefs.getBoolean("hide_balance_on_home", def.hideBalanceOnHome),
        backDateLockEnabled = prefs.getBoolean("backdate_lock_enabled", def.backDateLockEnabled),
        backDateLockDays = prefs.getInt("backdate_lock_days", def.backDateLockDays),
        defaultPaymentMode = prefs.getString("default_payment_mode", def.defaultPaymentMode) ?: def.defaultPaymentMode,
        voucherPrefix = prefs.getString("voucher_prefix", def.voucherPrefix) ?: def.voucherPrefix,
        pdfSignatoryName = prefs.getString("pdf_signatory_name", def.pdfSignatoryName) ?: def.pdfSignatoryName,
        pdfFooterNote = prefs.getString("pdf_footer_note", def.pdfFooterNote) ?: def.pdfFooterNote,
        pdfShowRunningBalance = prefs.getBoolean("pdf_show_running_bal", def.pdfShowRunningBalance),
        pdfShowCategoryAndMode = prefs.getBoolean("pdf_show_cat_mode", def.pdfShowCategoryAndMode),
        dailyReminderEnabled = prefs.getBoolean("daily_reminder_enabled", def.dailyReminderEnabled),
        dailyReminderTime = prefs.getString("daily_reminder_time", def.dailyReminderTime) ?: def.dailyReminderTime,
        whatsappReminderTemplate = prefs.getString("whatsapp_reminder_template", def.whatsappReminderTemplate) ?: def.whatsappReminderTemplate,
        appLanguage = prefs.getString("app_language", def.appLanguage) ?: def.appLanguage,
        customCashInCategories = customInRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() },
        customCashOutCategories = customOutRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    )
}

private fun saveBusinessProfileToPrefs(
    prefs: android.content.SharedPreferences,
    profile: BusinessProfile
) {
    prefs.edit()
        .putString("biz_name", profile.businessName)
        .putString("owner_name", profile.ownerName)
        .putString("operator_username", profile.operatorUsername)
        .putString("operator_password", profile.operatorPassword)
        .putString("profile_photo_path", profile.profilePhotoPath)
        .putString("biz_phone", profile.phone)
        .putString("biz_email", profile.email)
        .putString("biz_address", profile.address)
        .putString("biz_gstin", profile.gstin)
        .putString("biz_pan", profile.pan)
        .putString("biz_currency", profile.currencySymbol)
        .putString("biz_fy", profile.financialYear)
        .putBoolean("app_lock_enabled", profile.appLockEnabled)
        .putBoolean("hide_balance_on_home", profile.hideBalanceOnHome)
        .putBoolean("backdate_lock_enabled", profile.backDateLockEnabled)
        .putInt("backdate_lock_days", profile.backDateLockDays)
        .putString("default_payment_mode", profile.defaultPaymentMode)
        .putString("voucher_prefix", profile.voucherPrefix)
        .putString("pdf_signatory_name", profile.pdfSignatoryName)
        .putString("pdf_footer_note", profile.pdfFooterNote)
        .putBoolean("pdf_show_running_bal", profile.pdfShowRunningBalance)
        .putBoolean("pdf_show_cat_mode", profile.pdfShowCategoryAndMode)
        .putBoolean("daily_reminder_enabled", profile.dailyReminderEnabled)
        .putString("daily_reminder_time", profile.dailyReminderTime)
        .putString("whatsapp_reminder_template", profile.whatsappReminderTemplate)
        .putString("app_language", profile.appLanguage)
        .putString("custom_cash_in_categories", profile.customCashInCategories.joinToString("|"))
        .putString("custom_cash_out_categories", profile.customCashOutCategories.joinToString("|"))
        .apply()
}
