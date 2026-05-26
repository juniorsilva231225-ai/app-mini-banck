package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM bank_accounts ORDER BY id ASC")
    fun getAllAccountsState(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId ORDER BY id ASC")
    fun getAccountsForUser(userId: Int): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Int): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE accountNumber = :accNumber")
    suspend fun getAccountByNumber(accNumber: String): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccount): Long

    @Update
    suspend fun updateAccount(account: BankAccount)

    @Delete
    suspend fun deleteAccount(account: BankAccount)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long
}
