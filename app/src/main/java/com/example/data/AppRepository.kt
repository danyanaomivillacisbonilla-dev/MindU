package com.example.data

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(private val db: AppDatabase) {

    // DAOs
    private val userDao = db.userDao()
    private val resourceDao = db.academicResourceDao()
    private val emotionalRecordDao = db.emotionalRecordDao()
    private val diaryEntryDao = db.diaryEntryDao()
    private val clinicalCaseDao = db.clinicalCaseDao()
    private val simulationHistoryDao = db.simulationHistoryDao()
    private val unlockedBadgeDao = db.unlockedBadgeDao()
    private val chatMessageDao = db.chatMessageDao()

    // Flows
    val userFlow: Flow<UserEntity?> = userDao.getUser()
    val allResourcesFlow: Flow<List<AcademicResourceEntity>> = resourceDao.getAllResources()
    val allRecordsFlow: Flow<List<EmotionalRecordEntity>> = emotionalRecordDao.getAllRecords()
    val allDiaryEntriesFlow: Flow<List<DiaryEntryEntity>> = diaryEntryDao.getAllEntries()
    val allClinicalCasesFlow: Flow<List<ClinicalCaseEntity>> = clinicalCaseDao.getAllCases()
    val allHistoryFlow: Flow<List<SimulationHistoryEntity>> = simulationHistoryDao.getAllHistory()
    val allBadgesFlow: Flow<List<UnlockedBadgeEntity>> = unlockedBadgeDao.getAllBadges()
    val allChatMessagesFlow: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    // User Operations
    suspend fun updateUserData(user: UserEntity) = userDao.insertUser(user)

    suspend fun earnXp(amount: Int) {
        val currentUser = userDao.getUserSync(1) ?: UserEntity()
        val newXp = currentUser.xp + amount
        // Calculate dynamic level: 100 XP per level
        val newLevel = (newXp / 100) + 1
        val updatedUser = currentUser.copy(
            xp = newXp,
            level = if (newLevel > currentUser.level) newLevel else currentUser.level
        )
        userDao.insertUser(updatedUser)

        // Give a level-up badge if level increases
        if (newLevel > currentUser.level) {
            unlockBadge(
                badgeId = "level_$newLevel",
                title = "Estudiante Nivel $newLevel",
                description = "¡Has avanzado al nivel academico $newLevel!",
                rewardXp = newLevel * 50
            )
        }
    }

    // Resources Operations
    suspend fun searchResources(query: String): Flow<List<AcademicResourceEntity>> {
        return resourceDao.searchResources("%$query%")
    }

    suspend fun insertResource(resource: AcademicResourceEntity) = resourceDao.insertResource(resource)

    suspend fun toggleFavoriteResource(id: Int, isFav: Boolean) = resourceDao.updateFavorite(id, isFav)

    suspend fun addCommentToResource(id: Int, username: String, commentText: String, rating: Float) {
        val resource = resourceDao.getResourceById(id) ?: return
        val currentComments = JSONArray(resource.commentsJson)
        val newComment = JSONObject().apply {
            put("username", username)
            put("text", commentText)
            put("timestamp", System.currentTimeMillis())
        }
        currentComments.put(newComment)
        
        // Compute new rating
        val newCount = resource.ratingCount + 1
        val newRating = ((resource.rating * resource.ratingCount) + rating) / newCount

        resourceDao.updateCommentsAndRating(id, newRating, newCount, currentComments.toString())
        earnXp(15) // earn 15 XP for collaborative discussion
    }

    // Wellbeing Operations
    suspend fun registerMood(mood: String, stress: Int, energy: Int, motivation: Int, note: String) {
        val record = EmotionalRecordEntity(
            mood = mood,
            stressLevel = stress,
            energyLevel = energy,
            motivationLevel = motivation,
            note = note
        )
        emotionalRecordDao.insertRecord(record)
        earnXp(20) // earn XP for emotional self-care tracking
    }

    suspend fun addDiaryEntry(title: String, text: String, sentiment: String) {
        val entry = DiaryEntryEntity(
            title = title,
            text = text,
            sentiment = sentiment
        )
        diaryEntryDao.insertEntry(entry)
        earnXp(25) // tracking writing habits
    }

    // Clinical Simulation Operations
    suspend fun getCaseById(id: Int): ClinicalCaseEntity? = clinicalCaseDao.getCaseById(id)

    suspend fun saveSimulationResult(caseId: Int, score: Int, freqErrors: String) {
        val cases = clinicalCaseDao.getAllCases().first()
        val cCase = cases.find { it.id == caseId }
        val caseTitle = cCase?.title ?: "Caso Clínico"
        val difficulty = cCase?.difficulty ?: "Básico"

        val history = SimulationHistoryEntity(
            caseId = caseId,
            caseTitle = caseTitle,
            difficulty = difficulty,
            score = score,
            frequentErrors = freqErrors
        )
        simulationHistoryDao.insertHistory(history)

        val currentUser = userDao.getUserSync(1) ?: UserEntity()
        val updatedUser = currentUser.copy(
            completedCasesCount = currentUser.completedCasesCount + 1
        )
        userDao.insertUser(updatedUser)

        earnXp(score * 2) // earn XP based on score! Max 200 XP for perfect score!

        if (score >= 90) {
            unlockBadge(
                badgeId = "case_pro_${caseId}",
                title = "Clínico Excelente: $caseTitle",
                description = "Completaste el caso con una puntuacion brillante de $score%",
                rewardXp = 100
            )
        }
    }

    // Gamification & Badges
    suspend fun unlockBadge(badgeId: String, title: String, description: String, rewardXp: Int) {
        val badge = UnlockedBadgeEntity(badgeId, title, description, rewardXp)
        unlockedBadgeDao.insertBadge(badge)
    }

    // Chat
    suspend fun getChatHistory(): Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun addChatMessage(role: String, text: String) {
        val msg = ChatMessageEntity(role = role, text = text)
        chatMessageDao.insertMessage(msg)
    }

    suspend fun clearChat() = chatMessageDao.clearChat()

    // Database Initialization (Seeding default data)
    suspend fun seedInitialDataIfNecessary() {
        val userCheck = userDao.getUserSync(1)
        if (userCheck == null) {
            // 1. Seed default user
            userDao.insertUser(UserEntity())

            // 2. Seed academic resources
            resourceDao.insertResource(
                AcademicResourceEntity(
                    title = "Manual de Terapia Cognitivo Conductual (TCC)",
                    subject = "Psicología Clínica",
                    type = "Guía de estudio",
                    description = "Conceptos clave, registro de pensamientos automáticos y reestructuración cognitiva paso a paso.",
                    content = """La Terapia Cognitivo Conductual (TCC) es la corriente con mayor evidencia científica para la ansiedad y la depresión. Se basa en el modelo A-B-C:
- (A) Acontecimiento activador.
- (B) Creencias y pensamientos (Beliefs).
- (C) Consecuencias emocionales y conductuales.

Herramientas esenciales:
1. Reestructuración cognitiva: Identificación de distorsiones comunes como el Catastrofismo ("Todo va a salir mal") y Abstracción selectiva (enfocarse en lo negativo).
2. Registro de Pensamientos Automáticos (RPA) de 3, 5 y 7 columnas.
3. Técnicas de exposición gradual e in-vivo.
4. Activación Conductual como terapia independiente.""",
                    rating = 4.8f,
                    ratingCount = 12,
                    commentsJson = """[
                        {"username": "Dr_Lopez_UBA", "text": "Excelente sintesis para preparar el examen de Clinica.", "timestamp": 1781986500000},
                        {"username": "Matias_Psic", "text": "Muy util el formato de registro ABC. ¡Gracias!", "timestamp": 1781986600000}
                    ]"""
                )
            )

            resourceDao.insertResource(
                AcademicResourceEntity(
                    title = "Fisiología del SN Autónomo y Regulación",
                    subject = "Bases Neurobiológicas",
                    type = "Resumen",
                    description = "Función simpática y parasimpática en la respuesta al estrés agudo y ansiedad.",
                    content = """El Sistema Nervioso Autónomo (SNA) se encarga de regular las funciones viscerales involuntarias y es el motor biológico del estrés. Se ramifica en:
1. Sistema Nervioso Simpático (SNS): Prepara al cuerpo para 'pelear o huir'. Provoca liberación de adrenalina, noradrenalina y cortisol por las glándulas suprarrenales. Causa taquicardia, broncodilatación, midriasis y detención digestiva.
2. Sistema Nervioso Parasimpático (SNP): Promueve el 'descanso y digestión'. Estimulado por el nervio vago y regulado por la acetilcolina. Causa desaceleración del ritmo cardíaco, miosis y activación gástrica.

Implicación en clínica: En trastornos de angustia o estrés postraumático (TEPT), la hipersensibilidad del SNS mantiene al paciente en un estado constante de alerta. Las técnicas de biofeedback y respiración profunda actúan estimulando intencionalmente el vago parasimpático.""",
                    rating = 4.6f,
                    ratingCount = 8,
                    commentsJson = """[
                        {"username": "Sophia_Neuron", "text": "Un resumen perfecto del capitulo del Kandel.", "timestamp": 1781986420000}
                    ]"""
                )
            )

            resourceDao.insertResource(
                AcademicResourceEntity(
                    title = "Teoría del Apego de Bowlby",
                    subject = "Psicología del Desarrollo",
                    type = "Mapa conceptual",
                    description = "Estructura interactiva de los estilos de apego infantil y su proyección hacia relaciones de pareja adultas.",
                    content = """John Bowlby y Mary Ainsworth diseñaron la 'Situación Extraña' para clasificar los vínculos entre el cuidador y el infante, resultando en cuatro tipos principales:

1. Apego Seguro:
- Infancia: Explora confiado. Llora ante la separación pero se calma rápidamente al regreso de la madre.
- Adultez: Relaciones empáticas, equilibrio de autonomía y dependencia, seguridad afectiva.

2. Apego Inseguro-Evitativo:
- Infancia: Indiferente al cuidador. Suprime el llanto visible pero los niveles de conductancia cutánea muestran un alto estrés interno.
- Adultez: Miedo a la intimidad, autosuficiencia compulsiva, bloqueo de emociones.

3. Apego Inseguro-Ansioso-Ambivalente:
- Infancia: Ansiedad extrema ante la separación. Al regreso, muestra ira mezcolando búsqueda de contacto con rechazo hostil.
- Adultez: Preocupación constante por el abandono, dependencia emocional intensa, necesidad de validación.

4. Apego Desorganizado (Main y Solomon):
- Infancia: Respuestas contradictorias (acercarse dando la espalda), miedo al cuidador (visto como fuente de amenaza y amparo). Común en maltrato infantil.""",
                    rating = 4.7f,
                    ratingCount = 15,
                    commentsJson = """[
                        {"username": "Eduardo_Dev", "text": "Crucial para vincular psicopatologia infanto-juvenil con apego.", "timestamp": 1781986300000}
                    ]"""
                )
            )

            // 3. Seed Interactive Clinical Cases
            seedCases()

            // 4. Seed initial Badges Achievements
            unlockedBadgeDao.insertBadge(
                UnlockedBadgeEntity(
                    badgeId = "bienvenida",
                    title = "Bienvenido a MindU",
                    description = "Iniciaste tu camino de integracion como futuro profesional de la psicologia.",
                    xpReward = 50
                )
            )
        }
    }

    private suspend fun seedCases() {
        // Clara Case: Anxiety (Básico)
        clinicalCaseDao.insertCase(
            ClinicalCaseEntity(
                title = "Clara: Sintomatología de la Ansiedad",
                difficulty = "Básico",
                category = "Ansiedad",
                patientName = "Clara",
                patientAge = 24,
                patientGender = "Femenino",
                caseSummary = "Paciente de 24 años consulta por dificultades de concentración, insomnio de conciliación, palpitaciones recurrentes y preocupación excesiva por su rendimiento universitario.",
                clinicalHistory = """Clara es estudiante de último año de kinesiología. Menciona que las palpitaciones aumentaron en los últimos meses ante la cercanía de sus exámenes finales. Reporta que siente tensión en los hombros, temblores en las manos y que 'no puede parar la cabeza pensando que fracasará'. Describe que estos síntomas le ocurren en su casa e impiden que descanse adecuadamente o se concentre para estudiar. No presenta antecedentes de consumo de sustancias y su médico general descartó problemas de tiroides o cardiovasculares.""",
                symptomsJson = """["Insomnio de conciliación", "Palpitaciones recurrentes", "Dificultades de concentración", "Preocupación excesiva", "Tensión muscular"]""",
                stepsJson = """[
                    {
                        "stepNumber": 1,
                        "title": "Identificación de Síntomas",
                        "instruction": "clara refiere estar cansada y preocupada todo el dia. Analiza el caso y selecciona cuál de los siguientes NO es un síntoma compatible indicado en la historia clínica del paciente:",
                        "options": [
                            "Dificultades de concentración",
                            "Tensión muscular",
                            "Consumo abusivo de sustancias estimulantes",
                            "Palpitaciones recurrentes"
                        ],
                        "correctIndex": 2,
                        "feedbackText": "Correcto. La historia clínica de Clara descarta explícitamente el consumo de estimulantes o sustancias, y sus síntomas se asocian de manera pura a un cuadro ansioso generalizado."
                    },
                    {
                        "stepNumber": 2,
                        "title": "Formulación de Hipótesis",
                        "instruction": "Teniendo en cuenta que los síntomas duran más de 6 meses, afectan áreas significativas de su vida (estudio) y no son causados por sustancias ni afección médica general, ¿cuál es tu diagnóstico presuntivo?",
                        "options": [
                            "Trastorno Depresivo Mayor",
                            "Trastorno de Ansiedad Generalizada (TAG)",
                            "Trastorno de Pánico",
                            "Fobia Social"
                        ],
                        "correctIndex": 1,
                        "feedbackText": "¡Excelente elección! Cumple con los criterios DSM-5 para TAG: preocupación excesiva casi todos los días sobre múltiples eventos (estudiantes, salud), dificultad para controlar la preocupación, asociando tensión de hombros e insomnio."
                    },
                    {
                        "stepNumber": 3,
                        "title": "Estrategias de Intervención",
                        "instruction": "Para iniciar el tratamiento psicoterapéutico desde una perspectiva científica con Clara, ¿cuál terapia combinada de técnicas de primera elección deberías priorizar?",
                        "options": [
                            "Terapia de choque y confrontación de miedos extremos",
                            "Exposición gradual in-vivo inmediata de 8 horas continuas",
                            "Psicoanálisis clásico basado en la libre asociación de ideas durante un año",
                            "Reestructuración cognitiva basada en TCC junto a entrenamiento en respiración diafragmática y psicoeducación"
                        ],
                        "correctIndex": 3,
                        "feedbackText": "¡Correcto! La psicoeducación (explicarle que la ansiedad es una respuesta adaptativa normal), la respiración diafragmática (para modular el vago simpático) y la reestructuración de pensamientos catastrofistas son herramientas de oro en TAG."
                    }
                ]"""
            )
        )

        // Andres Case: Depression (Intermedio)
        clinicalCaseDao.insertCase(
            ClinicalCaseEntity(
                title = "Andrés: Tristeza y Anhedonia",
                difficulty = "Intermedio",
                category = "Depresión",
                patientName = "Andrés",
                patientAge = 32,
                patientGender = "Masculino",
                caseSummary = "Andrés describe desgano generalizado, tristeza prolongada, anhedonia (pérdida del placer) y pérdida significativa de peso en los últimos 3 meses.",
                clinicalHistory = """Andrés informa que tras la pérdida de su empleo hace 6 meses, ha experimentado apatía generalizada. Relata que ya no disfruta jugar fútbol con sus amigos (cosa que antes adoraba), se despierta de madrugada incapaz de conciliar el sueño y tiene sentimientos intensos de inutilidad y culpa por no aportar económicamente a su hogar.""",
                symptomsJson = """["Anhedonia", "Pérdida de peso", "Insomnio tardío", "Sentimientos de inutilidad", "Desgano prolongado"]""",
                stepsJson = """[
                    {
                        "stepNumber": 1,
                        "title": "Evaluación del Ánimo y la Anhedonia",
                        "instruction": "Andrés manifiesta que 'nada le genera alegría ni placer'. En psicología clínica, la inhabilidad para experimentar placer a partir de actividades que antes sí lo generaban se conoce científicamente como:",
                        "options": [
                            "Abulia",
                            "Anhedonia",
                            "Apatía profunda",
                            "Alexitimia"
                        ],
                        "correctIndex": 1,
                        "feedbackText": "¡Perfecto! La anhedonia es un síntoma cardinal de los trastornos del estado del ánimo (depresión)."
                    },
                    {
                        "stepNumber": 2,
                        "title": "Evaluación de Riesgo",
                        "instruction": "En un cuadro depresivo clínicamente moderado/grave, la primera e indispensable medida que un psicólogo ético debe evaluar de manera directa durante la entrevista clínica inicial es:",
                        "options": [
                            "La motivación del paciente para hacer tareas en casa",
                            "La presencia de ideación u planes de autolisis (suicidio)",
                            "La relación sentimental de sus padres",
                            "La calidad del sueño REM"
                        ],
                        "correctIndex": 1,
                        "feedbackText": "¡Crítico y Correcto! Resguardar la integridad del paciente es una prioridad ética y clínica inquebrantable. Evaluar ideación suicida directa es ineludible."
                    },
                    {
                        "stepNumber": 3,
                        "title": "Diseño Terapéutico",
                        "instruction": "Andrés se muestra muy abrumado con pensamientos recurrentes sobre su inutilidad. ¿Qué protocolo empírico TCC se prescribe inicialmente para promover un cambio conductual y reactivación del ánimo en el paciente?",
                        "options": [
                            "Activación Conductual (programación de actividades agradables estructuradas)",
                            "Análisis transaccional grupal interactivo",
                            "Reiki y terapia complementaria semanal",
                            "Exposición interoceptiva repetida"
                        ],
                        "correctIndex": 0,
                        "feedbackText": "¡Excelente elección! El protocolo de Activación Conductual tiene tremendo impacto empírico. Al programar actividades placenteras y de logro, rompemos el bucle de evitación y aislamiento depresivo."
                    }
                ]"""
            )
        )

        // Mateo Case: Trastorno del Sueño Infantil (Avanzado)
        clinicalCaseDao.insertCase(
            ClinicalCaseEntity(
                title = "Mateo: Terrores Nocturnos e Insomnio",
                difficulty = "Avanzado",
                category = "Casos Infantiles",
                patientName = "Mateo",
                patientAge = 8,
                patientGender = "Masculino",
                caseSummary = "Mateo, un niño de 8 años, presenta episodios súbitos de pánico de madrugada, gritos que no recuerda al despertar y un marcado rechazo de ir a dormir solo.",
                clinicalHistory = """Los padres de Mateo reportan que de 2 a 3 veces por semana, unas horas después de conciliar el sueño, Mateo se sienta en la cama gritando, sudoroso, con ojos abiertos y taquicardia. No responde a consuelos y parece no reconocerlos por un lapso de 10 minutos, luego vuelve a dormir plácidamente. Al día siguiente Andrés/Mateo no tiene ningún recuerdo de lo que pasó. Esto se asocia a angustia familiar y rechazo de acostarse solo.""",
                symptomsJson = """["Terrores nocturnos", "Sudoración fría nocturna", "Desorientación episódica", "Insomnio de conciliación secundario", "Amnesia del evento"]""",
                stepsJson = """[
                    {
                        "stepNumber": 1,
                        "title": "Diferenciación Diagnóstica",
                        "instruction": "Dado el despertar brusco con pánico, síntomas autonómicos de taquicardia extrema, falta de reactividad al consuelo parental y amnesia completa al día siguiente, estamos ante un caso clínico clásico de parasomnia del sueño No-REM denominado:",
                        "options": [
                            "Pesadillas infantiles severas (Sueño REM)",
                            "Síndrome de piernas inquietas",
                            "Terrores Nocturnos",
                            "Trastorno de Ansiedad de Separación puro"
                        ],
                        "correctIndex": 2,
                        "feedbackText": "¡Perfecto! Los Terrores Nocturnos ocurren típicamente en el primer tercio de la noche en el sueño No-REM profundo. Se caracterizan por respuesta protectora disfuncional y amnesia del suceso."
                    },
                    {
                        "stepNumber": 2,
                        "title": "Psicoeducación de Padres",
                        "instruction": "Los padres están aterrorizados pensando que Mateo tiene crisis epilépticas o está poseído de noche. Como psicólogo calificado, ¿qué recomendación de manejo familiar inmediata debes pautar?",
                        "options": [
                            "Despertar bruscamente a Mateo apenas empieza a gritar y obligarlo a encender la luz",
                            "Psicoeducar a los padres confirmándoles que es un proceso del neurodesarrollo benigno, sugiriendo asegurar el entorno físico (prevenir caídas) y acompañarlo pacientemente sin perturbar el episodio",
                            "Recomendar medicación ansiolítica pesada de inmediato",
                            "Castigar las conductas de manipulación infantil diurnas"
                        ],
                        "correctIndex": 1,
                        "feedbackText": "¡Soberbio! Tratar de despertar violentamente al niño bajo un terror nocturno puede prolongar y exacerbar la agitación clínica. La contención pacífica, mantener la calma parental y resguardar la seguridad física contra caídas son pilares indicados."
                    }
                ]"""
            )
        )
    }
}
