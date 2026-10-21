package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.LinkedWallet
import com.example.data.model.NotificationItem
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    HOME, VIP, WALLET, ACCOUNT
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = WalletRepository(database)

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTransactions: StateFlow<List<Transaction>> = _currentUserId.flatMapLatest { id ->
        if (id != null) repository.getRecentUserTransactions(id, 6) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUserTransactions: StateFlow<List<Transaction>> = _currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserTransactions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedWallets: StateFlow<List<LinkedWallet>> = _currentUserId.flatMapLatest { id ->
        if (id != null) repository.getLinkedWallets(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationItem>> = _currentUserId.flatMapLatest { id ->
        if (id != null) repository.getNotifications(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val depositNumber: StateFlow<String> = repository.getDepositNumberFlow()
        .map { it ?: "01960067933" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "01960067933")

    // Admin flows
    val pendingDeposits: StateFlow<List<Transaction>> = repository.getAllPendingDeposits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingWithdrawals: StateFlow<List<Transaction>> = repository.getAllPendingWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun register(
        phone: String,
        fullName: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.register(phone, fullName, pass)
            result.fold(
                onSuccess = { user ->
                    _currentUserId.value = user.id
                    _isAdminMode.value = user.isAdmin
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে")
                }
            )
        }
    }

    fun login(
        phone: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.login(phone, pass)
            result.fold(
                onSuccess = { user ->
                    _currentUserId.value = user.id
                    _isAdminMode.value = user.isAdmin
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "লগইন ব্যর্থ হয়েছে")
                }
            )
        }
    }

    fun logout() {
        _currentUserId.value = null
        _isAdminMode.value = false
        _selectedTab.value = MainTab.HOME
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    fun submitDeposit(
        method: String,
        amount: Double,
        senderNumber: String,
        trxId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser.value ?: return onError("প্রথমে লগ ইন করুন")
        viewModelScope.launch {
            val res = repository.submitDeposit(
                userId = user.id,
                userPhone = user.phoneNumber,
                method = method,
                amount = amount,
                senderNumber = senderNumber,
                trxId = trxId
            )
            res.fold(
                onSuccess = {
                    _actionMessage.value = "ডিপোজিট রিকোয়েস্ট সফলভাবে জমা হয়েছে!"
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "ডিপোজিট সাবমিট ব্যর্থ হয়েছে")
                }
            )
        }
    }

    fun submitWithdrawal(
        method: String,
        amount: Double,
        receiverNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser.value ?: return onError("প্রথমে লগ ইন করুন")
        viewModelScope.launch {
            val res = repository.submitWithdrawal(
                userId = user.id,
                userPhone = user.phoneNumber,
                method = method,
                amount = amount,
                receiverNumber = receiverNumber
            )
            res.fold(
                onSuccess = {
                    _actionMessage.value = "উত্তোলন রিকোয়েস্ট সফলভাবে গৃহীত হয়েছে!"
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "উত্তোলন সাবমিট ব্যর্থ হয়েছে")
                }
            )
        }
    }

    fun addLinkedWallet(method: String, number: String, type: String = "Personal", onDone: () -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.addLinkedWallet(user.id, method, number, type)
            onDone()
        }
    }

    fun deleteLinkedWallet(id: Long) {
        viewModelScope.launch {
            repository.deleteLinkedWallet(id)
        }
    }

    fun markNotificationsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markNotificationsRead(user.id)
        }
    }

    // Admin commands
    fun approveDeposit(txId: Long, note: String = "") {
        viewModelScope.launch {
            repository.approveDeposit(txId, note)
            _actionMessage.value = "ডিপোজিট অনুমোদন করা হয়েছে এবং ইউজারের একাউন্টে ব্যালেন্স যোগ হয়েছে!"
        }
    }

    fun rejectDeposit(txId: Long, reason: String) {
        viewModelScope.launch {
            repository.rejectDeposit(txId, reason)
            _actionMessage.value = "ডিপোজিট বাতিল করা হয়েছে।"
        }
    }

    fun approveWithdrawal(txId: Long, adminTrxId: String = "") {
        viewModelScope.launch {
            repository.approveWithdrawal(txId, adminTrxId)
            _actionMessage.value = "উত্তোলন রিকোয়েস্ট অনুমোদন করা হয়েছে!"
        }
    }

    fun rejectWithdrawal(txId: Long, reason: String) {
        viewModelScope.launch {
            repository.rejectWithdrawal(txId, reason)
            _actionMessage.value = "উত্তোলন বাতিল ও ব্যালেন্স রিফান্ড করা হয়েছে।"
        }
    }

    fun updateDepositNumber(newNumber: String) {
        viewModelScope.launch {
            repository.updateDepositNumber(newNumber)
            _actionMessage.value = "ডিপোজিট নম্বর আপডেট হয়েছে: $newNumber"
        }
    }

    fun adjustUserBalance(userId: Long, amount: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustUserBalance(userId, amount, reason)
            _actionMessage.value = "ইউজার ব্যালেন্স সমন্বয় করা হয়েছে।"
        }
    }
}
