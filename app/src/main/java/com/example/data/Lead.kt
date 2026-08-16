package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val budgetRange: String = "Any",
    val propertyType: String = "Apartment",
    val locationOfInterest: String = "Any",
    val notesSummary: String = "",
    val keywordsJson: String = "[]", // Stores tags like ["3-bed", "pool", "ready"]
    val status: String = "New",
    val sourceType: String = "Manual", // "Manual", "Call Log", "Audio Note", "Camera Capture", "Notch Trigger"
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    
    // Clean numeric-only behavior metrics (no unit names, no percents, focus on action/call spikes)
    val lastCallsCount: Int = 0,
    val avgDuration: Int = 0,
    val behaviorSpikes: Int = 0, // Spike level count
    val normalRate: Int = 0     // Normal baseline rate count
)
