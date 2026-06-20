package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.api.GeminiChatService
import com.example.data.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class AppScreen {
    HOME, ACADEMY, WELLBEING, CASES, ASSISTANT, PROFILE
}

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    // Seeding and boot
    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        // Reset subconcept details when navigating
        _selectedResource.value = null
        _activeCase.value = null
    }

    // User Profile flow
    val userState: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUser(name: String, university: String, semester: Int, isDarkMode: Boolean, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            val current = userState.value ?: UserEntity()
            val updated = current.copy(
                name = name,
                university = university,
                semester = semester,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled
            )
            repository.updateUserData(updated)
        }
    }

    // Academic Resources flow
    val allResources: StateFlow<List<AcademicResourceEntity>> = repository.allResourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _studyHistory = MutableStateFlow<List<String>>(emptyList())
    val studyHistory: StateFlow<List<String>> = _studyHistory.asStateFlow()

    val filteredResources: StateFlow<List<AcademicResourceEntity>> = combine(
        allResources,
        searchQuery,
        selectedCategory
    ) { resources, query, category ->
        resources.filter { resource ->
            val matchQuery = query.isEmpty() || 
                    resource.title.contains(query, ignoreCase = true) || 
                    resource.subject.contains(query, ignoreCase = true) ||
                    resource.description.contains(query, ignoreCase = true)
            
            val matchCategory = category == "Todos" || resource.subject == category || resource.type == category
            
            matchQuery && matchCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    private val _selectedResource = MutableStateFlow<AcademicResourceEntity?>(null)
    val selectedResource: StateFlow<AcademicResourceEntity?> = _selectedResource.asStateFlow()

    fun viewResourceDetail(resource: AcademicResourceEntity) {
        _selectedResource.value = resource
        // Record in local study/consult history
        if (!_studyHistory.value.contains(resource.title)) {
            _studyHistory.value = listOf(resource.title) + _studyHistory.value.take(4)
        }
        viewModelScope.launch {
            val user = userState.value ?: UserEntity()
            repository.updateUserData(user.copy(studiedResourcesCount = user.studiedResourcesCount + 1))
            repository.earnXp(5) // Earn 5 XP for studying!
        }
    }

    fun closeResourceDetail() {
        _selectedResource.value = null
    }

    fun toggleFavorite(resource: AcademicResourceEntity) {
        viewModelScope.launch {
            repository.toggleFavoriteResource(resource.id, !resource.isFavorite)
            // Refresh detailed resource state if active
            if (_selectedResource.value?.id == resource.id) {
                _selectedResource.value = _selectedResource.value?.copy(isFavorite = !resource.isFavorite)
            }
        }
    }

    fun addResourceComment(resourceId: Int, author: String, text: String, stars: Float) {
        viewModelScope.launch {
            repository.addCommentToResource(resourceId, author, text, stars)
            // Fetch updated resource to refresh UI details
            val resources = allResources.first()
            val updated = resources.find { it.id == resourceId }
            if (updated != null) {
                _selectedResource.value = updated
            }
        }
    }

    fun uploadSharedNote(title: String, subject: String, type: String, summary: String, text: String, author: String) {
        viewModelScope.launch {
            val newResource = AcademicResourceEntity(
                title = title,
                subject = subject,
                type = type,
                description = summary,
                content = text,
                author = author,
                rating = 5.0f,
                ratingCount = 1
            )
            repository.insertResource(newResource)
            repository.earnXp(30) // uploading a community note gives big XP rewards!
        }
    }

    // Wellbeing & Emotional Tracking Flow
    val emotionalRecords: StateFlow<List<EmotionalRecordEntity>> = repository.allRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = repository.allDiaryEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitMoodRecord(mood: String, stress: Int, energy: Int, motivation: Int, note: String) {
        viewModelScope.launch {
            repository.registerMood(mood, stress, energy, motivation, note)
            val user = userState.value ?: UserEntity()
            repository.updateUserData(user.copy(wellbeingActivitiesCount = user.wellbeingActivitiesCount + 1))
        }
    }

    fun submitDiaryEntry(title: String, text: String) {
        viewModelScope.launch {
            // Simple sentiment analyzer logic for psychological wellness
            val sentiment = when {
                text.contains("triste", ignoreCase = true) || text.contains("llor", ignoreCase = true) || text.contains("mal", ignoreCase = true) -> "Melancólico"
                text.contains("feliz", ignoreCase = true) || text.contains("aleg", ignoreCase = true) || text.contains("bien", ignoreCase = true) -> "Optimista"
                text.contains("ansie", ignoreCase = true) || text.contains("miedo", ignoreCase = true) || text.contains("estres", ignoreCase = true) -> "Ansioso"
                else -> "Tranquilo"
            }
            repository.addDiaryEntry(title, text, sentiment)
            val user = userState.value ?: UserEntity()
            repository.updateUserData(user.copy(wellbeingActivitiesCount = user.wellbeingActivitiesCount + 1))
        }
    }

    // Clinical Case Simulations Flow
    val clinicalCases: StateFlow<List<ClinicalCaseEntity>> = repository.allClinicalCasesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val simulationHistory: StateFlow<List<SimulationHistoryEntity>> = repository.allHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active case state
    private val _activeCase = MutableStateFlow<ClinicalCaseEntity?>(null)
    val activeCase: StateFlow<ClinicalCaseEntity?> = _activeCase.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    private val _userScore = MutableStateFlow(100)
    val userScore: StateFlow<Int> = _userScore.asStateFlow()

    private val _currentErrorsList = MutableStateFlow<List<String>>(emptyList())
    val currentErrorsList: StateFlow<List<String>> = _currentErrorsList.asStateFlow()

    private val _simulationCompleted = MutableStateFlow(false)
    val simulationCompleted: StateFlow<Boolean> = _simulationCompleted.asStateFlow()

    private val _stepFeedback = MutableStateFlow<String?>(null)
    val stepFeedback: StateFlow<String?> = _stepFeedback.asStateFlow()

    fun startCaseSimulation(clinicalCase: ClinicalCaseEntity) {
        _activeCase.value = clinicalCase
        _currentStepIndex.value = 0
        _selectedOptionIndex.value = null
        _userScore.value = 100
        _currentErrorsList.value = emptyList()
        _simulationCompleted.value = false
        _stepFeedback.value = null
        repository.allClinicalCasesFlow
    }

    fun selectSimulationOption(optionIndex: Int) {
        val currCase = _activeCase.value ?: return
        _selectedOptionIndex.value = optionIndex

        // Decode steps
        val stepsArr = JSONArray(currCase.stepsJson)
        if (_currentStepIndex.value >= stepsArr.length()) return
        val stepObj = stepsArr.getJSONObject(_currentStepIndex.value)
        val correctIndex = stepObj.getInt("correctIndex")
        val correctText = stepObj.getJSONArray("options").getString(correctIndex)
        val selectedText = stepObj.getJSONArray("options").getString(optionIndex)

        if (optionIndex == correctIndex) {
            _stepFeedback.value = stepObj.getString("feedbackText")
        } else {
            // Subtract points on error
            _userScore.value = maxOf(20, _userScore.value - 25)
            val errorMsg = "Paso ${_currentStepIndex.value + 1}: Seleccionó '$selectedText' en lugar de '$correctText'"
            if (!_currentErrorsList.value.contains(errorMsg)) {
                _currentErrorsList.value = _currentErrorsList.value + errorMsg
            }
            _stepFeedback.value = "⚠️ Incorrecto. Intenta razonar y buscar otra opción. El error se añadirá al historial pedagógico para análisis de competencias."
        }
    }

    fun nextSimulationStep() {
        val currCase = _activeCase.value ?: return
        val stepsArr = JSONArray(currCase.stepsJson)
        
        _selectedOptionIndex.value = null
        _stepFeedback.value = null

        val nextIndex = _currentStepIndex.value + 1
        if (nextIndex < stepsArr.length()) {
            _currentStepIndex.value = nextIndex
        } else {
            // Case finalized! Save simulation history
            _simulationCompleted.value = true
            val errorsJoined = _currentErrorsList.value.joinToString("; ")
            val finalScore = _userScore.value
            viewModelScope.launch {
                repository.saveSimulationResult(currCase.id, finalScore, if (errorsJoined.isEmpty()) "Ninguno (Excelente juicio clínico)" else errorsJoined)
            }
        }
    }

    fun finishCaseSimulation() {
        _activeCase.value = null
        _simulationCompleted.value = false
    }

    // AI Assistant (MindU Assistant Chat)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendMessageToAssistant(promptText: String) {
        if (promptText.trim().isEmpty()) return
        
        viewModelScope.launch {
            // 1. Add user message to DB
            repository.addChatMessage("user", promptText)

            // 2. Load prior chat history for context
            val historyList = chatMessages.value.map { it.role to it.text }

            _isChatLoading.value = true

            // 3. Request reply from service
            val replyText = GeminiChatService.generateResponse(promptText, historyList)

            // 4. Add assistant response to DB
            repository.addChatMessage("model", replyText)
            
            _isChatLoading.value = false
            repository.earnXp(5) // studying with AI awards small study points
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // Gamification & Badges
    val unlockedBadges: StateFlow<List<UnlockedBadgeEntity>> = repository.allBadgesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
