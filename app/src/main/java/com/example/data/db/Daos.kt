package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LinkedWallet
import com.example.data.model.NotificationItem
import com.example.data.model.SystemConfig
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdSync(id: Long): User?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone AND password = :password LIMIT 1")
    suspend fun login(phone: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("UPDATE users SET balance = balance + :amount, totalDeposit = totalDeposit + :amount WHERE id = :userId")
    suspend fun creditDeposit(userId: Long, amount: Double)

    @Query("UPDATE users SET balance = balance - :amount, totalWithdraw = totalWithdraw + :amount WHERE id = :userId")
    suspend fun debitWithdrawal(userId: Long, amount: Double)

    @Query("UPDATE users SET balance = balance - :amount WHERE id = :userId")
    suspend fun holdWithdrawal(userId: Long, amount: Double)

    @Query("UPDATE users SET balance = balance + :amount WHERE id = :userId")
    suspend fun refundWithdrawal(userId: Long, amount: Double)
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionsForUser(userId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTransactionsForUser(userId: Long, limit: Int = 10): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'DEPOSIT' AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingDeposits(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'WITHDRAW' AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingWithdrawals(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?
}

@Dao
interface LinkedWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: LinkedWallet): Long

    @Query("DELETE FROM linked_wallets WHERE id = :id")
    suspend fun deleteWallet(id: Long)

    @Query("SELECT * FROM linked_wallets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWalletsForUser(userId: Long): Flow<List<LinkedWallet>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationItem>>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: Long)
}

@Dao
interface SystemConfigDao {
    @Query("SELECT value FROM system_configs WHERE `key` = :key LIMIT 1")
    suspend fun getConfigValue(key: String): String?

    @Query("SELECT value FROM system_configs WHERE `key` = :key LIMIT 1")
    fun getConfigFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: SystemConfig)

    @Query("SELECT * FROM system_configs")
    fun getAllConfigs(): Flow<List<SystemConfig>>
}
