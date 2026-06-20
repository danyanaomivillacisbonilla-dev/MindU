package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val isDark = userState?.isDarkMode ?: false

    MyApplicationTheme(darkTheme = isDark) {
        val snackbarHostState = remember { SnackbarHostState() }
        
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                MindUBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(viewModel)
                        AppScreen.ACADEMY -> AcademyScreen(viewModel)
                        AppScreen.WELLBEING -> WellbeingScreen(viewModel)
                        AppScreen.CASES -> CasesScreen(viewModel)
                        AppScreen.ASSISTANT -> AssistantScreen(viewModel)
                        AppScreen.PROFILE -> ProfileScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    com.example.ui.theme.MyApplicationTheme(darkTheme = darkTheme) {
        content()
    }
}

// BOTTOM NAVIGATION
@Composable
fun MindUBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val items = listOf(
            NavigationItem(AppScreen.HOME, "Inicio", Icons.Default.Home),
            NavigationItem(AppScreen.ACADEMY, "Academia", Icons.Default.List),
            NavigationItem(AppScreen.WELLBEING, "Bienestar", Icons.Default.Favorite),
            NavigationItem(AppScreen.CASES, "Casos", Icons.Default.Edit),
            NavigationItem(AppScreen.ASSISTANT, "Asistente", Icons.Default.Send),
            NavigationItem(AppScreen.PROFILE, "Perfil", Icons.Default.Person)
        )

        items.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}

data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

