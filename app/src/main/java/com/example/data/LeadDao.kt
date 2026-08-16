package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY timestamp DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id LIMIT 1")
    fun getLeadById(id: Int): Flow<Lead?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: Int)

    @Query("""
        SELECT * FROM leads 
        WHERE name LIKE '%' || :query || '%' 
        OR phone LIKE '%' || :query || '%' 
        OR locationOfInterest LIKE '%' || :query || '%' 
        OR propertyType LIKE '%' || :query || '%' 
        OR keywordsJson LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchLeads(query: String): Flow<List<Lead>>

    @Query("UPDATE leads SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, synced: Boolean)
}
