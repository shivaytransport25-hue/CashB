package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.data.model.CashTransaction
import com.example.ui.components.LedgerSelectableChip
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ThemeAppearanceMode
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: BusinessProfile,
    userEmail: String,
    currentTheme: AppThemeMode,
    appearanceMode: ThemeAppearanceMode,
    isDarkTheme: Boolean,
    useDynamicColor: Boolean,
    onSaveProfile: (BusinessProfile) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectAppearanceMode: (ThemeAppearanceMode) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onExportBackupJson: () -> Unit,
    onImportBackupJson: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    // Operator Account & Security State
    var operatorUsername by remember(profile.operatorUsername) { mutableStateOf(profile.operatorUsername) }
    var operatorPassword by remember(profile.operatorPassword) { mutableStateOf(profile.operatorPassword) }
    var showPasswordText by remember { mutableStateOf(false) }
    var profilePhotoPath by remember(profile.profilePhotoPath) { mutableStateOf(profile.profilePhotoPath) }
    var appLockEnabled by remember(profile.appLockEnabled) { mutableStateOf(profile.appLockEnabled) }

    // Privacy & Protection State
    var hideBalanceOnHome by remember(profile.hideBalanceOnHome) { mutableStateOf(profile.hideBalanceOnHome) }
    var backDateLockEnabled by remember(profile.backDateLockEnabled) { mutableStateOf(profile.backDateLockEnabled) }
    var backDateLockDays by remember(profile.backDateLockDays) { mutableIntStateOf(profile.backDateLockDays) }

    // Voucher & Custom Category State
    var defaultPaymentMode by remember(profile.defaultPaymentMode) { mutableStateOf(profile.defaultPaymentMode) }
    var voucherPrefix by remember(profile.voucherPrefix) { mutableStateOf(profile.voucherPrefix) }
    var customCashInCategories by remember(profile.customCashInCategories) { mutableStateOf(profile.customCashInCategories) }
    var customCashOutCategories by remember(profile.customCashOutCategories) { mutableStateOf(profile.customCashOutCategories) }
    var newCategoryInput by remember { mutableStateOf("") }

    // Business Profile & PDF Customization State
    var businessName by remember(profile.businessName) { mutableStateOf(profile.businessName) }
    var ownerName by remember(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var address by remember(profile.address) { mutableStateOf(profile.address) }
    var gstin by remember(profile.gstin) { mutableStateOf(profile.gstin) }
    var pan by remember(profile.pan) { mutableStateOf(profile.pan) }
    var currencySymbol by remember(profile.currencySymbol) { mutableStateOf(profile.currencySymbol) }
    var financialYear by remember(profile.financialYear) { mutableStateOf(profile.financialYear) }
    var pdfSignatoryName by remember(profile.pdfSignatoryName) { mutableStateOf(profile.pdfSignatoryName) }
    var pdfFooterNote by remember(profile.pdfFooterNote) { mutableStateOf(profile.pdfFooterNote) }
    var pdfShowRunningBalance by remember(profile.pdfShowRunningBalance) { mutableStateOf(profile.pdfShowRunningBalance) }
    var pdfShowCategoryAndMode by remember(profile.pdfShowCategoryAndMode) { mutableStateOf(profile.pdfShowCategoryAndMode) }

    // Reminders & Language State
    var dailyReminderEnabled by remember(profile.dailyReminderEnabled) { mutableStateOf(profile.dailyReminderEnabled) }
    var dailyReminderTime by remember(profile.dailyReminderTime) { mutableStateOf(profile.dailyReminderTime) }
    var whatsappReminderTemplate by remember(profile.whatsappReminderTemplate) { mutableStateOf(profile.whatsappReminderTemplate) }
    var appLanguage by remember(profile.appLanguage) { mutableStateOf(profile.appLanguage) }

    var statusBanner by remember { mutableStateOf<String?>(null) }

    fun buildUpdatedProfile(): BusinessProfile {
        return profile.copy(
            businessName = businessName.trim().ifEmpty { "Shivay CashBook" },
            ownerName = ownerName.trim().ifEmpty { "Owner" },
            operatorUsername = operatorUsername.trim().ifEmpty { "shivay_admin" },
            operatorPassword = operatorPassword.trim(),
            profilePhotoPath = profilePhotoPath,
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            gstin = gstin.trim().uppercase(),
            pan = pan.trim().uppercase(),
            currencySymbol = currencySymbol.trim().ifEmpty { "₹" },
            financialYear = financialYear.trim().ifEmpty { "2026-2027" },
            appLockEnabled = appLockEnabled,
            hideBalanceOnHome = hideBalanceOnHome,
            backDateLockEnabled = backDateLockEnabled,
            backDateLockDays = backDateLockDays,
            defaultPaymentMode = defaultPaymentMode,
            voucherPrefix = voucherPrefix.trim().uppercase().ifEmpty { "VCH" },
            pdfSignatoryName = pdfSignatoryName.trim().ifEmpty { "Authorized Signatory" },
            pdfFooterNote = pdfFooterNote.trim(),
            pdfShowRunningBalance = pdfShowRunningBalance,
            pdfShowCategoryAndMode = pdfShowCategoryAndMode,
            dailyReminderEnabled = dailyReminderEnabled,
            dailyReminderTime = dailyReminderTime,
            whatsappReminderTemplate = whatsappReminderTemplate.trim(),
            appLanguage = appLanguage,
            customCashInCategories = customCashInCategories,
            customCashOutCategories = customCashOutCategories
        )
    }

    // Zero-permission Android Photo Picker for Profile Photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val destFile = File(context.filesDir, "shivay_profile_photo_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0L) {
                    profilePhotoPath = destFile.absolutePath
                    val updated = buildUpdatedProfile().copy(profilePhotoPath = destFile.absolutePath)
                    onSaveProfile(updated)
                    statusBanner = "प्रोफाइल फोटो सेव हो गई (Profile photo updated!)"
                }
            } catch (e: Exception) {
                statusBanner = "फोटो लोड नहीं हो सकी: ${e.localizedMessage}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Banner
        if (!statusBanner.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CashInGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CashInGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CashInGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { statusBanner = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = CashInGreen)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1. OPERATOR PROFILE PHOTO, USERNAME & PASSWORD SECURITY CARD
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "1. PROFILE PHOTO, USERNAME & PASSWORD (प्रोफाइल और पासवर्ड लॉक)",
                icon = Icons.Default.AccountCircle
            ) {
                // Profile Photo Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileAvatarImage(
                        photoPath = profilePhotoPath,
                        sizeDp = 72,
                        modifier = Modifier
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("settings_profile_photo_avatar")
                    )

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = ownerName.ifBlank { "Shivay Operator" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "@${operatorUsername.ifBlank { "shivay_admin" }} • $userEmail",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("change_profile_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Photo (फोटो बदलें)", style = MaterialTheme.typography.labelSmall)
                            }

                            if (profilePhotoPath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        profilePhotoPath = ""
                                        onSaveProfile(buildUpdatedProfile().copy(profilePhotoPath = ""))
                                        statusBanner = "डिफ़ॉल्ट शिवय लोगो सेट किया गया (Reset to default logo)"
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Photo",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))

                // Operator Username & Security Password / PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = operatorUsername,
                        onValueChange = { operatorUsername = it },
                        label = { Text("Username (यूज़रनेम)") },
                        placeholder = { Text("e.g. shivay_admin") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_username_input")
                    )

                    OutlinedTextField(
                        value = operatorPassword,
                        onValueChange = { operatorPassword = it },
                        label = { Text("Password / PIN (पासवर्ड)") },
                        placeholder = { Text("e.g. 1234 or secret") },
                        singleLine = true,
                        visualTransformation = if (showPasswordText) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPasswordText = !showPasswordText }) {
                                Icon(
                                    imageVector = if (showPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_password_input")
                    )
                }

                // App Lock Toggle & Lock Now Action
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Username & Password Lock (ऐप लॉक)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (operatorPassword.isBlank()) {
                                    "ऊपर पासवर्ड या 4-अंकों का PIN डालें और लॉक चालू करें"
                                } else if (appLockEnabled) {
                                    "ऐप खुलते समय पासवर्ड मांगा जाएगा (Active)"
                                } else {
                                    "पासवर्ड सेट है — स्विच ऑन करके ऐप लॉक चालू करें"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && operatorPassword.isBlank()) {
                                    operatorPassword = "1234"
                                    statusBanner = "डिफ़ॉल्ट PIN '1234' सेट किया गया — आप इसे बदल सकते हैं"
                                }
                                appLockEnabled = enabled
                                onSaveProfile(buildUpdatedProfile().copy(appLockEnabled = enabled, operatorPassword = operatorPassword))
                            },
                            modifier = Modifier.testTag("settings_app_lock_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = buildUpdatedProfile()
                            onSaveProfile(updated)
                            statusBanner = "यूज़रनेम, पासवर्ड और प्रोफाइल सेव हो गए!"
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("save_credentials_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Credentials", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            if (operatorPassword.isBlank()) {
                                operatorPassword = "1234"
                            }
                            appLockEnabled = true
                            onSaveProfile(buildUpdatedProfile().copy(appLockEnabled = true, operatorPassword = operatorPassword))
                            onLockAppNow()
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("lock_app_now_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock App Now (लॉक करें)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // =========================================================================
        // 2. PRIVACY & ENTRY PROTECTION SETTINGS
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "2. PRIVACY & SECURITY CONTROLS (सुरक्षा और प्राइवेसी)",
                icon = Icons.Default.Security
            ) {
                // Hide Balance on Home Screen
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Balance on Home Screen (बैलेंस छुपाएं ₹ ****)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "रजिस्टर के ऊपर मुख्य Cash In / Out / Net Balance को ₹ **** से छुपाता है",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hideBalanceOnHome,
                            onCheckedChange = { hidden ->
                                hideBalanceOnHome = hidden
                                onSaveProfile(buildUpdatedProfile().copy(hideBalanceOnHome = hidden))
                            },
                            modifier = Modifier.testTag("settings_hide_balance_switch")
                        )
                    }
                }

                // Back-Date Entry Lock
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Back-Date Edit/Delete Protection (पुरानी एंट्री लॉक)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "पुरानी एंट्री को गलती से डिलीट या बदलने से रोकता है",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = backDateLockEnabled,
                                onCheckedChange = { locked ->
                                    backDateLockEnabled = locked
                                    onSaveProfile(buildUpdatedProfile().copy(backDateLockEnabled = locked))
                                },
                                modifier = Modifier.testTag("settings_backdate_lock_switch")
                            )
                        }

                        if (backDateLockEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lock entries older than:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                listOf(1, 3, 7, 15, 30).forEach { days ->
                                    LedgerSelectableChip(
                                        label = "$days Days",
                                        selected = backDateLockDays == days,
                                        onClick = {
                                            backDateLockDays = days
                                            onSaveProfile(buildUpdatedProfile().copy(backDateLockDays = days))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. DISPLAY MODE (LIGHT / DARK / SYSTEM), THEMES & LANGUAGE
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "3. DISPLAY MODE, COLOR THEMES & LANGUAGE (थीम और भाषा)",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = "DISPLAY APPEARANCE (LIGHT • DARK • SYSTEM):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeAppearanceMode.entries.forEach { mode ->
                        val selected = appearanceMode == mode
                        val icon = when (mode) {
                            ThemeAppearanceMode.LIGHT -> Icons.Default.LightMode
                            ThemeAppearanceMode.DARK -> Icons.Default.DarkMode
                            ThemeAppearanceMode.SYSTEM -> Icons.Default.SettingsBrightness
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectAppearanceMode(mode) }
                                .testTag("settings_appearance_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(4.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode.displayName,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = mode.hindiName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Dynamic Color Toggle
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Material 3 Dynamic Color (डायनामिक कलर)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Android 12+ वॉलपेपर से ऑटोमैटिक कलर स्कीम",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColor || currentTheme == AppThemeMode.DYNAMIC_WALLPAPER,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                }

                // Color Theme Chips
                Text(
                    text = "COLOR THEME PALETTE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        LedgerSelectableChip(
                            label = "${mode.displayName} (${mode.hindiName})",
                            selected = currentTheme == mode,
                            onClick = { onSelectTheme(mode) }
                        )
                    }
                }

                // App Language Preference
                Text(
                    text = "APP LANGUAGE (भाषा चुनाव):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Hinglish (हिंदी + English)",
                        "हिंदी (Hindi)",
                        "English (Commercial)"
                    ).forEach { lang ->
                        LedgerSelectableChip(
                            label = lang,
                            selected = appLanguage == lang,
                            onClick = {
                                appLanguage = lang
                                onSaveProfile(buildUpdatedProfile().copy(appLanguage = lang))
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. VOUCHER DEFAULTS & CUSTOM CATEGORIES MANAGER
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "4. CUSTOM CATEGORIES & VOUCHER SETTINGS (कैटेगरी और वाउचर)",
                icon = Icons.Default.Category
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = voucherPrefix,
                        onValueChange = { voucherPrefix = it.uppercase().take(10) },
                        label = { Text("Voucher Prefix") },
                        placeholder = { Text("VCH / SHIVAY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = "Default Payment Mode:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CashTransaction.PAYMENT_MODES.forEach { mode ->
                                LedgerSelectableChip(
                                    label = mode,
                                    selected = defaultPaymentMode == mode,
                                    onClick = {
                                        defaultPaymentMode = mode
                                        onSaveProfile(buildUpdatedProfile().copy(defaultPaymentMode = mode))
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "ADD CUSTOM CATEGORY (नई कैटेगरी जोड़ें - जैसे कांटा पर्ची, गाड़ी भाड़ा):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("New Category Name (नई कैटेगरी का नाम)") },
                    placeholder = { Text("e.g. कांटा पर्ची (Weighbridge), चाय-पानी, गोदाम किराया") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_category_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clean = newCategoryInput.trim()
                            if (clean.isNotEmpty() && clean !in customCashInCategories) {
                                val updatedList = customCashInCategories + clean
                                customCashInCategories = updatedList
                                newCategoryInput = ""
                                onSaveProfile(buildUpdatedProfile().copy(customCashInCategories = updatedList))
                                statusBanner = "'$clean' को Cash In कैटेगरी में जोड़ा गया"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CashInGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add to Cash In", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            val clean = newCategoryInput.trim()
                            if (clean.isNotEmpty() && clean !in customCashOutCategories) {
                                val updatedList = customCashOutCategories + clean
                                customCashOutCategories = updatedList
                                newCategoryInput = ""
                                onSaveProfile(buildUpdatedProfile().copy(customCashOutCategories = updatedList))
                                statusBanner = "'$clean' को Cash Out कैटेगरी में जोड़ा गया"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CashOutRed, contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add to Cash Out", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (customCashInCategories.isNotEmpty() || customCashOutCategories.isNotEmpty()) {
                    Text(
                        text = "Your Custom Categories (Tap × to remove):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customCashInCategories.forEach { cat ->
                            LedgerSelectableChip(
                                label = "+ $cat ×",
                                selected = true,
                                onClick = {
                                    val updatedList = customCashInCategories - cat
                                    customCashInCategories = updatedList
                                    onSaveProfile(buildUpdatedProfile().copy(customCashInCategories = updatedList))
                                }
                            )
                        }
                        customCashOutCategories.forEach { cat ->
                            LedgerSelectableChip(
                                label = "- $cat ×",
                                selected = true,
                                onClick = {
                                    val updatedList = customCashOutCategories - cat
                                    customCashOutCategories = updatedList
                                    onSaveProfile(buildUpdatedProfile().copy(customCashOutCategories = updatedList))
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. BUSINESS PROFILE, TAX (GSTIN/PAN) & PDF STATEMENT CUSTOMIZATION
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "5. BUSINESS PROFILE & PDF RECEIPT SETTINGS (फर्म और बिल सेटिंग्स)",
                icon = Icons.Default.Business
            ) {
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business / Firm Name (फर्म का नाम)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Proprietor / Owner Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile / Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Business Address / Office") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it.uppercase() },
                        label = { Text("PAN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = financialYear,
                        onValueChange = { financialYear = it },
                        label = { Text("FY") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "PDF STATEMENT & VOUCHER RECEIPT CUSTOMIZATION:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pdfSignatoryName,
                    onValueChange = { pdfSignatoryName = it },
                    label = { Text("Authorized Signatory Label (हस्ताक्षर नाम)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pdfFooterNote,
                    onValueChange = { pdfFooterNote = it },
                    label = { Text("PDF Statement Footer Note / Terms (बिल फुटर नोट)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Running Balance Column in PDF", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = pdfShowRunningBalance,
                        onCheckedChange = {
                            pdfShowRunningBalance = it
                            onSaveProfile(buildUpdatedProfile().copy(pdfShowRunningBalance = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Category & Payment Mode in PDF", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = pdfShowCategoryAndMode,
                        onCheckedChange = {
                            pdfShowCategoryAndMode = it
                            onSaveProfile(buildUpdatedProfile().copy(pdfShowCategoryAndMode = it))
                        }
                    )
                }

                Button(
                    onClick = {
                        val updated = buildUpdatedProfile()
                        onSaveProfile(updated)
                        statusBanner = "बिज़नेस प्रोफाइल और PDF सेटिंग्स सेव हो गईं!"
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_business_settings_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Business & PDF Settings (सेव करें)")
                }
            }
        }

        // =========================================================================
        // 6. DAILY REMINDER, WHATSAPP TEMPLATE & CLOUD BACKUP
        // =========================================================================
        item {
            SettingsSectionCard(
                title = "6. REMINDERS, CLOUD SYNC & BACKUP (रिमाइंडर और क्लाउड बैकअप)",
                icon = Icons.Default.NotificationsActive
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Evening Hisab Reminder (रोज़ाना हिसाब अलार्म)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Reminder Time: $dailyReminderTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = {
                            dailyReminderEnabled = it
                            onSaveProfile(buildUpdatedProfile().copy(dailyReminderEnabled = it))
                        }
                    )
                }

                if (dailyReminderEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("07:00 PM", "08:00 PM", "09:00 PM", "10:00 PM").forEach { timeSlot ->
                            LedgerSelectableChip(
                                label = timeSlot,
                                selected = dailyReminderTime == timeSlot,
                                onClick = {
                                    dailyReminderTime = timeSlot
                                    onSaveProfile(buildUpdatedProfile().copy(dailyReminderTime = timeSlot))
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = whatsappReminderTemplate,
                    onValueChange = { whatsappReminderTemplate = it },
                    label = { Text("WhatsApp Payment Reminder Template (तगादा मैसेज)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = CashInGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Cloud Firestore Sync Active: $userEmail",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CashInGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CashOutRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Statement", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, tint = CashInGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel CSV", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportBackupJson,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save JSON Backup", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onImportBackupJson,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Backup", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CashOutRed),
                    border = BorderStroke(1.dp, CashOutRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Google Cloud Account ($userEmail)")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Reusable Profile Avatar composable that renders either the user's custom picked photo
 * from internal storage or the default Shivay CashBook emblem logo.
 */
@Composable
fun ProfileAvatarImage(
    photoPath: String,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    val customBitmap = remember(photoPath) {
        if (photoPath.isNotBlank()) {
            try {
                val file = File(photoPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = "Operator Profile Photo",
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_shivay_logo_v2_1793873182923),
            contentDescription = "Shivay CashBook Logo",
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Full-screen Operator Security Lock Gate (Username + Password/PIN)
 * shown when App Lock is enabled in Settings.
 */
@Composable
fun OperatorAppLockScreen(
    profile: BusinessProfile,
    onUnlockSuccess: () -> Unit
) {
    var enteredUsername by remember { mutableStateOf(profile.operatorUsername) }
    var enteredPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatarImage(
                    photoPath = profile.profilePhotoPath,
                    sizeDp = 76
                )

                Text(
                    text = profile.businessName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "OPERATOR SECURITY LOCK (यूज़रनेम और पासवर्ड डालें)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )

                OutlinedTextField(
                    value = enteredUsername,
                    onValueChange = {
                        enteredUsername = it
                        errorMessage = null
                    },
                    label = { Text("Username (यूज़रनेम)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_screen_username_input")
                )

                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = {
                        enteredPassword = it
                        errorMessage = null
                    },
                    label = { Text("Password / PIN (पासवर्ड)") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_screen_password_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CashOutRed
                    )
                }

                Button(
                    onClick = {
                        val expectedUser = profile.operatorUsername.trim()
                        val expectedPass = profile.operatorPassword.trim()
                        val userMatches = expectedUser.isEmpty() || enteredUsername.trim().equals(expectedUser, ignoreCase = true)
                        val passMatches = expectedPass.isEmpty() || enteredPassword.trim() == expectedPass
                        if (userMatches && passMatches) {
                            onUnlockSuccess()
                        } else {
                            errorMessage = "गलत यूज़रनेम या पासवर्ड! (Invalid Username or Password/PIN)"
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unlock_app_button")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UNLOCK CASHBOOK (अनलॉक करें)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