// PANTALLA 1: HOME SCREEN
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val allHistory by viewModel.simulationHistory.collectAsStateWithLifecycle()
    val allBadges by viewModel.unlockedBadges.collectAsStateWithLifecycle()
    val records by viewModel.emotionalRecords.collectAsStateWithLifecycle()

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingMessage = when {
        currentHour < 12 -> "¡Buenos días,"
        currentHour < 19 -> "¡Buenas tardes,"
        else -> "¡Buenas noches,"
    }

    val motivationalPhrases = listOf(
        "\"La curiosa paradoja es que cuando me acepto tal como soy, entonces puedo cambiar.\" - Carl Rogers",
        "\"De nuestras vulnerabilidades surgirá tu fuerza.\" - Sigmund Freud",
        "\"La mente no es un vaso que llenar, sino un fuego que encender.\" - Plutarco",
        "\"Quien tiene un porqué para vivir encontrará casi siempre el cómo.\" - Viktor Frankl",
        "\"La empatía es la herramienta de sanación más sofisticada de la psicología clínica.\""
    )
    val randomPhrase = remember { motivationalPhrases.random() }

    // Detected logged mood for highlight
    var clickedMood by remember { mutableStateOf<String?>(null) }
    val lastRecord = records.lastOrNull()
    val isRecentlyLogged = lastRecord != null && (System.currentTimeMillis() - lastRecord.timestamp < 12 * 60 * 60 * 1000)
    val currentLoggedMood = if (isRecentlyLogged) lastRecord?.mood else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 1. HEADER SECTION (BENTO STYLE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MindU",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BentoBlue)
                        )
                    }
                    Text(
                        text = "\"Comprender para sanar\"",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = BentoSlateLight
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Level / stats circular bubble selector
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 18.sp)
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(BentoMint)
                                .border(1.5.dp, Color.White, CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${user?.level ?: 4}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoSlateDark
                            )
                        }
                    }

                    // Avatar photo/emoji circle block
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoBlue.copy(alpha = 0.6f))
                            .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                            .shadow(1.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 20.sp)
                    }
                }
            }
        }

        // 2. WELCOME & GAMIFICATION PROGRESS TRACKER (BENTO CARD CARD)
        item {
            val xpMax = 100
            val currentXpInLevel = (user?.xp ?: 0) % xpMax
            val progressFraction = currentXpInLevel.toFloat() / xpMax.toFloat()

            Card(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Sofia & stats title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "¡Hola, ${user?.name ?: "Sofía"}! 👋",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Semestre ${user?.semester ?: 6} • ${user?.university ?: "Univ. Autónoma"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BentoSlateLight
                            )
                        }

                        // Premium Level badge pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoSlateDark.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Nivel ${user?.level ?: 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress detail row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso de Aprendizaje",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoSlateLight
                        )
                        Text(
                            text = "$currentXpInLevel / $xpMax XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Level Up indicator bar
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = BentoMint,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Embedded 3 Indicators Stat Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val stats = listOf(
                            Triple("Casos", user?.completedCasesCount ?: 0, BentoRose),
                            Triple("Apuntes", user?.studiedResourcesCount ?: 0, BentoBlue),
                            Triple("Días Calma", user?.wellbeingActivitiesCount ?: 0, BentoMint)
                        )

                        stats.forEachIndexed { index, (label, value, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$value",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlateLight
                                    )
                                }
                            }
                            if (index < 2) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK MOOD CHECK SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ESTADO EMOCIONAL HOY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = BentoSlateLight,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val moodOptions = listOf(
                    Quad("😌", "Calma", "Tranquilo", BentoMint),
                    Quad("🤩", "Enfoque", "Feliz", BentoBlue),
                    Quad("😰", "Estrés", "Estresado", Color(0xFFFFCC80)),
                    Quad("😴", "Cansada", "Cansado", Color(0xFFE2E8F0))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    moodOptions.forEach { (emoji, label, dbName, themeColor) ->
                        val isSelected = (currentLoggedMood == dbName) || (clickedMood == label)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) themeColor.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    clickedMood = label
                                    // Submit real mood recording to database!
                                    val stress = when (dbName) {
                                        "Tranquilo" -> 1
                                        "Feliz" -> 1
                                        "Estresado" -> 5
                                        else -> 2
                                    }
                                    val energy = when (dbName) {
                                        "Tranquilo" -> 3
                                        "Feliz" -> 5
                                        "Estresado" -> 4
                                        else -> 1
                                    }
                                    val motivation = when (dbName) {
                                        "Tranquilo" -> 4
                                        "Feliz" -> 5
                                        "Estresado" -> 3
                                        else -> 2
                                    }
                                    viewModel.submitMoodRecord(dbName, stress, energy, motivation, "Mood rápido desde Bento Grid")
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else BentoSlateMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. BENTO GRID MODULES
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Bento Large: Simulación Clínica (Tall)
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(BentoBlue)
                        .clickable { viewModel.navigateTo(AppScreen.CASES) }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎯", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Simulación Clínica",
                                color = BentoSlateDark,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 21.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Practica con casos clínicos virtuales, retroalimentación diagnóstica y TCC.",
                                color = BentoSlateDark.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }

                        // Continuing Practice White Card CTA Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continuar Práctica",
                                color = BentoSlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Bento Column: Stacked Library & Wellbeing
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Library card (Academic resources)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                            .clickable { viewModel.navigateTo(AppScreen.ACADEMY) }
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BentoMint.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📚", fontSize = 14.sp)
                                }
                                Text(
                                    text = "Biblioteca",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Guías de referencia, mapas conceptuales y TCC.",
                                color = BentoSlateLight,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Wellbeing card (Emotional wellness)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                            .clickable { viewModel.navigateTo(AppScreen.WELLBEING) }
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BentoRose.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌿", fontSize = 14.sp)
                                }
                                Text(
                                    text = "Bienestar",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Mindfulness, diario y bienestar emocional.",
                                color = BentoSlateLight,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. AI ASSISTANT PREMIUM FLOATING ROW
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { viewModel.navigateTo(AppScreen.ASSISTANT) },
                colors = CardDefaults.cardColors(containerColor = BentoSlateDark),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BentoBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tutor Inteligente IA",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "¿Tienes dudas? Pregunta a MindU Assistant...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Consultar",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. MOTIVATIONAL WISDOM CARD (BENTO STYLE)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BentoMint.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = randomPhrase,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// Small helper representations for mood setup
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun IndicatorStat(title: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun QuickCard(
    title: String,
    desc: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Text(desc, fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
        }
    }
}


// PANTALLA 2: ACADEMY SCREEN (BIBLIOTECA ACADÉMICA)
@Composable
fun AcademyScreen(viewModel: AppViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredResources by viewModel.filteredResources.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedResource by viewModel.selectedResource.collectAsStateWithLifecycle()
    val user by viewModel.userState.collectAsStateWithLifecycle()

    var showUploadDialog by remember { mutableStateOf(false) }

    if (selectedResource != null) {
        // Detail View
        ResourceDetailView(resource = selectedResource!!, viewModel = viewModel)
    } else {
        // List View
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title & Search
                Text(
                    text = "Apoyo Académico Colectivo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Biblioteca compartida por estudiantes de Psicología.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar síntesis, conceptos, TCC...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags for subjects & filters
                val categories = listOf("Todos", "Psicología Clínica", "Bases Neurobiológicas", "Psicología del Desarrollo", "Guía de estudio", "Resumen", "Mapa conceptual")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resource Feed
                if (filteredResources.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary.copy(0.3f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No encontramos recursos para esa búsqueda.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredResources) { rEntity ->
                            AcademicCard(
                                resource = rEntity,
                                onClick = { viewModel.viewResourceDetail(rEntity) },
                                onFavClick = { viewModel.toggleFavorite(rEntity) }
                            )
                        }
                    }
                }
            }

            // Floating action button for upload community shared note
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Subir Apunte")
            }
        }
    }

    if (showUploadDialog) {
        UploadNoteDialog(
            userAuthor = user?.name ?: "Usuario",
            onDismiss = { showUploadDialog = false },
            onUpload = { title, subject, type, summary, content ->
                viewModel.uploadSharedNote(title, subject, type, summary, content, user?.name ?: "Estudiante")
                showUploadDialog = false
            }
        )
    }
}

