package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeAppearanceMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import java.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CashBook
import com.example.data.model.CashTransaction
import com.example.data.model.Party
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.FinancialCalculationEngine
import com.example.util.StatementExportHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoucherEntrySheet(
    initialType: String,
    existingTransaction: CashTransaction? = null,
    currencySymbol: String,
    parties: List<Party>,
    preselectedParty: Party? = null,
    customCashInCategories: List<String> = emptyList(),
    customCashOutCategories: List<String> = emptyList(),
    defaultPaymentMode: String = "Cash",
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Double,
        remark: String,
        category: String,
        paymentMode: String,
        partyId: String,
        partyName: String,
        transactionDate: Long
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onPrintVoucherReceipt: ((CashTransaction) -> Unit)? = null,
    onReverseVoucher: ((CashTransaction) -> Unit)? = null,
    onQuickCreateParty: (name: String, phone: String, role: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var voucherType by remember {
        mutableStateOf(existingTransaction?.type ?: initialType)
    }
    var amountText by remember {
        mutableStateOf(
            existingTransaction?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var remarkText by remember {
        mutableStateOf(existingTransaction?.remark ?: "")
    }
    val availableCategories = if (voucherType == CashTransaction.TYPE_CASH_IN) {
        (customCashInCategories + CashTransaction.CATEGORIES_CASH_IN).distinct()
    } else {
        (customCashOutCategories + CashTransaction.CATEGORIES_CASH_OUT).distinct()
    }
    var selectedCategory by remember(voucherType) {
        mutableStateOf(
            existingTransaction?.category?.takeIf { it.isNotBlank() }
                ?: availableCategories.first()
        )
    }
    var selectedPaymentMode by remember {
        mutableStateOf(existingTransaction?.paymentMode ?: defaultPaymentMode.ifBlank { "Cash" })
    }
    var selectedPartyId by remember {
        mutableStateOf(existingTransaction?.partyId ?: preselectedParty?.id ?: "")
    }
    var selectedPartyName by remember {
        mutableStateOf(existingTransaction?.partyName ?: preselectedParty?.name ?: "")
    }
    var partySearchQuery by remember {
        mutableStateOf(existingTransaction?.partyName ?: preselectedParty?.name ?: "")
    }
    var quickPartyNamePrefill by remember { mutableStateOf("") }
    var selectedDateMillis by remember {
        mutableLongStateOf(
            existingTransaction?.transactionDate?.takeIf { it > 0L }
                ?: System.currentTimeMillis()
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showQuickPartyDialog by remember { mutableStateOf(false) }
    var showCalendarDatePicker by remember { mutableStateOf(false) }
    var showClockTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(parties, selectedPartyName) {
        if (selectedPartyId.isEmpty() && selectedPartyName.isNotBlank()) {
            val match = parties.firstOrNull { it.name.equals(selectedPartyName.trim(), ignoreCase = true) }
            if (match != null) {
                selectedPartyId = match.id
                selectedPartyName = match.name
            }
        }
    }

    val isCashIn = voucherType == CashTransaction.TYPE_CASH_IN
    val accentColor = if (isCashIn) CashInGreen else CashOutRed
    val liveAmountDouble = amountText.trim().toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Utilitarian Accounting Voucher Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (existingTransaction != null && existingTransaction.id.isNotBlank()) {
                            "VOUCHER #VCH-${existingTransaction.id.take(6).uppercase()}"
                        } else if (isCashIn) {
                            "CASH IN VOUCHER (RECEIPT / CR)"
                        } else {
                            "CASH OUT VOUCHER (PAYMENT / DR)"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = StatementExportHelper.formatShortDate(selectedDateMillis) +
                            " • " + StatementExportHelper.formatTime(selectedDateMillis),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.88f),
                            fontFamily = JetBrainsMonoFamily
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existingTransaction != null && existingTransaction.id.isNotBlank() && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(existingTransaction.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_voucher_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete voucher",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close voucher sheet",
                            tint = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Cash In / Cash Out Type Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isCashIn) CashInGreen else MaterialTheme.colorScheme.surface
                            )
                            .clickable { voucherType = CashTransaction.TYPE_CASH_IN }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ CASH IN (Cr)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCashIn) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (!isCashIn) CashOutRed else MaterialTheme.colorScheme.surface
                            )
                            .clickable { voucherType = CashTransaction.TYPE_CASH_OUT }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "- CASH OUT (Dr)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isCashIn) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Input (Monospaced)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountText = input
                            validationError = null
                        }
                    },
                    label = { Text("Voucher Amount ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    prefix = {
                        Text(
                            text = "$currencySymbol ",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = accentColor
                            )
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voucher_amount_input")
                )

                if (liveAmountDouble > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "In Words: ${FinancialCalculationEngine.amountToIndianWords(liveAmountDouble)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Adder Chips (+₹10, +₹20, +₹50, +₹100, +₹200, +₹500)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Add:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CashTransaction.QUICK_AMOUNT_STEPS.forEach { step ->
                        LedgerSelectableChip(
                            label = "+$currencySymbol$step",
                            selected = false,
                            onClick = {
                                val currentVal = amountText.trim().toDoubleOrNull() ?: 0.0
                                val updated = currentVal + step
                                amountText = if (updated % 1.0 == 0.0) updated.toLong().toString() else updated.toString()
                                validationError = null
                            }
                        )
                    }
                    if (amountText.isNotBlank()) {
                        LedgerSelectableChip(
                            label = "Clear",
                            selected = false,
                            onClick = { amountText = "" }
                        )
                    }
                }

                // 1-Tap Petty Cash / Chhote-Mote Kharche Templates (shown on Cash Out)
                if (!isCashIn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚡ छोटे-मोटे फुटकर खर्चे (1-TAP PETTY CASH - NO PARTY NEEDED):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = CashOutRed
                                )
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CashTransaction.PETTY_EXPENSE_TEMPLATES.forEach { (chipLabel, defaultNarration, defaultAmt) ->
                                    val isTemplateSelected = remarkText == defaultNarration
                                    LedgerSelectableChip(
                                        label = "$chipLabel ($currencySymbol$defaultAmt)",
                                        selected = isTemplateSelected,
                                        onClick = {
                                            remarkText = defaultNarration
                                            selectedCategory = "Petty Cash (छोटे खर्चे)"
                                            selectedPaymentMode = "Cash"
                                            selectedPartyId = ""
                                            selectedPartyName = ""
                                            partySearchQuery = ""
                                            if (amountText.isBlank()) {
                                                amountText = defaultAmt.toString()
                                            }
                                            validationError = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Particulars / Narration Input
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = {
                        remarkText = it
                        validationError = null
                    },
                    label = { Text("Particulars / Voucher Narration") },
                    placeholder = {
                        Text(
                            if (isCashIn) "e.g. Invoice #204 receipt, freight advance"
                            else "e.g. चाय-नाश्ता, HSD Diesel 200L, driver bhatta"
                        )
                    },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voucher_remark_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Enhanced Voucher Date & Time Selector (Calendar Picker + Clock Time Picker + Quick Chips)
                Text(
                    text = "VOUCHER DATE & TIME (तारीख और समय चुनें)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Interactive Calendar Date Button
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .clickable { showCalendarDatePicker = true }
                            .testTag("voucher_date_picker_button"),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DATE (तारीख)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = StatementExportHelper.formatShortDate(selectedDateMillis),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Interactive Clock Time Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showClockTimePicker = true }
                            .testTag("voucher_time_picker_button"),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TIME (समय)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = StatementExportHelper.formatTime(selectedDateMillis),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val now = remember { System.currentTimeMillis() }
                val dayMs = 86_400_000L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Today (आज)" to now,
                        "Yesterday (कल)" to (now - dayMs),
                        "2 Days Ago" to (now - 2 * dayMs)
                    ).forEach { (label, targetMillis) ->
                        val isSelectedDate = kotlin.math.abs(selectedDateMillis - targetMillis) < (dayMs / 2)
                        LedgerSelectableChip(
                            label = label,
                            selected = isSelectedDate,
                            onClick = { selectedDateMillis = targetMillis }
                        )
                    }
                    LedgerSelectableChip(
                        label = "◀ -1 Day",
                        selected = false,
                        onClick = { selectedDateMillis -= dayMs }
                    )
                    LedgerSelectableChip(
                        label = "+1 Day ▶",
                        selected = false,
                        onClick = { selectedDateMillis += dayMs }
                    )
                    LedgerSelectableChip(
                        label = "📅 Calendar",
                        selected = false,
                        onClick = { showCalendarDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Mode Selector
                Text(
                    text = "PAYMENT MODE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashTransaction.PAYMENT_MODES.forEach { mode ->
                        LedgerSelectableChip(
                            label = mode,
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Party (Customer / Supplier / Staff) Selector & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTY / KHATA ACCOUNT (OPTIONAL)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    TextButton(
                        onClick = {
                            quickPartyNamePrefill = partySearchQuery.trim()
                            showQuickPartyDialog = true
                        },
                        modifier = Modifier.testTag("quick_add_party_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Party", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // If party is selected or locked from Khata screen
                if (selectedPartyId.isNotBlank() || preselectedParty != null) {
                    val activeParty = parties.firstOrNull { it.id == selectedPartyId } ?: preselectedParty
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedPartyName.ifBlank { activeParty?.name.orEmpty() },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    val roleText = activeParty?.role ?: "Customer"
                                    val phoneText = activeParty?.phone?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                                    Text(
                                        text = "Khata Linked: $roleText$phoneText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (preselectedParty == null) {
                                IconButton(
                                    onClick = {
                                        selectedPartyId = ""
                                        selectedPartyName = ""
                                        partySearchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove party link",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Search & Free-Text Party Input
                    OutlinedTextField(
                        value = partySearchQuery,
                        onValueChange = { input ->
                            partySearchQuery = input
                            selectedPartyName = input
                            selectedPartyId = ""
                        },
                        label = { Text("Search Party Name or Leave Blank for General") },
                        placeholder = { Text("e.g. Ramesh, Rahul, Sharma Traders...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (partySearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        partySearchQuery = ""
                                        selectedPartyName = ""
                                        selectedPartyId = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voucher_party_search_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Matching Party Suggestions
                    val filteredParties = if (partySearchQuery.isBlank()) {
                        parties.take(4)
                    } else {
                        parties.filter {
                            it.name.contains(partySearchQuery, ignoreCase = true) ||
                                it.phone.contains(partySearchQuery) ||
                                it.role.contains(partySearchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredParties.isNotEmpty()) {
                        Text(
                            text = if (partySearchQuery.isBlank()) "Quick Select Party or General Entry:" else "Matching Parties (Tap to Select):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LedgerSelectableChip(
                                label = "No Party (General)",
                                selected = selectedPartyId.isEmpty() && partySearchQuery.isEmpty(),
                                onClick = {
                                    selectedPartyId = ""
                                    selectedPartyName = ""
                                    partySearchQuery = ""
                                }
                            )
                            filteredParties.forEach { party ->
                                LedgerSelectableChip(
                                    label = "${party.name} (${party.role.first()})",
                                    selected = selectedPartyId == party.id,
                                    onClick = {
                                        selectedPartyId = party.id
                                        selectedPartyName = party.name
                                        partySearchQuery = party.name
                                    }
                                )
                            }
                        }
                    }

                    // Prompt to Create New Party if typed name doesn't match an existing ledger
                    val hasExactMatch = parties.any { it.name.equals(partySearchQuery.trim(), ignoreCase = true) }
                    if (partySearchQuery.isNotBlank() && !hasExactMatch) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    quickPartyNamePrefill = partySearchQuery.trim()
                                    showQuickPartyDialog = true
                                },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "+ Create \"${partySearchQuery.trim()}\" as New Party in Khata",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                    Text(
                                        text = "Tap to add contact & role, or save directly as general entry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Head of Account (Category)
                Text(
                    text = "HEAD OF ACCOUNT (CATEGORY)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        LedgerSelectableChip(
                            label = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                if (!validationError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Existing Voucher Actions: Print Receipt Slip & Post Reversal Entry
                if (existingTransaction != null && existingTransaction.id.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onPrintVoucherReceipt != null) {
                            OutlinedButton(
                                onClick = {
                                    onPrintVoucherReceipt(existingTransaction)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("print_voucher_receipt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receipt PDF", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (onReverseVoucher != null) {
                            OutlinedButton(
                                onClick = {
                                    onReverseVoucher(existingTransaction)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("reverse_voucher_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post Reversal", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val parsedAmount = amountText.trim().toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            validationError = "Enter a valid voucher amount greater than 0."
                            return@Button
                        }
                        val finalRemark = remarkText.trim().ifEmpty { selectedCategory }
                        val resolvedParty = parties.firstOrNull {
                            it.id == selectedPartyId ||
                                (selectedPartyName.isNotBlank() && it.name.equals(selectedPartyName.trim(), ignoreCase = true))
                        }
                        val finalPartyId = resolvedParty?.id ?: selectedPartyId
                        val finalPartyName = resolvedParty?.name ?: selectedPartyName.trim()
                        onSave(
                            voucherType,
                            parsedAmount,
                            finalRemark,
                            selectedCategory,
                            selectedPaymentMode,
                            finalPartyId,
                            finalPartyName,
                            selectedDateMillis
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_voucher_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingTransaction != null && existingTransaction.id.isNotBlank()) {
                            "UPDATE VOUCHER"
                        } else {
                            "SAVE VOUCHER ENTRY"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showQuickPartyDialog) {
        PartyEditorDialog(
            existingParty = null,
            initialName = quickPartyNamePrefill,
            onDismiss = { showQuickPartyDialog = false },
            onSave = { name, phone, role, _ ->
                onQuickCreateParty(name, phone, role)
                selectedPartyName = name
                partySearchQuery = name
                showQuickPartyDialog = false
            }
        )
    }

    if (showCalendarDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showCalendarDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedDateUtc = datePickerState.selectedDateMillis
                        if (pickedDateUtc != null) {
                            val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            val hour = currentCal.get(Calendar.HOUR_OF_DAY)
                            val minute = currentCal.get(Calendar.MINUTE)
                            val second = currentCal.get(Calendar.SECOND)

                            val pickedCal = Calendar.getInstance().apply {
                                timeInMillis = pickedDateUtc
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, second)
                            }
                            selectedDateMillis = pickedCal.timeInMillis
                        }
                        showCalendarDatePicker = false
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showClockTimePicker) {
        val currentCal = remember(selectedDateMillis) {
            Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = currentCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showClockTimePicker = false },
            shape = RoundedCornerShape(6.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SELECT VOUCHER TIME (समय चुनें)", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LedgerSelectableChip(
                            label = "Now (अभी)",
                            selected = false,
                            onClick = {
                                val nowCal = Calendar.getInstance()
                                val updatedCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
                                }
                                selectedDateMillis = updatedCal.timeInMillis
                                showClockTimePicker = false
                            }
                        )
                        listOf(
                            "09:00 AM" to (9 to 0),
                            "01:30 PM" to (13 to 30),
                            "06:00 PM" to (18 to 0),
                            "09:00 PM" to (21 to 0)
                        ).forEach { (label, hrMin) ->
                            LedgerSelectableChip(
                                label = label,
                                selected = false,
                                onClick = {
                                    val updatedCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedDateMillis
                                        set(Calendar.HOUR_OF_DAY, hrMin.first)
                                        set(Calendar.MINUTE, hrMin.second)
                                        set(Calendar.SECOND, 0)
                                    }
                                    selectedDateMillis = updatedCal.timeInMillis
                                    showClockTimePicker = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        selectedDateMillis = updatedCal.timeInMillis
                        showClockTimePicker = false
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountTransferDialog(
    books: List<CashBook>,
    activeBookId: String?,
    onDismiss: () -> Unit,
    onCreateNewBankBook: () -> Unit,
    onConfirmTransfer: (
        fromBookId: String,
        toBookId: String,
        amount: Double,
        paymentMode: String,
        remark: String
    ) -> Unit
) {
    var fromId by remember {
        mutableStateOf(activeBookId ?: books.firstOrNull()?.id.orEmpty())
    }
    var toId by remember {
        mutableStateOf(books.firstOrNull { it.id != fromId }?.id ?: books.firstOrNull()?.id.orEmpty())
    }
    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Bank Transfer") }
    var remarkText by remember { mutableStateOf("Cash-to-Bank Contra Transfer") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONTRA ACCOUNT TRANSFER",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Transfer funds between Cash and Bank registers without inflating operating income or expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (books.size < 2) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "You currently have 1 register. Create a second register (e.g. SBI Bank A/c or Petty Cash) to post Contra transfers.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onCreateNewBankBook()
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Create Second Register")
                            }
                        }
                    }
                } else {
                    Text("SOURCE REGISTER (CASH OUT / DR):", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        books.forEach { b ->
                            LedgerSelectableChip(
                                label = b.name,
                                selected = fromId == b.id,
                                onClick = {
                                    fromId = b.id
                                    if (toId == b.id) {
                                        toId = books.firstOrNull { it.id != b.id }?.id.orEmpty()
                                    }
                                }
                            )
                        }
                    }

                    Text("DESTINATION REGISTER (CASH IN / CR):", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        books.forEach { b ->
                            LedgerSelectableChip(
                                label = b.name,
                                selected = toId == b.id,
                                onClick = { toId = b.id }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                amountText = input
                                errorText = null
                            }
                        },
                        label = { Text("Transfer Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contra_transfer_amount_input")
                    )

                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("Contra Narration") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!errorText.isNullOrBlank()) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (books.size >= 2) {
                Button(
                    onClick = {
                        val amt = amountText.trim().toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            errorText = "Enter a valid amount greater than 0."
                            return@Button
                        }
                        if (fromId == toId) {
                            errorText = "Source and Destination registers must be different."
                            return@Button
                        }
                        onConfirmTransfer(fromId, toId, amt, paymentMode, remarkText)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_contra_transfer_button")
                ) {
                    Text("Post Contra Entry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BusinessProfileDialog(
    currentProfile: BusinessProfile,
    onDismiss: () -> Unit,
    onSave: (BusinessProfile) -> Unit
) {
    var bizName by remember { mutableStateOf(currentProfile.businessName) }
    var ownerName by remember { mutableStateOf(currentProfile.ownerName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var gstin by remember { mutableStateOf(currentProfile.gstin) }
    var pan by remember { mutableStateOf(currentProfile.pan) }
    var financialYear by remember { mutableStateOf(currentProfile.financialYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                text = "BUSINESS PROFILE & TAX SETTINGS",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Configures headers for printed A4 Statements, Receipt Vouchers, and Backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = bizName,
                    onValueChange = { bizName = it },
                    label = { Text("Business / Firm Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Proprietor / Signatory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = financialYear,
                        onValueChange = { financialYear = it },
                        label = { Text("Financial Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Business Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Operating Address / Depot") },
                    singleLine = false,
                    maxLines = 2,
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bizName.isNotBlank()) {
                        onSave(
                            currentProfile.copy(
                                businessName = bizName.trim(),
                                ownerName = ownerName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                gstin = gstin.trim().uppercase(),
                                pan = pan.trim().uppercase(),
                                financialYear = financialYear.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onApplyRange: (startDateMillis: Long, endDateMillis: Long) -> Unit
) {
    val dayMs = 86_400_000L
    val now = remember { System.currentTimeMillis() }
    var selectedStart by remember { mutableLongStateOf(if (initialStartDate > 0L) initialStartDate else now - 30 * dayMs) }
    var selectedEnd by remember { mutableLongStateOf(if (initialEndDate > 0L) initialEndDate else now) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("FILTER BY DATE RANGE (तारीख चुनें)", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tap Start or End Date to open Calendar:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pickingStartDate = true }
                            .testTag("range_start_date_button"),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "FROM (शुरुआती तारीख)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = StatementExportHelper.formatShortDate(selectedStart),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pickingEndDate = true }
                            .testTag("range_end_date_button"),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TO (अंतिम तारीख)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = StatementExportHelper.formatShortDate(selectedEnd),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text("Quick Selection Presets:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LedgerSelectableChip(
                        label = "Today (आज)",
                        selected = false,
                        onClick = {
                            selectedStart = now
                            selectedEnd = now
                        }
                    )
                    LedgerSelectableChip(
                        label = "Yesterday (कल)",
                        selected = false,
                        onClick = {
                            selectedStart = now - dayMs
                            selectedEnd = now - dayMs
                        }
                    )
                    LedgerSelectableChip(
                        label = "Last 7 Days",
                        selected = false,
                        onClick = {
                            selectedStart = now - 7 * dayMs
                            selectedEnd = now
                        }
                    )
                    LedgerSelectableChip(
                        label = "Last 30 Days",
                        selected = false,
                        onClick = {
                            selectedStart = now - 30 * dayMs
                            selectedEnd = now
                        }
                    )
                    LedgerSelectableChip(
                        label = "Last 90 Days",
                        selected = false,
                        onClick = {
                            selectedStart = now - 90 * dayMs
                            selectedEnd = now
                        }
                    )
                    LedgerSelectableChip(
                        label = "FY 2026-27 (YTD)",
                        selected = false,
                        onClick = {
                            selectedStart = now - 180 * dayMs
                            selectedEnd = now
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startOrdered = minOf(selectedStart, selectedEnd)
                    val endOrdered = maxOf(selectedStart, selectedEnd)
                    onApplyRange(startOrdered, endOrdered)
                    onDismiss()
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Apply Range")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (pickingStartDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedStart)
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                Button(
                    onClick = {
                        state.selectedDateMillis?.let { selectedStart = it }
                        pickingStartDate = false
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Select Start Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (pickingEndDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedEnd)
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                Button(
                    onClick = {
                        state.selectedDateMillis?.let { selectedEnd = it }
                        pickingEndDate = false
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Select End Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun LedgerSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSwitcherSheet(
    books: List<CashBook>,
    activeBookId: String?,
    onSelectBook: (String) -> Unit,
    onCreateBookClick: () -> Unit,
    onEditBookClick: (CashBook) -> Unit,
    onDeleteBookClick: (CashBook) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CASH BOOKS & LEDGER REGISTERS",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Select an active register or create a separate book",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        onDismiss()
                        onCreateBookClick()
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("create_new_book_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Book")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            books.forEach { book ->
                val isSelected = book.id == activeBookId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else Color.Transparent
                        )
                        .clickable {
                            onSelectBook(book.id)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (book.description.isNotBlank()) {
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = book.currencySymbol,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            onDismiss()
                            onEditBookClick(book)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${book.name}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (books.size > 1) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onDeleteBookClick(book)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete ${book.name}",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BookEditorDialog(
    existingBook: CashBook? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, currencySymbol: String) -> Unit
) {
    var name by remember { mutableStateOf(existingBook?.name ?: "") }
    var description by remember { mutableStateOf(existingBook?.description ?: "") }
    var currency by remember { mutableStateOf(existingBook?.currencySymbol ?: "₹") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                text = if (existingBook == null) "CREATE NEW CASH BOOK" else "EDIT CASH BOOK",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cash Book Name") },
                    placeholder = { Text("e.g. Branch Office, SBI Current A/c, Petty Cash") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    placeholder = { Text("e.g. Daily cash & bank vouchers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Currency:", style = MaterialTheme.typography.labelLarge)
                    listOf("₹", "$", "€", "£").forEach { sym ->
                        LedgerSelectableChip(
                            label = sym,
                            selected = currency == sym,
                            onClick = { currency = sym }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), description.trim(), currency)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("save_book_button")
            ) {
                Text("Save Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PartyEditorDialog(
    existingParty: Party? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, role: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(existingParty?.name ?: initialName) }
    var phone by remember { mutableStateOf(existingParty?.phone ?: "") }
    var role by remember { mutableStateOf(existingParty?.role ?: "Customer") }
    var notes by remember { mutableStateOf(existingParty?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                text = if (existingParty == null) "ADD PARTY TO KHATA LEDGER" else "EDIT PARTY ACCOUNT",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party / Business Name") },
                    placeholder = { Text("e.g. Bharat Logistics, Highway Diesel") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / Contact Number") },
                    placeholder = { Text("e.g. 9820114455") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Account Classification:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CashTransaction.PARTY_ROLES.forEach { r ->
                        LedgerSelectableChip(
                            label = r,
                            selected = role == r,
                            onClick = { role = r }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Account Notes / GST / Route") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), phone.trim(), role, notes.trim())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("save_party_button")
            ) {
                Text("Save Party")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatementExportDialog(
    summary: BookSummary,
    entryCount: Int,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onExportBackupJson: () -> Unit,
    onImportBackupJson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPORT, PRINT & BACKUP",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Register: ${summary.bookName} ($entryCount Vouchers)\n" +
                        "Closing Balance: ${StatementExportHelper.formatCurrency(summary.netBalance, summary.currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = {
                        onExportPdf()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = CashOutRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Share A4 Statement (PDF)")
                }

                OutlinedButton(
                    onClick = {
                        onPrintPdf()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("print_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Direct Print A4 Statement")
                }

                Button(
                    onClick = {
                        onExportCsv()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CashInGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableView,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Microsoft Excel Register (CSV)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExportBackupJson()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_backup_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Backup", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            onImportBackupJson()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("import_backup_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore JSON", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppThemeMode,
    isDarkTheme: Boolean,
    appearanceMode: ThemeAppearanceMode = if (isDarkTheme) ThemeAppearanceMode.DARK else ThemeAppearanceMode.LIGHT,
    useDynamicColor: Boolean = (currentTheme == AppThemeMode.DYNAMIC_WALLPAPER),
    onDismiss: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectAppearanceMode: (ThemeAppearanceMode) -> Unit = { mode ->
        onToggleDarkTheme(mode == ThemeAppearanceMode.DARK)
    },
    onToggleDynamicColor: (Boolean) -> Unit = { enabled ->
        if (enabled) onSelectTheme(AppThemeMode.DYNAMIC_WALLPAPER)
        else onSelectTheme(AppThemeMode.CLASSIC_NAVY)
    }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text(
                text = "CENTRALIZED THEME & DISPLAY SETTINGS",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DISPLAY MODE (LIGHT • DARK • SYSTEM):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3-Way Segmented Selector: Light | Dark | System
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
                                .testTag("appearance_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(4.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
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
                                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                                        fontSize = 10.sp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                )
                            }
                        }
                    }
                }

                // Dedicated Dark Mode Toggle Card
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                                text = "Dark Mode Toggle (डार्क मोड)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Active: ${appearanceMode.displayName} (${if (isDarkTheme) "Dark Surface" else "Light Surface"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }

                // Dedicated Material 3 Dynamic Color Toggle Card
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                                text = "Material 3 Dynamic Color",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Adapts palette to Android 12+ system wallpaper (DynamicColorScheme)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColor || currentTheme == AppThemeMode.DYNAMIC_WALLPAPER,
                            onCheckedChange = onToggleDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "COLOR THEMES & MONOCHROME B&W MODE:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppThemeMode.entries.forEach { mode ->
                    val isSelected = currentTheme == mode
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(mode) }
                            .testTag("theme_option_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${mode.displayName} (${mode.hindiName})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(4.dp)) {
                Text("Done")
            }
        }
    )
}


