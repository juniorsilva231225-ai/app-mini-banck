package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BankAccount
import com.example.data.BankRepository
import com.example.data.Transaction
import com.example.data.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BankViewModel(private val repository: BankRepository) : ViewModel() {

    // Current logged-in user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // List of bank accounts owned by the current user
    val accounts: StateFlow<List<BankAccount>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getAccountsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected account ID
    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId.asStateFlow()

    init {
        // Auto-select first account when list isn't empty, reactively responding to user switches
        viewModelScope.launch {
            accounts.collect { list ->
                if (list.isNotEmpty()) {
                    if (_selectedAccountId.value == null || list.none { it.id == _selectedAccountId.value }) {
                        _selectedAccountId.value = list.first().id
                    }
                } else {
                    _selectedAccountId.value = null
                }
            }
        }
    }

    // Header/Active Account
    val selectedAccount: StateFlow<BankAccount?> = combine(accounts, _selectedAccountId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Transactions associated with reactive selection
    val selectedAccountTransactions: StateFlow<List<Transaction>> = _selectedAccountId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getTransactionsForAccount(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun selectAccount(id: Int) {
        _selectedAccountId.value = id
    }

    fun login(username: String, passwordPlain: String) {
        val u = username.trim().lowercase()
        val p = passwordPlain.trim()
        if (u.isEmpty() || p.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Se requiere usuario y contraseña."))
            }
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.getUserByUsername(u)
                if (user != null && user.passwordPlain == p) {
                    _currentUser.value = user
                    _uiEvent.emit(UiEvent.ShowSuccess("¡Bienvenido, ${user.fullName}!"))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Usuario o contraseña incorrectos."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al iniciar sesión: ${e.message}"))
            }
        }
    }

    fun register(username: String, passwordPlain: String, fullName: String) {
        val u = username.trim().lowercase()
        val p = passwordPlain.trim()
        val name = fullName.trim()

        if (u.isEmpty() || p.isEmpty() || name.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Todos los campos son obligatorios."))
            }
            return
        }

        viewModelScope.launch {
            try {
                val existing = repository.getUserByUsername(u)
                if (existing != null) {
                    _uiEvent.emit(UiEvent.ShowError("El nombre de usuario ya está registrado."))
                    return@launch
                }

                val newUser = User(
                    username = u,
                    passwordPlain = p,
                    fullName = name
                )
                val newUserId = repository.insertUser(newUser).toInt()
                
                // Auto seed one default account for the new user so they don't see an empty list
                val r1 = (1000..9999).random()
                val r2 = (2000..9999).random()
                val r3 = (3000..9999).random()
                val r4 = (4000..9999).random()
                val initAccountNumber = "$r1-$r2-$r3-$r4"
                
                val defaultAccount = BankAccount(
                    userId = newUserId,
                    name = "Mi Primera Cuenta",
                    accountNumber = initAccountNumber,
                    balance = 500.0,
                    currency = "USD",
                    type = "Ahorros",
                    colorHex = "#1E3C72"
                )
                val accId = repository.insertAccount(defaultAccount)
                
                repository.insertTransaction(
                    Transaction(
                        accountId = accId.toInt(),
                        type = "DEPOSITO",
                        amount = 500.0,
                        description = "Bono de bienvenida",
                        category = "Otros"
                    )
                )

                val loggedUser = newUser.copy(id = newUserId)
                _currentUser.value = loggedUser
                _uiEvent.emit(UiEvent.ShowSuccess("¡Usuario creado e inicio de sesión exitoso!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al registrar usuario: ${e.message}"))
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _selectedAccountId.value = null
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSuccess("Sesión cerrada correctamente."))
        }
    }

    fun makeDeposit(amount: Double, description: String, category: String) {
        if (amount <= 0.0) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("El monto debe ser mayor a cero."))
            }
            return
        }
        val currentAcc = selectedAccount.value
        if (currentAcc == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("No hay ninguna cuenta seleccionada."))
            }
            return
        }

        viewModelScope.launch {
            try {
                val updatedAccount = currentAcc.copy(balance = currentAcc.balance + amount)
                repository.updateAccount(updatedAccount)

                val transaction = Transaction(
                    accountId = currentAcc.id,
                    type = "DEPOSITO",
                    amount = amount,
                    description = description.ifBlank { "Depósito en efectivo" },
                    category = category.ifBlank { "Otros" }
                )
                repository.insertTransaction(transaction)
                _uiEvent.emit(UiEvent.ShowSuccess("Depósito realizado con éxito por $amount USD"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al procesar el depósito: ${e.message}"))
            }
        }
    }

    fun makeWithdrawal(amount: Double, description: String, category: String) {
        if (amount <= 0.0) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("El monto debe ser mayor a cero."))
            }
            return
        }
        val currentAcc = selectedAccount.value
        if (currentAcc == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("No hay ninguna cuenta seleccionada."))
            }
            return
        }
        if (currentAcc.balance < amount) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Fondos insuficientes. Saldo disponible: ${currentAcc.balance} USD"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val updatedAccount = currentAcc.copy(balance = currentAcc.balance - amount)
                repository.updateAccount(updatedAccount)

                val transaction = Transaction(
                    accountId = currentAcc.id,
                    type = "RETIRO",
                    amount = amount,
                    description = description.ifBlank { "Retiro de efectivo" },
                    category = "Otros"
                )
                repository.insertTransaction(transaction)
                _uiEvent.emit(UiEvent.ShowSuccess("Retiro realizado con éxito por $amount USD"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al procesar el retiro: ${e.message}"))
            }
        }
    }

    fun makeTransfer(targetAccountNumber: String, amount: Double, description: String) {
        val trimmedTarget = targetAccountNumber.trim()
        if (trimmedTarget.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Debe ingresar un número de cuenta destino."))
            }
            return
        }
        if (amount <= 0.0) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("El monto debe ser mayor a cero."))
            }
            return
        }
        val currentAcc = selectedAccount.value
        if (currentAcc == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("No hay ninguna cuenta seleccionada."))
            }
            return
        }
        if (currentAcc.accountNumber == trimmedTarget) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("No puede transferir dinero a la misma cuenta de origen."))
            }
            return
        }
        if (currentAcc.balance < amount) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Fondos insuficientes. Saldo disponible: ${currentAcc.balance} USD"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val destinationAccount = repository.getAccountByNumber(trimmedTarget)

                if (destinationAccount != null) {
                    val updatedSender = currentAcc.copy(balance = currentAcc.balance - amount)
                    val updatedReceiver = destinationAccount.copy(balance = destinationAccount.balance + amount)

                    repository.updateAccount(updatedSender)
                    repository.updateAccount(updatedReceiver)

                    // Sender Transaction
                    repository.insertTransaction(
                        Transaction(
                            accountId = currentAcc.id,
                            type = "TRANSFERENCIA",
                            amount = amount,
                            description = "Transferencia a: ${destinationAccount.name} (${destinationAccount.accountNumber}) - ${description.ifBlank { "Sin descripción" }}",
                            category = "Transferencia",
                            destinationAccount = trimmedTarget
                        )
                    )

                    // Receiver Transaction
                    repository.insertTransaction(
                        Transaction(
                            accountId = destinationAccount.id,
                            type = "DEPOSITO",
                            amount = amount,
                            description = "Transferencia de: ${currentAcc.name} (${currentAcc.accountNumber}) - ${description.ifBlank { "Sin descripción" }}",
                            category = "Transferencia"
                        )
                    )
                    _uiEvent.emit(UiEvent.ShowSuccess("Transferencia local de $amount USD realizada con éxito."))
                } else {
                    val updatedSender = currentAcc.copy(balance = currentAcc.balance - amount)
                    repository.updateAccount(updatedSender)

                    repository.insertTransaction(
                        Transaction(
                            accountId = currentAcc.id,
                            type = "TRANSFERENCIA",
                            amount = amount,
                            description = "Trf. externa a cuenta: $trimmedTarget - ${description.ifBlank { "Sin descripción" }}",
                            category = "Transferencia",
                            destinationAccount = trimmedTarget
                        )
                    )
                    _uiEvent.emit(UiEvent.ShowSuccess("Transferencia de $amount USD enviada con éxito a cuenta externa $trimmedTarget."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al realizar la transferencia: ${e.message}"))
            }
        }
    }

    fun createNewAccount(name: String, balance: Double, type: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("El nombre de la cuenta no puede estar vacío."))
            }
            return
        }
        if (balance < 0.0) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("El saldo inicial no puede ser negativo."))
            }
            return
        }

        viewModelScope.launch {
            try {
                val r1 = (1000..9999).random()
                val r2 = (2000..9999).random()
                val r3 = (3000..9999).random()
                val r4 = (4000..9999).random()
                val newAccountNumber = "$r1-$r2-$r3-$r4"

                val cardColors = listOf("#0061A4", "#3E5151", "#243B55", "#4B0082", "#2E8B57", "#1E3C72")
                val randomColor = cardColors.random()

                val newAccount = BankAccount(
                    userId = _currentUser.value?.id ?: 0,
                    name = trimmedName,
                    accountNumber = newAccountNumber,
                    balance = balance,
                    currency = "USD",
                    type = type.ifBlank { "Ahorros" },
                    colorHex = randomColor
                )

                val newId = repository.insertAccount(newAccount)

                if (balance > 0.0) {
                    repository.insertTransaction(
                        Transaction(
                            accountId = newId.toInt(),
                            type = "DEPOSITO",
                            amount = balance,
                            description = "Saldo de apertura",
                            category = "Otros"
                        )
                    )
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Cuenta '$trimmedName' creada con un saldo inicial de $balance USD."))
                _selectedAccountId.value = newId.toInt()
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al crear la cuenta: ${e.message}"))
            }
        }
    }

    sealed interface UiEvent {
        data class ShowSuccess(val message: String) : UiEvent
        data class ShowError(val message: String) : UiEvent
    }
}

class BankViewModelFactory(private val repository: BankRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BankViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BankViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
