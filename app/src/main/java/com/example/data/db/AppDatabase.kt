package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LinkedWallet
import com.example.data.model.NotificationItem
import com.example.data.model.SystemConfig
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Transaction::class,
        LinkedWallet::class,
        NotificationItem::class,
        SystemConfig::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun linkedWalletDao(): LinkedWalletDao
    abstract fun notificationDao(): NotificationDao
    abstract fun systemConfigDao(): SystemConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ewallet_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(db: AppDatabase) {
                val configDao = db.systemConfigDao()
                configDao.setConfig(SystemConfig("deposit_number", "01960067933"))
                configDao.setConfig(SystemConfig("min_deposit", "100"))
                configDao.setConfig(SystemConfig("min_withdraw", "200"))
                configDao.setConfig(SystemConfig("support_phone", "01960067933"))
                configDao.setConfig(SystemConfig("support_telegram", "@ewallet_support"))

                val userDao = db.userDao()
                // Default Admin
                userDao.insertUser(
                    User(
                        id = 1,
                        phoneNumber = "01960067933",
                        fullName = "Admin Panel",
                        password = "admin",
                        balance = 50000.0,
                        totalDeposit = 50000.0,
                        totalWithdraw = 0.0,
                        isAdmin = true
                    )
                )

                // Demo User
                val demoUserId = userDao.insertUser(
                    User(
                        id = 2,
                        phoneNumber = "01712345678",
                        fullName = "Md. Imran Hossain",
                        password = "user123",
                        balance = 0.0,
                        totalDeposit = 0.0,
                        totalWithdraw = 0.0,
                        todayCommission = 0.0,
                        totalCommission = 0.0,
                        isAdmin = false
                    )
                )

                val notifDao = db.notificationDao()
                notifDao.insertNotification(
                    NotificationItem(
                        userId = demoUserId,
                        title = "স্বাগতম E-WALLET এ!",
                        message = "আপনার অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে। ডিপোজিট করতে Deposit বাটনে ক্লিক করুন।",
                        type = "INFO"
                    )
                )
            }
        }
    }
}
