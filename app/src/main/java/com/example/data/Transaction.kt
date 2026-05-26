package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val type: String, // "DEPOSITO", "RETIRO", "TRANSFERENCIA"
    val amount: Double,
    val description: String,
    val category: String, // "Alimentos", "Servicios", "Nómina", "Transporte", "Inversión", "Otros"
    val timestamp: Long = System.currentTimeMillis(),
    val destinationAccount: String? = null // For transfers/transfers target account number
)
