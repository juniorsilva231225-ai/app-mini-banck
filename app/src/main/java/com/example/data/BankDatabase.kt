package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [BankAccount::class, Transaction::class, User::class], version = 1, exportSchema = false)
abstract class BankDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao

    companion object {
        @Volatile
        private var INSTANCE: BankDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BankDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BankDatabase::class.java,
                    "bank_database"
                )
                .addCallback(BankDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class BankDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.bankDao())
                }
            }
        }

        suspend fun populateDatabase(bankDao: BankDao) {
            // Check if default user already exists (just in case)
            val seedUser = User(
                username = "javier",
                passwordPlain = "1234",
                fullName = "Javier Pérez"
            )
            val userId = bankDao.insertUser(seedUser).toInt()

            // Seed user #2 to allow testing switching users and seeing different accounts
            val secondUser = User(
                username = "maria",
                passwordPlain = "1234",
                fullName = "María Rodríguez"
            )
            val user2Id = bankDao.insertUser(secondUser).toInt()

            // Initial Accounts for Javier (userId)
            val acc1Id = bankDao.insertAccount(
                BankAccount(
                    userId = userId,
                    name = "Cuenta Corriente",
                    accountNumber = "9876-5432-1011-1213",
                    balance = 1250.75,
                    currency = "USD",
                    type = "Corriente",
                    colorHex = "#1E3C72" // Royal metallic blue
                )
            )
            val acc2Id = bankDao.insertAccount(
                BankAccount(
                    userId = userId,
                    name = "Cuenta de Ahorros",
                    accountNumber = "1234-5678-9012-3456",
                    balance = 5430.50,
                    currency = "USD",
                    type = "Ahorros",
                    colorHex = "#0F2027" // Deep space grey/teal gradient
                )
            )

            // Initial Accounts for Maria (user2Id)
            val acc3Id = bankDao.insertAccount(
                BankAccount(
                    userId = user2Id,
                    name = "Ahorro Personal",
                    accountNumber = "3141-5926-5358-9793",
                    balance = 8200.00,
                    currency = "USD",
                    type = "Ahorros",
                    colorHex = "#4B0082" // Deep Indigo
                )
            )

            // Initial Transactions for acc1 (Javier's Cuenta Corriente)
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc1Id.toInt(),
                    type = "DEPOSITO",
                    amount = 1500.00,
                    description = "Depósito de nómina quincenal",
                    category = "Nómina"
                )
            )
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc1Id.toInt(),
                    type = "RETIRO",
                    amount = 120.50,
                    description = "Supermercado La Unión",
                    category = "Alimentos"
                )
            )
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc1Id.toInt(),
                    type = "RETIRO",
                    amount = 45.00,
                    description = "Gasolinera Uno",
                    category = "Transporte"
                )
            )
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc1Id.toInt(),
                    type = "RETIRO",
                    amount = 83.75,
                    description = "Pago de servicio de energía eléctrica",
                    category = "Servicios"
                )
            )

            // Initial Transactions for acc2 (Javier's Cuenta de Ahorros)
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc2Id.toInt(),
                    type = "DEPOSITO",
                    amount = 5000.00,
                    description = "Depósito inicial de ahorros",
                    category = "Otros"
                )
            )
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc2Id.toInt(),
                    type = "DEPOSITO",
                    amount = 430.50,
                    description = "Rentabilidad de fondos de inversión",
                    category = "Inversión"
                )
            )

            // Initial Transactions for acc3 (Maria's Ahorro)
            bankDao.insertTransaction(
                Transaction(
                    accountId = acc3Id.toInt(),
                    type = "DEPOSITO",
                    amount = 8200.00,
                    description = "Saldo de apertura",
                    category = "Otros"
                )
            )
        }
    }
}
