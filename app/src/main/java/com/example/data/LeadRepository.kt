package com.example.data

import kotlinx.coroutines.flow.Flow

class LeadRepository(private val leadDao: LeadDao) {
    val allLeads: Flow<List<Lead>> = leadDao.getAllLeads()

    fun getLeadById(id: Int): Flow<Lead?> = leadDao.getLeadById(id)

    suspend fun insertLead(lead: Lead): Long = leadDao.insertLead(lead)

    suspend fun updateLead(lead: Lead) = leadDao.updateLead(lead)

    suspend fun deleteLead(lead: Lead) = leadDao.deleteLead(lead)

    suspend fun deleteLeadById(id: Int) = leadDao.deleteLeadById(id)

    fun searchLeads(query: String): Flow<List<Lead>> = leadDao.searchLeads(query)

    suspend fun updateSyncStatus(id: Int, synced: Boolean) = leadDao.updateSyncStatus(id, synced)
}
