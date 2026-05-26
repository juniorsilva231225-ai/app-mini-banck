package com.example.data

import kotlinx.coroutines.flow.Flow

class BankRepository(private val bankDao: BankDao) {
    val allAccounts: Flow<List<BankAccount>> = bankDao.getAllAccountsState()
    val allTransactions: Flow<List<Transaction>> = bankDao.getAllTransactions()

    fun getAccountsForUser(userId: Int): Flow<List<BankAccount>> {
        return bankDao.getAccountsForUser(userId)
    }

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> {
        return bankDao.getTransactionsForAccount(accountId)
    }

    suspend fun getAccountById(accountId: Int): BankAccount? {
        return bankDao.getAccountById(accountId)
    }

    suspend fun getAccountByNumber(accNumber: String): BankAccount? {
        return bankDao.getAccountByNumber(accNumber)
    }

    suspend fun insertAccount(account: BankAccount): Long {
        return bankDao.insertAccount(account)
    }

    suspend fun updateAccount(account: BankAccount) {
        bankDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: BankAccount) {
        bankDao.deleteAccount(account)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return bankDao.insertTransaction(transaction)
    }

    suspend fun getUserByUsername(username: String): User? {
        return bankDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User): Long {
        return bankDao.insertUser(user)
    }
}
