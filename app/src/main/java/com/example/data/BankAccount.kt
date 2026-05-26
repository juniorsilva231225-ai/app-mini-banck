package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val accountNumber: String,
    val balance: Double,
    val currency: String = "USD",
    val type: String, // "Ahorros" (Savings), "Corriente" (Checking)
    val colorHex: String // Custom color for the card UI (e.g. #1E3C72, #0F2027)
)
