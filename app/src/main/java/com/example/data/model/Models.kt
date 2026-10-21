package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val fullName: String,
    val password: String,
    val balance: Double = 0.0,
    val totalDeposit: Double = 0.0,
    val totalWithdraw: Double = 0.0,
    val todayCommission: Double = 0.0,
    val totalCommission: Double = 0.0,
    val vipLevel: Int = 1,
    val isAdmin: Boolean = false,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val userPhone: String,
    val type: String, // "DEPOSIT", "WITHDRAW", "COMMISSION"
    val method: String, // "bKash", "Nagad", "Rocket"
    val amount: Double,
    val senderNumber: String = "",
    val receiverNumber: String = "",
    val trxId: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
)

@Entity(tableName = "linked_wallets")
data class LinkedWallet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val method: String, // "bKash", "Nagad", "Rocket"
    val accountNumber: String,
    val accountType: String = "Personal", // "Personal", "Agent"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val type: String = "INFO", // "DEPOSIT_APPROVED", "WITHDRAW_APPROVED", "REJECTED", "INFO"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_configs")
data class SystemConfig(
    @PrimaryKey
    val key: String,
    val value: String
)
