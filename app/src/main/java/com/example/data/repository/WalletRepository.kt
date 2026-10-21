package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.LinkedWallet
import com.example.data.model.NotificationItem
import com.example.data.model.SystemConfig
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class WalletRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val txDao = db.transactionDao()
    private val walletDao = db.linkedWalletDao()
    private val notifDao = db.notificationDao()
    private val configDao = db.systemConfigDao()

    suspend fun register(phone: String, fullName: String, pass: String): Result<User> {
        val cleanPhone = phone.trim()
        val cleanPass = pass.trim()
        val cleanName = fullName.trim()

        if (cleanPhone.length < 10) {
            return Result.failure(IllegalArgumentException("সঠিক মোবাইল নম্বর প্রদান করুন"))
        }
        if (cleanPass.length < 4) {
            return Result.failure(IllegalArgumentException("পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে"))
        }

        val existing = userDao.getUserByPhone(cleanPhone)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("এই নম্বরে ইতিমধ্যে অ্যাকাউন্ট রয়েছে! লগ ইন করুন।"))
        }

        val newUser = User(
            phoneNumber = cleanPhone,
            fullName = if (cleanName.isNotEmpty()) cleanName else "User $cleanPhone",
            password = cleanPass,
            balance = 0.0,
            totalDeposit = 0.0,
            totalWithdraw = 0.0,
            todayCommission = 0.0,
            totalCommission = 0.0,
            isAdmin = (cleanPhone == "01960067933" || cleanPhone == "admin")
        )

        val id = userDao.insertUser(newUser)
        val userWithId = newUser.copy(id = id)

        notifDao.insertNotification(
            NotificationItem(
                userId = id,
                title = "অ্যাকাউন্ট সফলভাবে প্রস্তুত!",
                message = "E-WALLET এ স্বাগতম। ০১৯৬০০৬৭৯৩৩ নম্বরে টাকা পাঠিয়ে ট্রানজেকশন আইডি দিয়ে ব্যালেন্স যুক্ত করুন।",
                type = "INFO"
            )
        )

        return Result.success(userWithId)
    }

    suspend fun login(phone: String, pass: String): Result<User> {
        val cleanPhone = phone.trim()
        val cleanPass = pass.trim()

        val user = userDao.login(cleanPhone, cleanPass)
        return if (user != null) {
            if (user.isBlocked) {
                Result.failure(IllegalStateException("আপনার অ্যাকাউন্ট ব্লক করা রয়েছে। এডমিনের সাথে যোগাযোগ করুন।"))
            } else {
                Result.success(user)
            }
        } else {
            Result.failure(IllegalArgumentException("মোবাইল নম্বর বা পাসওয়ার্ড ভুল হয়েছে!"))
        }
    }

    fun getUserFlow(userId: Long): Flow<User?> = userDao.getUserById(userId)

    suspend fun getUserSync(userId: Long): User? = userDao.getUserByIdSync(userId)

    fun getUserTransactions(userId: Long): Flow<List<Transaction>> = txDao.getTransactionsForUser(userId)

    fun getRecentUserTransactions(userId: Long, limit: Int = 5): Flow<List<Transaction>> =
        txDao.getRecentTransactionsForUser(userId, limit)

    fun getLinkedWallets(userId: Long): Flow<List<LinkedWallet>> = walletDao.getWalletsForUser(userId)

    suspend fun addLinkedWallet(userId: Long, method: String, number: String, type: String = "Personal"): Long {
        return walletDao.insertWallet(
            LinkedWallet(
                userId = userId,
                method = method,
                accountNumber = number.trim(),
                accountType = type
            )
        )
    }

    suspend fun deleteLinkedWallet(walletId: Long) {
        walletDao.deleteWallet(walletId)
    }

    fun getNotifications(userId: Long): Flow<List<NotificationItem>> = notifDao.getNotificationsForUser(userId)

    suspend fun markNotificationsRead(userId: Long) {
        notifDao.markAllRead(userId)
    }

    suspend fun getDepositNumber(): String {
        return configDao.getConfigValue("deposit_number") ?: "01960067933"
    }

    fun getDepositNumberFlow(): Flow<String?> = configDao.getConfigFlow("deposit_number")

    suspend fun updateDepositNumber(number: String) {
        configDao.setConfig(SystemConfig("deposit_number", number.trim()))
    }

    suspend fun submitDeposit(
        userId: Long,
        userPhone: String,
        method: String,
        amount: Double,
        senderNumber: String,
        trxId: String
    ): Result<Long> {
        if (amount < 50) {
            return Result.failure(IllegalArgumentException("সর্বনিম্ন ডিপোজিট ৫০ টাকা"))
        }
        if (senderNumber.trim().length < 10) {
            return Result.failure(IllegalArgumentException("সঠিক প্রেরক (Sender) নম্বর প্রদান করুন"))
        }
        if (trxId.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("ট্রানজেকশন আইডি (TrxID) আবশ্যক"))
        }

        val tx = Transaction(
            userId = userId,
            userPhone = userPhone,
            type = "DEPOSIT",
            method = method,
            amount = amount,
            senderNumber = senderNumber.trim(),
            receiverNumber = getDepositNumber(),
            trxId = trxId.trim().uppercase(),
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        val txId = txDao.insertTransaction(tx)

        notifDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "ডিপোজিট রিকোয়েস্ট গৃহীত হয়েছে",
                message = "৳${"%.2f".format(amount)} (${method}) এর রিকোয়েস্ট পেন্ডিং রয়েছে। এডমিন শীঘ্রই অনুমোদন করবেন।",
                type = "INFO"
            )
        )

        return Result.success(txId)
    }

    suspend fun submitWithdrawal(
        userId: Long,
        userPhone: String,
        method: String,
        amount: Double,
        receiverNumber: String
    ): Result<Long> {
        val user = userDao.getUserByIdSync(userId) ?: return Result.failure(IllegalArgumentException("ব্যবহারকারী পাওয়া যায়নি"))

        if (amount < 100) {
            return Result.failure(IllegalArgumentException("সর্বনিম্ন উত্তোলন ১০০ টাকা"))
        }
        if (user.balance < amount) {
            return Result.failure(IllegalArgumentException("পর্যাপ্ত ব্যালেন্স নেই! আপনার বর্তমান ব্যালেন্স ৳${"%.2f".format(user.balance)}"))
        }
        if (receiverNumber.trim().length < 10) {
            return Result.failure(IllegalArgumentException("সঠিক ওয়ালেট নম্বর প্রদান করুন"))
        }

        // Hold amount immediately from user's balance
        userDao.holdWithdrawal(userId, amount)

        val tx = Transaction(
            userId = userId,
            userPhone = userPhone,
            type = "WITHDRAW",
            method = method,
            amount = amount,
            receiverNumber = receiverNumber.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        val txId = txDao.insertTransaction(tx)

        notifDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "উত্তোলন রিকোয়েস্ট পাঠানো হয়েছে",
                message = "৳${"%.2f".format(amount)} (${method}) উত্তোলনের জন্য প্রসেসিংয়ে আছে।",
                type = "INFO"
            )
        )

        return Result.success(txId)
    }

    // Admin Operations
    fun getAllPendingDeposits(): Flow<List<Transaction>> = txDao.getPendingDeposits()

    fun getAllPendingWithdrawals(): Flow<List<Transaction>> = txDao.getPendingWithdrawals()

    fun getAllTransactions(): Flow<List<Transaction>> = txDao.getAllTransactions()

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun approveDeposit(txId: Long, adminNote: String = ""): Result<Unit> {
        val tx = txDao.getTransactionById(txId) ?: return Result.failure(IllegalArgumentException("ট্রানজেকশন পাওয়া যায়নি"))
        if (tx.status != "PENDING") {
            return Result.failure(IllegalStateException("ট্রানজেকশন ইতিমধ্যে প্রসেস করা হয়েছে"))
        }

        val updated = tx.copy(
            status = "APPROVED",
            adminNote = adminNote,
            approvedAt = System.currentTimeMillis()
        )
        txDao.updateTransaction(updated)

        // Credit to user balance and totalDeposit
        userDao.creditDeposit(tx.userId, tx.amount)

        notifDao.insertNotification(
            NotificationItem(
                userId = tx.userId,
                title = "ডিপোজিট সফল হয়েছে! 🎉",
                message = "আপনার ৳${"%.2f".format(tx.amount)} (${tx.method}) ডিপোজিট অনুমোদন করা হয়েছে। বর্তমান ব্যালেন্সে যোগ করা হয়েছে।",
                type = "DEPOSIT_APPROVED"
            )
        )

        return Result.success(Unit)
    }

    suspend fun rejectDeposit(txId: Long, reason: String = "তথ্য যাচাইয়ে ভুল পাওয়া গেছে"): Result<Unit> {
        val tx = txDao.getTransactionById(txId) ?: return Result.failure(IllegalArgumentException("ট্রানজেকশন পাওয়া যায়নি"))
        if (tx.status != "PENDING") {
            return Result.failure(IllegalStateException("ট্রানজেকশন ইতিমধ্যে প্রসেস করা হয়েছে"))
        }

        val updated = tx.copy(
            status = "REJECTED",
            adminNote = reason,
            approvedAt = System.currentTimeMillis()
        )
        txDao.updateTransaction(updated)

        notifDao.insertNotification(
            NotificationItem(
                userId = tx.userId,
                title = "ডিপোজিট বাতিল হয়েছে ❌",
                message = "আপনার ৳${"%.2f".format(tx.amount)} ডিপোজিট বাতিল করা হয়েছে। কারণ: $reason",
                type = "REJECTED"
            )
        )

        return Result.success(Unit)
    }

    suspend fun approveWithdrawal(txId: Long, adminTrxId: String = ""): Result<Unit> {
        val tx = txDao.getTransactionById(txId) ?: return Result.failure(IllegalArgumentException("ট্রানজেকশন পাওয়া যায়নি"))
        if (tx.status != "PENDING") {
            return Result.failure(IllegalStateException("ট্রানজেকশন ইতিমধ্যে প্রসেস করা হয়েছে"))
        }

        val updated = tx.copy(
            status = "APPROVED",
            trxId = if (adminTrxId.isNotEmpty()) adminTrxId else tx.trxId,
            adminNote = "টাকা পাঠানো হয়েছে",
            approvedAt = System.currentTimeMillis()
        )
        txDao.updateTransaction(updated)

        // Increment totalWithdraw for user
        val user = userDao.getUserByIdSync(tx.userId)
        if (user != null) {
            userDao.updateUser(user.copy(totalWithdraw = user.totalWithdraw + tx.amount))
        }

        notifDao.insertNotification(
            NotificationItem(
                userId = tx.userId,
                title = "উত্তোলন সফল হয়েছে! 💸",
                message = "আপনার ৳${"%.2f".format(tx.amount)} (${tx.method}) নম্বরে সফলভাবে পাঠানো হয়েছে।",
                type = "WITHDRAW_APPROVED"
            )
        )

        return Result.success(Unit)
    }

    suspend fun rejectWithdrawal(txId: Long, reason: String = "ভুল ওয়ালেট নম্বর"): Result<Unit> {
        val tx = txDao.getTransactionById(txId) ?: return Result.failure(IllegalArgumentException("ট্রানজেকশন পাওয়া যায়নি"))
        if (tx.status != "PENDING") {
            return Result.failure(IllegalStateException("ট্রানজেকশন ইতিমধ্যে প্রসেস করা হয়েছে"))
        }

        val updated = tx.copy(
            status = "REJECTED",
            adminNote = reason,
            approvedAt = System.currentTimeMillis()
        )
        txDao.updateTransaction(updated)

        // Refund held balance back to user
        userDao.refundWithdrawal(tx.userId, tx.amount)

        notifDao.insertNotification(
            NotificationItem(
                userId = tx.userId,
                title = "উত্তোলন বাতিল ও রিফান্ড ⚠️",
                message = "আপনার ৳${"%.2f".format(tx.amount)} উত্তোলন বাতিল করা হয়েছে। টাকা পুনরায় আপনার ব্যালেন্সে ফিরিয়ে দেওয়া হয়েছে। কারণ: $reason",
                type = "REJECTED"
            )
        )

        return Result.success(Unit)
    }

    suspend fun adjustUserBalance(userId: Long, amountToAdd: Double, reason: String) {
        if (amountToAdd > 0) {
            userDao.creditDeposit(userId, amountToAdd)
        } else {
            userDao.holdWithdrawal(userId, -amountToAdd)
        }
        notifDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = if (amountToAdd >= 0) "ব্যালেন্স বোনাস/এডমিন ক্রেডিট" else "ব্যালেন্স সমন্বয়",
                message = "এডমিন কর্তৃক ৳${"%.2f".format(amountToAdd)} সমন্বয় করা হয়েছে। দ্রষ্টব্য: $reason",
                type = "INFO"
            )
        )
    }
}
