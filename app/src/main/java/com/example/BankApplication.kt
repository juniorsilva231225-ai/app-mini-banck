package com.example

import android.app.Application
import com.example.data.BankDatabase
import com.example.data.BankRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class BankApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { BankDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { BankRepository(database.bankDao()) }
}
