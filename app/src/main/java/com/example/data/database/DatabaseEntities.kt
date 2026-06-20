package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Estudiante de Psicología",
    val university: String = "Universidad Central de Psicología",
    val semester: Int = 5,
    val xp: Int = 120,
    val level: Int = 1,
    val completedCasesCount: Int = 0,
    val studiedResourcesCount: Int = 0,
    val wellbeingActivitiesCount: Int = 0,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isPrivateProfile: Boolean = false
)

@Entity(tableName = "academic_resources")
data class AcademicResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val type: String, // "Resumen", "Guía de estudio", "Mapa conceptual", "Artículo"
    val description: String,
    val content: String,
    val rating: Float = 4.5f,
    val ratingCount: Int = 1,
    val author: String = "MindU Admin",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val commentsJson: String = "[]" // JSON representation of List of Comment Objects
)

@Entity(tableName = "emotional_records")
data class EmotionalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String, // "Feliz", "Tranquilo", "Ansioso", "Estresado", "Cansado"
    val stressLevel: Int, // 1 - 5
    val energyLevel: Int, // 1 - 5
    val motivationLevel: Int, // 1 - 5
    val note: String
)

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val text: String,
    val sentiment: String = "Neutral"
)

@Entity(tableName = "clinical_cases")
data class ClinicalCaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val difficulty: String, // "Básico", "Intermedio", "Avanzado"
    val category: String, // "Ansiedad", "Depresión", "Estres", "Problemas de sueño", "Dificultades emocionales", "Casos familiares", "Casos infantiles"
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val caseSummary: String,
    val clinicalHistory: String,
    val symptomsJson: String, // List of symptoms e.g. ["Palpitaciones", "Insomnio"]
    val stepsJson: String // Serialized diagnostic steps
)

@Entity(tableName = "simulation_histories")
data class SimulationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val caseTitle: String,
    val difficulty: String,
    val score: Int,
    val frequentErrors: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "unlocked_badges")
data class UnlockedBadgeEntity(
    @PrimaryKey val badgeId: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user", "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