@Composable
fun AcademicCard(
    resource: AcademicResourceEntity,
    onClick: () -> Unit,
    onFavClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            resource.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        resource.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        resource.subject,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.8f)
                    )
                }

                IconButton(onClick = onFavClick) {
                    Icon(
                        imageVector = if (resource.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritos",
                        tint = if (resource.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                resource.description,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${String.format(Locale.US, "%.1f", resource.rating)} (${resource.ratingCount} valoraciones)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }

                Text(
                    "Por: ${resource.author}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        }
    }
}

@Composable
fun ResourceDetailView(
    resource: AcademicResourceEntity,
    viewModel: AppViewModel
) {
    val comments = remember(resource.commentsJson) {
        val list = mutableListOf<Triple<String, String, Long>>()
        try {
            val jsonArr = JSONArray(resource.commentsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    Triple(
                        obj.getString("username"),
                        obj.getString("text"),
                        obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    var myComment by remember { mutableStateOf("") }
    var scoreReview by remember { mutableStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.closeResourceDetail() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detalle del Recurso", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Badge & Meta
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(resource.type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(resource.title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(resource.subject, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("Publicado por: ${resource.author}", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 11.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Fracciones y Contenido Científico:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        
        // Full Document Canvas
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = resource.content,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Collaborative Learning Comment Section
        Text("Aprendizaje Colaborativo (${comments.size} Comentarios)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Add critique form
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("¿Qué opinas de este apunte? Califícalo:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            null,
                            tint = if (i <= scoreReview) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { scoreReview = i }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = myComment,
                    onValueChange = { myComment = it },
                    placeholder = { Text("Añade críticas de apoyo o complementos teóricos...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val reviewName = "Estudiante_" + (100..999).random()
                        viewModel.addResourceComment(resource.id, reviewName, myComment, scoreReview.toFloat())
                        myComment = ""
                    },
                    enabled = myComment.trim().isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Calificar y Ganar +15 XP")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        comments.forEach { (reviewer, text, time) ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .shadow(0.5.dp, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(reviewer, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        val datetimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(time))
                        Text(datetimeStr, fontSize = 10.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun UploadNoteDialog(
    userAuthor: String,
    onDismiss: () -> Unit,
    onUpload: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Psicología Clínica") }
    var type by remember { mutableStateOf("Resumen") }
    // subjects list
    val subjects = listOf("Psicología Clínica", "Bases Neurobiológicas", "Psicología del Desarrollo")
    // types list
    val types = listOf("Guía de estudio", "Resumen", "Mapa conceptual", "Artículo")
    
    var summary by remember { mutableStateOf("") }
    var textBody by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Compartir Conocimiento", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text("Ayuda a tus compañeros de Psicología de tu comunidad universitaria.", fontSize = 11.sp, color = Color.Gray)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Apunte") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdowns or simpler Rows for options
                Text("Asignatura / Materia:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { sub ->
                        val selected = subject == sub
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.3f))
                                .clickable { subject = sub }
                                .padding(6.dp)
                        ) {
                            Text(sub, color = if (selected) Color.White else Color.Black, fontSize = 10.sp)
                        }
                    }
                }

                Text("Formato del Recurso:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { t ->
                        val selected = type == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.3f))
                                .clickable { type = t }
                                .padding(6.dp)
                        ) {
                            Text(t, color = if (selected) Color.White else Color.Black, fontSize = 10.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Breve resumen") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = textBody,
                    onValueChange = { textBody = it },
                    label = { Text("Contenido completo") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = { onUpload(title, subject, type, summary, textBody) },
                        enabled = title.isNotBlank() && summary.isNotBlank() && textBody.isNotBlank()
                    ) {
                        Text("Subir y Ganar +30 XP")
                    }
                }
            }
        }
    }
}


// PANTALLA 3: BIENESTAR EMOCIONAL (DIARIO Y SEGUIDOR EMOCIONAL)
@Composable
fun WellbeingScreen(viewModel: AppViewModel) {
    val emotionalRecords by viewModel.emotionalRecords.collectAsStateWithLifecycle()
    val user by viewModel.userState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("registro") } // "registro", "diario", "respirar"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Wellbeing navigation headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bienestar Emocional",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Autocuidado mental guiado para el estudiante de Psicología.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.LightGray.copy(alpha = 0.15f))
                .padding(4.dp)
        ) {
            val tabs = listOf(
                Pair("registro", "Seguidor"),
                Pair("diario", "Diario"),
                Pair("respirar", "Respiración")
            )
            tabs.forEach { (id, title) ->
                val active = activeTab == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { activeTab = id }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content Router
        when (activeTab) {
            "registro" -> MoodTrackerModule(viewModel)
            "diario" -> EmotionalDiaryModule(viewModel)
            "respirar" -> BreathingModule()
        }
    }
}

@Composable
fun MoodTrackerModule(viewModel: AppViewModel) {
    val records by viewModel.emotionalRecords.collectAsStateWithLifecycle()

    var mood by remember { mutableStateOf("Tranquilo") }
    var stress by remember { mutableStateOf(3f) }
    var energy by remember { mutableStateOf(3f) }
    var motivation by remember { mutableStateOf(3f) }
    var notes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Quick mood record selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("¿Cómo te sientes en este punto académico?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val moodsMap = listOf(
                        Pair("Feliz", "🌸"),
                        Pair("Tranquilo", "🍃"),
                        Pair("Ansioso", "⚡"),
                        Pair("Estresado", "☄️"),
                        Pair("Cansado", "💤")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moodsMap.forEach { (m, emoji) ->
                            val isSelected = mood == m
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { mood = m }
                                    .padding(8.dp)
                            ) {
                                Text(emoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(m, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics sliders
                    MetricSlider("Nivel de estrés universitario", stress) { stress = it }
                    MetricSlider("Nivel de energía física", energy) { energy = it }
                    MetricSlider("Motivación para el estudio", motivation) { motivation = it }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Escribe notas cortas (ej. 'Muchos exámenes', 'Estudié con café')") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.submitMoodRecord(mood, stress.toInt(), energy.toInt(), motivation.toInt(), notes)
                            notes = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Calma y Ganar +20 XP")
                    }
                }
            }
        }

        // Custom Bezier Chart for Stress & Calm tracking! Fully native and visually glorious!
        item {
            Text("Estadística Semanal de Autocuidado", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Evolución de Estrés (Verde) y Motivación (Azul)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxPoints = 5
                            val numRecordsToShow = minOf(records.size, 6)
                            val chartList = if (numRecordsToShow > 0) {
                                records.take(numRecordsToShow).reversed()
                            } else {
                                // Default static mock placeholder if db empty, avoids blank space!
                                listOf(
                                    EmotionalRecordEntity(stressLevel = 4, motivationLevel = 2, mood = "Tranquilo", energyLevel = 3, note = ""),
                                    EmotionalRecordEntity(stressLevel = 3, motivationLevel = 3, mood = "Tranquilo", energyLevel = 3, note = ""),
                                    EmotionalRecordEntity(stressLevel = 5, motivationLevel = 2, mood = "Tranquilo", energyLevel = 3, note = ""),
                                    EmotionalRecordEntity(stressLevel = 2, motivationLevel = 4, mood = "Tranquilo", energyLevel = 3, note = ""),
                                    EmotionalRecordEntity(stressLevel = 3, motivationLevel = 4, mood = "Tranquilo", energyLevel = 3, note = "")
                                )
                            }

                            val stepX = size.width / (chartList.size - 1).coerceAtLeast(1)
                            
                            // Let's draw horizontal guidelines
                            val lines = 3
                            for (l in 1..lines) {
                                val y = (size.height / (lines + 1)) * l
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw stress path
                            val stressPath = Path()
                            chartList.forEachIndexed { idx, point ->
                                val x = idx * stepX
                                val y = size.height - (point.stressLevel.toFloat() / maxPoints.toFloat() * size.height)
                                if (idx == 0) {
                                    stressPath.moveTo(x, y)
                                } else {
                                    stressPath.lineTo(x, y)
                                }
                                drawCircle(
                                    color = Color(0xFF2E8B57), // SeaGreen
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(
                                path = stressPath,
                                color = Color(0xFF2E8B57),
                                style = Stroke(width = 4f)
                            )

                            // Draw motivation path
                            val motivationPath = Path()
                            chartList.forEachIndexed { idx, point ->
                                val x = idx * stepX
                                val y = size.height - (point.motivationLevel.toFloat() / maxPoints.toFloat() * size.height)
                                if (idx == 0) {
                                    motivationPath.moveTo(x, y)
                                } else {
                                    motivationPath.lineTo(x, y)
                                }
                                drawCircle(
                                    color = Color(0xFF2196F3), // Bright Blue
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(
                                path = motivationPath,
                                color = Color(0xFF2196F3),
                                style = Stroke(width = 4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Días Anteriores ➔", fontSize = 10.sp, color = Color.Gray)
                        Text("Hoy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Historial de estados de ánimo list cards
        item {
            Text("Historial de Registros", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aún no tienes registros de calma el día de hoy.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(records) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val emoji = when (rec.mood) {
                                "Feliz" -> "🌸"
                                "Tranquilo" -> "🍃"
                                "Ansioso" -> "⚡"
                                "Estresado" -> "☄️"
                                "Cansado" -> "💤"
                                else -> "😊"
                            }
                            Text(emoji, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sentirse " + rec.mood, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (rec.note.isNotEmpty()) {
                                Text(rec.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(rec.timestamp))
                            Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Estrés: ${rec.stressLevel}/5", fontSize = 11.sp)
                            Text("Energía: ${rec.energyLevel}/5", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun MetricSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value.toInt().toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EmotionalDiaryModule(viewModel: AppViewModel) {
    val entries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    var diaryTitle by remember { mutableStateOf("") }
    var diaryText by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Form to write diary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Espacio de Escritura Terapéutica ✍️", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Escribe reflexivamente. La app guardará este diario de forma encriptada y privada.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = diaryTitle,
                        onValueChange = { diaryTitle = it },
                        placeholder = { Text("Título de hoy") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = diaryText,
                        onValueChange = { diaryText = it },
                        placeholder = { Text("Escribe tus pensamientos internos, temores, logros...") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.submitDiaryEntry(diaryTitle, diaryText)
                            diaryTitle = ""
                            diaryText = ""
                        },
                        enabled = diaryTitle.isNotBlank() && diaryText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sellar Diario y Ganar +25 XP")
                    }
                }
            }
        }

        item {
            Text("Entradas anteriores", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aún no tienes reflexiones en tu diario privado.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(entries) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // Beautiful automated sentiment analysis helper
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (entry.sentiment) {
                                            "Optimista" -> Color(0xFFC8E6C9)
                                            "Melancólico" -> Color(0xFFD1C4E9)
                                            "Ansioso" -> Color(0xFFFFCC80)
                                            else -> Color(0xFFB3E5FC)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    entry.sentiment, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = when (entry.sentiment) {
                                        "Optimista" -> Color(0xFF1B5E20)
                                        "Melancólico" -> Color(0xFF4A148C)
                                        "Ansioso" -> Color(0xFFE65100)
                                        else -> Color(0xFF01579B)
                                    }
                                )
                            }
                        }

                        val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                        Text(timeFormatted, fontSize = 11.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(entry.text, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun BreathingModule() {
    var isBreathingActive by remember { mutableStateOf(false) }
    var cycleText by remember { mutableStateOf("Respira a tu ritmo") }
    var currentCycleSeconds by remember { mutableStateOf(0) }
    var targetScale by remember { mutableStateOf(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "PulseEffect")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    val customAnimScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = when (cycleText) {
                "Inhala..." -> 4000
                "Sostén..." -> 2000
                else -> 6000
            },
            easing = LinearOutSlowInEasing
        ),
        label = "BreathingScale"
    )

    LaunchedEffect(isBreathingActive, cycleText) {
        if (isBreathingActive) {
            when (cycleText) {
                "Inhala..." -> {
                    targetScale = 1.9f
                    delay(4000)
                    cycleText = "Sostén..."
                }
                "Sostén..." -> {
                    targetScale = 1.9f
                    delay(2000)
                    cycleText = "Exhala..."
                }
                "Exhala..." -> {
                    targetScale = 1f
                    delay(6000)
                    cycleText = "Inhala..."
                }
                else -> {
                    cycleText = "Inhala..."
                }
            }
        } else {
            targetScale = 1.0f
            cycleText = "Toma asiento y prepárate"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Pacer de Respiración Diafragmática Coherente", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp, 
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Método científico de estimulación vagal parasimpática: Inhalación profunda abdominal (4s), mantener (2s), y exhalación completa (6s).",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large concentric breathing orb
        Box(
            modifier = Modifier
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background ambient aura circle
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        val zoom = if (isBreathingActive) customAnimScale else pulseScale
                        drawCircle(
                            color = Color(0xFFA7D8F0).copy(alpha = 0.15f),
                            radius = size.minDimension / 2 * zoom
                        )
                    }
            )

            // Primary beautiful breathing central dot
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .drawBehind {
                        val zoom = if (isBreathingActive) customAnimScale else 1.0f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFB7E4C7), Color(0xFF2E8B57))
                            ),
                            radius = size.minDimension / 2 * zoom
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBreathingActive) cycleText else "Presiona\nInicio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                isBreathingActive = !isBreathingActive
                if (isBreathingActive) cycleText = "Inhala..."
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBreathingActive) Color.Red.copy(0.8f) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
        ) {
            Text(if (isBreathingActive) "Detener Guía" else "Iniciar Respiración diafragmática")
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}


// PANTALLA 4: CLINICAL CASES SIMULATOR (SIMULACIONES DE CASOS)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CasesScreen(viewModel: AppViewModel) {
    val casesList by viewModel.clinicalCases.collectAsStateWithLifecycle()
    val activeCase by viewModel.activeCase.collectAsStateWithLifecycle()
    val historyLog by viewModel.simulationHistory.collectAsStateWithLifecycle()

    if (activeCase != null) {
        // Active interactive case player
        CaseSimulationPlayer(activeCase = activeCase!!, viewModel = viewModel)
    } else {
        // List of cases & historical records
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = "Gabinete Clínico Virtual",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Práctica diagnóstica asistida con fines puramente educativos.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Case cards
            item {
                Text("Casos de Diagnóstico Disponibles", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (casesList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(casesList) { cCase ->
                    // Find if user already answered this case in history to add checked tick
                    val isCompleted = historyLog.any { it.caseId == cCase.id }
                    val maxScore = historyLog.filter { it.caseId == cCase.id }.maxOfOrNull { it.score }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.5.dp, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Row tags
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (cCase.difficulty) {
                                                        "Básico" -> Color(0xFFC8E6C9)
                                                        "Intermedio" -> Color(0xFFFFE082)
                                                        else -> Color(0xFFFFCDD2)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                cCase.difficulty,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (cCase.difficulty) {
                                                    "Básico" -> Color(0xFF1B5E20)
                                                    "Intermedio" -> Color(0xFF7F5F00)
                                                    else -> Color(0xFFB71C1C)
                                                }
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(cCase.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(cCase.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Paciente: ${cCase.patientName} (${cCase.patientAge} años)", fontSize = 12.sp, color = Color.Gray)
                                }

                                if (isCompleted) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Icon(Icons.Default.CheckCircle, "Completed Cases Check", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                        Text("Score: $maxScore%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                cCase.caseSummary,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.startCaseSimulation(cCase) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isCompleted) "Re-entrenar caso clínico" else "Iniciar Simulación Clínica")
                            }
                        }
                    }
                }
            }

            // Historical completions log
            item {
                Text("Tu Historial Clínico de Competencias", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (historyLog.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ningún caso resuelto todavía. ¡Inicia con un caso básico!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                items(historyLog) { log ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.caseTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Puntuación: ${log.score}%", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (log.score >= 80) Color(0xFF2E8B57) else Color.Red)
                            }
                            val logTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                            Text("Completado: $logTime | Nivel: ${log.difficulty}", fontSize = 11.sp, color = Color.Gray)
                            if (log.frequentErrors.isNotEmpty() && log.frequentErrors != "Ninguno (Excelente juicio clínico)") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Correcciones: ${log.frequentErrors}", fontSize = 10.sp, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseSimulationPlayer(
    activeCase: ClinicalCaseEntity,
    viewModel: AppViewModel
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsStateWithLifecycle()
    val userScore by viewModel.userScore.collectAsStateWithLifecycle()
    val feedbackText by viewModel.stepFeedback.collectAsStateWithLifecycle()
    val isCompleted by viewModel.simulationCompleted.collectAsStateWithLifecycle()

    val stepsArr = remember(activeCase.stepsJson) { JSONArray(activeCase.stepsJson) }
    val isCurrentStepValid = currentStepIndex < stepsArr.length()

    val symptomsList = remember(activeCase.symptomsJson) {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(activeCase.symptomsJson)
            for (i in 0 until arr.length()) list.add(arr.getString(i))
        } catch (e: Exception) { e.printStackTrace() }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.finishCaseSimulation() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulador de Casos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (isCompleted) {
            // Results overlay card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Ψ SIMULACIÓN COMPLETADA", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$userScore%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }

                    Text(
                        text = when {
                            userScore >= 90 -> "¡Impecable juicio clínico! Demuestras competencias excelentes para conceptualizar patologías psicoterapéuticas."
                            userScore >= 70 -> "Buen desempeño clínico de apoyo. Corrige pequeños errores conceptuales repasando las guías académicas."
                            else -> "Práctica crítica necesaria. Te recomendamos estudiar minuciosamente la TCC antes de repetir la simulación."
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )

                    Text(
                        "IMPORTANTE: Las simulaciones en MindU tienen fines orientativos pedagógicos/educativos académicos exclusivamente y no sustituyen la práctica clínica profesional supervisada de un psicólogo titulado.",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Light,
                        color = Color.Gray,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Button(
                        onClick = { viewModel.finishCaseSimulation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finalizar y Registrar Competencias")
                    }
                }
            }
        } else if (isCurrentStepValid) {
            val stepObj = stepsArr.getJSONObject(currentStepIndex)
            val stepTitle = stepObj.getString("title")
            val instructionText = stepObj.getString("instruction")
            
            val optionsList = remember(currentStepIndex) {
                val list = mutableListOf<String>()
                val optArr = stepObj.getJSONArray("options")
                for (i in 0 until optArr.length()) list.add(optArr.getString(i))
                list
            }

            // Overview patient board
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paciente: ${activeCase.patientName}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Anamnesis e Historial clínico:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(activeCase.clinicalHistory, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Síntomas Identificados:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        symptomsList.forEach { s ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(0.08f))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(s, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Step Progress pacer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Desafío ${currentStepIndex + 1} de ${stepsArr.length()}: $stepTitle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Score: $userScore Pts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Linear tracking bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until stepsArr.length()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    i < currentStepIndex -> MaterialTheme.colorScheme.primary
                                    i == currentStepIndex -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else -> Color.LightGray.copy(0.3f)
                                }
                            )
                    )
                }
            }

            // Step Question Board
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.02f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(instructionText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    // Options buttons
                    optionsList.forEachIndexed { opIdx, textOption ->
                        val selected = selectedOptionIndex == opIdx
                        val isCorrect = opIdx == stepObj.getInt("correctIndex")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        selected && isCorrect -> Color(0xFFC8E6C9)
                                        selected && !isCorrect -> Color(0xFFFFCDD2)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        selected && isCorrect -> Color(0xFF388E3C)
                                        selected && !isCorrect -> Color(0xFFD32F2F)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(0.12f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (selectedOptionIndex == null) viewModel.selectSimulationOption(opIdx)
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${'A' + opIdx}.  ",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    textOption,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Feedback box text
                    if (feedbackText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Text(feedbackText!!, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Next button
                        Button(
                            onClick = { viewModel.nextSimulationStep() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(if (currentStepIndex == stepsArr.length() - 1) "Finalizar Diagnóstico" else "Confirmar y Continuar")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}


// PANTALLA 5: ASSISTANT CHAT SCREEN (MINDU AI ASSISTANT)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(viewModel: AppViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }

    val preloadedSuggestions = listOf(
        "¿Qué es la TCC?",
        "Técnica para el estrés",
        "Estilos de apego de Bowlby",
        "¿Qué es la anhedonia?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Chat header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C784))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MindU Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text("Tutor de psicología clínica y académica 24/7", fontSize = 11.sp, color = Color.Gray)
            }

            IconButton(onClick = { viewModel.clearChatHistory() }) {
                Icon(Icons.Default.Clear, "Vaciar Conversación", tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chats lists
        LazyColumn(
            reverseLayout = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            "¡Hola! Te doy la bienvenida al tutor inteligente de MindU.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Puedes resolver dudas sobre asignaturas, términos médicos del DSM-5, o consultar ejercicios contra el estrés de tu carrera en psicología.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Suggestion Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            preloadedSuggestions.forEach { sugg ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { viewModel.sendMessageToAssistant(sugg) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(sugg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            } else {
                items(messages) { msg ->
                    ChatBubble(role = msg.role, text = msg.text)
                }
            }

            if (isChatLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("MindU Assistant está analizando...", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Input Tray Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Escribe una duda de Psicología...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            FloatingActionButton(
                onClick = {
                    if (inputPrompt.trim().isNotEmpty()) {
                        viewModel.sendMessageToAssistant(inputPrompt)
                        inputPrompt = ""
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Send, "Enviar", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(role: String, text: String) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary else Color.LightGray.copy(0.2f)
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "Tú" else "MindU Assistant",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (isUser) Color.White.copy(0.8f) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


// PANTALLA 6: USER PROFILE AND SETTINGS SCREEN (PERFIL)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val badges by viewModel.unlockedBadges.collectAsStateWithLifecycle()
    val casesHistory by viewModel.simulationHistory.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                "Perfil Universitario",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Beautiful student badge ID card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.5.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎓", fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(user?.name ?: "Estudiante de Psicología", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(user?.university ?: "Universidad Central", fontSize = 12.sp, color = Color.Gray)
                            Text("${user?.semester ?: 1}° Semestre académico", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rango Actual", fontSize = 11.sp, color = Color.Gray)
                            Text("Clínico Nivel " + (user?.level ?: 1), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.1f)),
                            elevation = null
                        ) {
                            Text("Editar Perfil", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Config section / settings
        item {
            Text("Preferencias y Configuración", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsSwitchRow(
                        label = "Modo Oscuro",
                        checked = user?.isDarkMode ?: false,
                        onCheckChanged = { darkVal ->
                            viewModel.updateUser(
                                user?.name ?: "",
                                user?.university ?: "",
                                user?.semester ?: 1,
                                isDarkMode = darkVal,
                                notificationsEnabled = user?.notificationsEnabled ?: true
                            )
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        label = "Notificaciones Académicas",
                        checked = user?.notificationsEnabled ?: true,
                        onCheckChanged = { notVal ->
                            viewModel.updateUser(
                                user?.name ?: "",
                                user?.university ?: "",
                                user?.semester ?: 1,
                                isDarkMode = user?.isDarkMode ?: false,
                                notificationsEnabled = notVal
                            )
                        }
                    )
                }
            }
        }

        // Gamification: Insignias de Psicología (Badges achievements)
        item {
            Text("Tus Logros Académicos y Profesionalismo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (badges.isEmpty()) {
            item {
                Text("Todavía no desbloqueas logros.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            items(badges) { bdg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD54F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏆", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(bdg.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(bdg.description, fontSize = 11.sp, color = Color.Gray)
                            Text("+${bdg.xpReward} XP Recompensa", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = user?.name ?: "",
            currentUniversity = user?.university ?: "",
            currentSemester = user?.semester ?: 1,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, uni, sem ->
                viewModel.updateUser(name, uni, sem, user?.isDarkMode ?: false, user?.notificationsEnabled ?: true)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun SettingsSwitchRow(label: String, checked: Boolean, onCheckChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckChanged)
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentUniversity: String,
    currentSemester: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var university by remember { mutableStateOf(currentUniversity) }
    var semesterString by remember { mutableStateOf(currentSemester.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Refinar Credenciales Estudiantiles", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("Universidad / Instituto") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = semesterString,
                    onValueChange = { semesterString = it },
                    label = { Text("Semestre en Curso") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val semInt = semesterString.toIntOrNull() ?: 1
                            onSave(name, university, semInt)
                        },
                        enabled = name.isNotBlank() && university.isNotBlank()
                    ) {
                        Text("Guardar Ajustes")
                    }
                }
            }
        }
    }
}
