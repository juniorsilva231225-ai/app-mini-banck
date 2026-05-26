package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BankAccount
import com.example.data.Transaction
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Safe helper to convert HEX string to Compose Color
fun parseHexColor(hex: String, default: Color = Color(0xFF1E3C72)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDashboard(
    viewModel: BankViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Collect states reactively
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.selectedAccountTransactions.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    // Dialog trigger states
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }

    // Active transaction list filter
    var activeFilter by remember { mutableStateOf("Todo") } // "Todo", "Depósitos", "Retiros", "Transferencias"

    // Receive ViewModel toast events
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is BankViewModel.UiEvent.ShowSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is BankViewModel.UiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (currentUser == null) {
        LoginScreen(
            onLogin = { u, p -> viewModel.login(u, p) },
            onRegister = { u, p, name -> viewModel.register(u, p, name) },
            modifier = modifier
        )
    } else {
        // Main layout
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        val activeUser = currentUser
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val initials = remember(activeUser) {
                                val name = activeUser?.fullName ?: ""
                                val parts = name.split(" ").filter { it.isNotBlank() }
                                if (parts.size >= 2) {
                                    "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
                                } else if (parts.isNotEmpty()) {
                                    "${parts[0].firstOrNull() ?: ""}"
                                } else {
                                    "U"
                                }.uppercase()
                            }
                            // Professional Polish dynamic Initials Avatar Circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text(
                                    text = "Hola de nuevo,",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = activeUser?.fullName ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewAccountDialog = true },
                            modifier = Modifier
                                .testTag("create_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Crear nueva cuenta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Cerrar sesión",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            modifier = modifier.testTag("banking_dashboard_root")
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Section 1: Swipable Accounts Carousel
            item {
                Column {
                    Text(
                        text = "Mis Cuentas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (accounts.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(accounts, key = { it.id }) { account ->
                                CreditCardItem(
                                    account = account,
                                    isSelected = account.id == selectedAccountId,
                                    onSelect = { viewModel.selectAccount(account.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Quick Actions Panel
            item {
                QuickActionsPanel(
                    onDeposit = { showDepositDialog = true },
                    onWithdraw = { showWithdrawDialog = true },
                    onTransfer = { showTransferDialog = true },
                    selectedAccountExists = selectedAccount != null
                )
            }

            // Section 3: History & Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historial de Actividad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Action count badge
                        Text(
                            text = "${transactions.size} movimientos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters Scroll
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("Todo", "Depósitos", "Retiros", "Transferencias")
                        items(filters) { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter) },
                                modifier = Modifier.testTag("filter_${filter.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Section 4: Transactions Feed
            val filteredTransactions = when (activeFilter) {
                "Depósitos" -> transactions.filter { it.type == "DEPOSITO" }
                "Retiros" -> transactions.filter { it.type == "RETIRO" }
                "Transferencias" -> transactions.filter { it.type == "TRANSFERENCIA" }
                else -> transactions
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Sin transacciones",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay movimientos registrados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Realice un depósito o transferencia para ver su historial aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(transaction = tx)
                }
            }
        }
    }

    // Modal Dialogs setup
    if (showDepositDialog && selectedAccount != null) {
        DepositDialog(
            accountName = selectedAccount!!.name,
            onDismiss = { showDepositDialog = false },
            onSubmit = { amount, description, category ->
                viewModel.makeDeposit(amount, description, category)
                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog && selectedAccount != null) {
        WithdrawDialog(
            accountName = selectedAccount!!.name,
            availableBalance = selectedAccount!!.balance,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { amount, description, category ->
                viewModel.makeWithdrawal(amount, description, category)
                showWithdrawDialog = false
            }
        )
    }

    if (showTransferDialog && selectedAccount != null) {
        TransferDialog(
            accountName = selectedAccount!!.name,
            availableBalance = selectedAccount!!.balance,
            accounts = accounts,
            currentAccountNumber = selectedAccount!!.accountNumber,
            onDismiss = { showTransferDialog = false },
            onSubmit = { targetAccount, amount, description ->
                viewModel.makeTransfer(targetAccount, amount, description)
                showTransferDialog = false
            }
        )
    }

    if (showNewAccountDialog) {
        NewAccountDialog(
            onDismiss = { showNewAccountDialog = false },
            onSubmit = { name, initialBalance, type ->
                viewModel.createNewAccount(name, initialBalance, type)
                showNewAccountDialog = false
            }
        )
    }
    }
}

// -------------------------------------------------------------
// UI COMPONENTS DEFINITION
// -------------------------------------------------------------

@Composable
fun CreditCardItem(
    account: BankAccount,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val gradientColors = if (isSelected) {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            Color(0xFF00223E) // Premium deep shadow blue
        )
    } else {
        listOf(
            parseHexColor(account.colorHex),
            parseHexColor(account.colorHex).copy(alpha = 0.7f),
            Color(0xFF1C1B1F)
        )
    }

    val format = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val formattedBalance = format.format(account.balance)

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(170.dp)
            .clickable { onSelect() }
            .testTag("credit_card_${account.id}"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 10.dp else 2.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder(true).copy(
            brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
        ) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            // Chip icon illustration
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Account Name, Balance Title & Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = account.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cuenta de ${account.type}",
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        ) {}
                    }
                }

                // Balance display
                Column {
                    Text(
                        text = "Saldo Disponible",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "$formattedBalance ${account.currency}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                // Bottom Row: Number masked
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = account.accountNumber,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Banca local",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsPanel(
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onTransfer: () -> Unit,
    selectedAccountExists: Boolean
) {
    Column {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Default.ArrowUpward,
                label = "Depositar",
                tint = Color(0xFF2E7D32),
                enabled = selectedAccountExists,
                onClick = onDeposit,
                modifier = Modifier.testTag("action_deposit")
            )

            QuickActionButton(
                icon = Icons.Default.ArrowDownward,
                label = "Retirar",
                tint = Color(0xFFC62828),
                enabled = selectedAccountExists,
                onClick = onWithdraw,
                modifier = Modifier.testTag("action_withdraw")
            )

            QuickActionButton(
                icon = Icons.Default.SwapHoriz,
                label = "Transferir",
                tint = Color(0xFF1565C0),
                enabled = selectedAccountExists,
                onClick = onTransfer,
                modifier = Modifier.testTag("action_transfer")
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp)
            .width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransactionRowItem(transaction: Transaction) {
    val isDeposit = transaction.type == "DEPOSITO"
    val icon = when (transaction.category) {
        "Alimentos" -> Icons.Default.ArrowDownward
        "Servicios" -> Icons.Default.ArrowDownward
        "Nómina" -> Icons.Default.ArrowUpward
        "Transporte" -> Icons.Default.ArrowDownward
        "Inversión" -> Icons.Default.ArrowUpward
        "Transferencia" -> Icons.Default.SwapHoriz
        else -> if (isDeposit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    }

    val iconContainerColor = when (transaction.category) {
        "Nómina" -> Color(0xFFE8F5E9)
        "Alimentos" -> Color(0xFFFFEBEE)
        "Servicios" -> Color(0xFFECEFF1)
        "Transferencia" -> MaterialTheme.colorScheme.primaryContainer
        else -> if (isDeposit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    }

    val iconColor = when (transaction.category) {
        "Nómina" -> Color(0xFF116B31)
        "Alimentos" -> Color(0xFFBA1A1A)
        "Servicios" -> Color(0xFF44474E)
        "Transferencia" -> MaterialTheme.colorScheme.primary
        else -> if (isDeposit) Color(0xFF116B31) else Color(0xFFBA1A1A)
    }

    val format = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val formattedAmount = format.format(transaction.amount)
    
    val dateString = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "ES"))
        sdf.format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category/Type Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•  $dateString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Value badge
            Text(
                text = if (isDeposit) "+$formattedAmount" else "-$formattedAmount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDeposit) Color(0xFF116B31) else Color(0xFFBA1A1A)
            )
        }
    }
}

// -------------------------------------------------------------
// TRANSACTION & DIALOG DEFINITIONS
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositDialog(
    accountName: String,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Otros") }

    val categories = listOf("Otros", "Nómina", "Inversión", "Reembolso")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Realizar Depósito",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Abonando fondos a la cuenta: $accountName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                    singleLine = true
                )

                // High intensity presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("20", "50", "100", "500").forEach { preset ->
                        OutlinedButton(
                            onClick = { amount = preset },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("+$preset", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().testTag("deposit_description_input"),
                    singleLine = true
                )

                Column {
                    Text("Categoría:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                            onSubmit(parsedAmount, description, category)
                        },
                        modifier = Modifier.testTag("deposit_submit_button")
                    ) {
                        Text("Depositar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawDialog(
    accountName: String,
    availableBalance: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentos") }

    val categories = listOf("Alimentos", "Servicios", "Transporte", "Otros")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Retirar Efectivo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Retirará recursos de: $accountName\nSaldo disponible: $availableBalance USD",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input"),
                    singleLine = true
                )

                // High intensity presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("10", "20", "50", "100").forEach { preset ->
                        OutlinedButton(
                            onClick = { amount = preset },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("$preset icon_usd", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_description_input"),
                    singleLine = true
                )

                Column {
                    Text("Categoría:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                            onSubmit(parsedAmount, description, category)
                        },
                        modifier = Modifier.testTag("withdraw_submit_button")
                    ) {
                        Text("Retirar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accountName: String,
    availableBalance: Double,
    accounts: List<BankAccount>,
    currentAccountNumber: String,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var destinationAccountNum by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nueva Transferencia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Origen: $accountName\nSaldo disponible: $availableBalance USD",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = destinationAccountNum,
                    onValueChange = { destinationAccountNum = it },
                    label = { Text("Cuenta Destinataria") },
                    modifier = Modifier.fillMaxWidth().testTag("transfer_target_input"),
                    singleLine = true,
                    placeholder = { Text("Ej: 1234-5678-...") }
                )

                // Quick select from other local accounts
                val otherAccounts = accounts.filter { it.accountNumber != currentAccountNumber }
                if (otherAccounts.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Tus otras cuentas:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            otherAccounts.forEach { otherAcc ->
                                SuggestionChip(
                                    onClick = { destinationAccountNum = otherAcc.accountNumber },
                                    label = { Text(otherAcc.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto a enviar (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nota / Descripción") },
                    modifier = Modifier.fillMaxWidth().testTag("transfer_description_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                            onSubmit(destinationAccountNum, parsedAmount, description)
                        },
                        modifier = Modifier.testTag("transfer_submit_button")
                    ) {
                        Text("Transferir")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Ahorros") } // "Ahorros", "Corriente"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Apertura de Cuenta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Nombre de la cuenta") },
                    modifier = Modifier.fillMaxWidth().testTag("new_account_name_input"),
                    singleLine = true,
                    placeholder = { Text("Ej: Ahorro Vacaciones") }
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Saldo de apertura (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("new_account_balance_input"),
                    singleLine = true,
                    placeholder = { Text("0.0") }
                )

                Column {
                    Text("Tipo de cuenta:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { accountType = "Ahorros" }
                        ) {
                            RadioButton(selected = accountType == "Ahorros", onClick = { accountType = "Ahorros" })
                            Text("Ahorros", modifier = Modifier.padding(start = 4.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { accountType = "Corriente" }
                        ) {
                            RadioButton(selected = accountType == "Corriente", onClick = { accountType = "Corriente" })
                            Text("Corriente", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsedBalance = initialBalance.toDoubleOrNull() ?: 0.0
                            onSubmit(accountName, parsedBalance, accountType)
                        },
                        modifier = Modifier.testTag("new_account_submit_button")
                    ) {
                        Text("Abrir Cuenta")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoginTab by remember { mutableStateOf(true) }
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Elegant professional logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Banco Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mi Banca Móvil",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Gestión financiera inteligente y segura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Material 3 custom segment switch/tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isLoginTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isLoginTab = true }
                        .testTag("tab_login"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLoginTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLoginTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!isLoginTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isLoginTab = false }
                        .testTag("tab_register"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registrarse",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isLoginTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isLoginTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input fields card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nombre Completo") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("register_fullname_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (isLoginTab) {
                                onLogin(username, password)
                            } else {
                                onRegister(username, password, fullName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoginTab) "Entrar" else "Crear Cuenta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Test accounts section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Acceso rápido de prueba (Autocompletar)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Javier Chip
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                username = "javier"
                                password = "1234"
                                onLogin("javier", "1234")
                            }
                            .testTag("demo_user_javier"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("JP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Javier Pérez", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Clave: 1234", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Maria Chip
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                username = "maria"
                                password = "1234"
                                onLogin("maria", "1234")
                            }
                            .testTag("demo_user_maria"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("MR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("María Rodríguez", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Clave: 1234", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
