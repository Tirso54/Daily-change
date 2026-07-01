package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ChallengeRepository
import com.example.data.DbCompletedChallenge
import com.example.data.UserPreferences
import com.example.domain.ChallengeManager
import com.example.model.ChallengeCategory
import com.example.ui.ChallengeViewModel
import com.example.ui.SettingsScreen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val challengeManager = ChallengeManager(applicationContext)
        val repository = ChallengeRepository(database.challengeDao(), challengeManager)
        val userPreferences = UserPreferences(applicationContext)
        val viewModelFactory = ChallengeViewModel.Factory(repository, userPreferences)

        setContent {
            val viewModel: ChallengeViewModel by viewModels { viewModelFactory }
            val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
            
            val darkTheme = when (themeSetting) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            DailyChallengeScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    viewModel: ChallengeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val history by viewModel.completedHistory.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    
    var showResetDialog by remember { mutableStateOf(false) }
    val todayDateFormatted = remember { getFormattedDateDisplay() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Daily Challenge",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = todayDateFormatted,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val state = appState
            if (state == null) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                
                ChallengeSection(
                    isCompletedToday = state.isCompletedToday,
                    categoryName = state.currentChallengeCategory,
                    challengeText = state.currentChallengeText,
                    isGenerating = isGenerating,
                    onComplete = { viewModel.completeChallenge() },
                    onGenerateNew = { viewModel.generateNewChallenge() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                StreakAndBadgesSection(streakCount = state.currentStreak)

                Spacer(modifier = Modifier.height(24.dp))

                HistorySection(
                    historyList = history,
                    onResetClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Restaura el Progreso") },
            text = { Text(text = "¿Estás seguro de que deseas eliminar tu historial de retos completados y reiniciar tu racha a cero?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllProgress()
                        showResetDialog = false
                    }
                ) {
                    Text(text = "Reiniciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChallengeSection(
    isCompletedToday: Boolean,
    categoryName: String,
    challengeText: String,
    isGenerating: Boolean,
    onComplete: () -> Unit,
    onGenerateNew: () -> Unit
) {
    val categoryColor = getCategoryColor(categoryName)
    val categoryLabel = getCategoryLabel(categoryName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("challenge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.5.dp, if (isCompletedToday) Emerald400 else categoryColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("category_chip"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryLabel.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = categoryColor)
            } else {
                Text(
                    text = challengeText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedContent(
                targetState = isCompletedToday,
                transitionSpec = {
                    scaleIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(100))
                },
                label = "StatusBadge"
            ) { completed ->
                if (completed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completado exitosamente",
                            tint = Emerald400,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¡Reto Completado! 🎉",
                            color = Emerald400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Pendiente de completar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reto diario pendiente",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onComplete,
                enabled = !isCompletedToday && !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald400,
                    contentColor = Slate900,
                    disabledContainerColor = Slate800.copy(alpha = 0.5f),
                    disabledContentColor = Slate300.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_button"),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isCompletedToday) "Completado" else "Marcar como completado ✔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGenerateNew,
                enabled = !isGenerating,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Generar nuevo reto", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StreakAndBadgesSection(streakCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("streak_badge"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(
                        if (streakCount > 0) Amber400.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (streakCount > 0) "🔥" else "🥶", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (streakCount == 1) "1 día completado" else "$streakCount días seguidos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = if (streakCount > 0) Amber400 else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getStreakMotivation(streakCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Badges
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BadgeIcon("7 Días", streakCount >= 7, "🥉")
                BadgeIcon("30 Días", streakCount >= 30, "🥈")
                BadgeIcon("100 Días", streakCount >= 100, "🏆")
            }
        }
    }
}

@Composable
fun BadgeIcon(label: String, isUnlocked: Boolean, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) Sky400.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUnlocked) icon else "🔒",
                fontSize = 18.sp,
                modifier = Modifier.scale(if (isUnlocked) 1.2f else 1f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnlocked) Sky400 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}


@Composable
fun HistorySection(
    historyList: List<DbCompletedChallenge>,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().testTag("history_list")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Historial de Logros",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${historyList.size} retos",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onResetClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar historial", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Star, contentDescription = "Estrella", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aún no has completado retos", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
                    HistoryItemRow(item = item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: DbCompletedChallenge) {
    val categoryColor = getCategoryColor(item.category)
    val displayCategory = getCategoryLabel(item.category)
    val formattedDate = remember(item.completedDate) { formatHistoryDate(item.completedDate) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(categoryColor))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.challengeText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = displayCategory, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                    Text(text = formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

fun getStreakMotivation(streakCount: Int): String {
    return when {
        streakCount <= 0 -> "Completa el reto de hoy para arrancar tu racha diaria."
        streakCount == 1 -> "¡Arrancaste con fuerza! Termina el de mañana para seguir."
        streakCount in 2..4 -> "¡Excelente racha! Mantén viva la constancia."
        streakCount in 5..9 -> "¡Alucinante! Tu constancia se está volviendo un hábito saludable."
        else -> "¡Increíble nivel de compromiso! Eres imparable."
    }
}

fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.uppercase(Locale.getDefault())) {
        "FITNESS" -> CategorySalud
        "PRODUCTIVITY" -> CategoryProd
        "CREATIVITY" -> CategoryApren
        "MINDFULNESS" -> CategoryMent
        else -> Sky400
    }
}

fun getCategoryLabel(categoryName: String): String {
    return try {
        ChallengeCategory.valueOf(categoryName).displayName
    } catch (e: Exception) {
        categoryName
    }
}

fun getFormattedDateDisplay(): String {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    return sdf.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun formatHistoryDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(dateStr) ?: return dateStr
        val formatter = SimpleDateFormat("d 'de' MMM", Locale("es", "ES"))
        formatter.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
