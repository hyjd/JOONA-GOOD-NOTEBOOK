package com.lockscreennotepad

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// Data Classes
// ============================================================
data class Note(
    val id: String = UUID.randomUUID().toString(),
    var content: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isLocked: Boolean = false,
    var backgroundColor: Long = 0xFFFFF9C4,
    var order: Int = 0
)

data class AppSettings(
    var fontSize: Float = 16f,
    var noteSpacing: Float = 8f,
    var textAlign: String = "left"
)

// ============================================================
// Color Palette
// ============================================================
val noteColors = listOf(
    0xFFFFF9C4, // Yellow
    0xFFFFCCBC, // Orange
    0xFFC8E6C9, // Green
    0xFFBBDEFB, // Blue
    0xFFE1BEE7, // Purple
    0xFFF8BBD0, // Pink
    0xFFFFFFFF, // White
    0xFFD7CCC8  // Brown
)

// ============================================================
// MainActivity
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = dynamicLightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LockscreenNotepadApp()
                }
            }
        }
    }
}

@Composable
fun dynamicLightColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFF625B71),
        surface = Color(0xFFFFFBFE),
        background = Color(0xFFF6F2FA)
    )
}

// ============================================================
// Main App Composable
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockscreenNotepadApp() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var settings by remember { mutableStateOf(loadSettings(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredNotes = if (searchQuery.isBlank()) {
        notes.sortedBy { it.order }
    } else {
        notes.filter { it.content.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.order }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("메모 검색...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "📝 준아 메모장",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "검색"
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "메모 추가", tint = Color.White)
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "메모가 없습니다\n+ 버튼을 눌러 메모를 추가하세요",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(settings.noteSpacing.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(filteredNotes, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        settings = settings,
                        onEdit = { editingNote = it },
                        onDelete = { showDeleteConfirm = it },
                        onToggleLock = {
                            notes = notes.map { n ->
                                if (n.id == it.id) n.copy(isLocked = !n.isLocked) else n
                            }
                            saveNotes(context, notes)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Add Note Dialog
    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { content, color ->
                val newNote = Note(
                    content = content,
                    backgroundColor = color,
                    order = notes.size
                )
                notes = notes + newNote
                saveNotes(context, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Note Dialog
    editingNote?.let { note ->
        if (!note.isLocked) {
            EditNoteDialog(
                note = note,
                onDismiss = { editingNote = null },
                onConfirm = { updatedContent, updatedColor ->
                    notes = notes.map {
                        if (it.id == note.id) it.copy(
                            content = updatedContent,
                            backgroundColor = updatedColor,
                            timestamp = System.currentTimeMillis()
                        ) else it
                    }
                    saveNotes(context, notes)
                    editingNote = null
                }
            )
        } else {
            editingNote = null
        }
    }

    // Delete Confirm Dialog
    showDeleteConfirm?.let { note ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("메모 삭제") },
            text = { Text("이 메모를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    notes = notes.filter { it.id != note.id }
                    saveNotes(context, notes)
                    showDeleteConfirm = null
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("취소")
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettingsDialog = false },
            onConfirm = {
                settings = it
                saveSettings(context, it)
                showSettingsDialog = false
            }
        )
    }
}

// ============================================================
// NoteItem Composable
// ============================================================
@Composable
fun NoteItem(
    note: Note,
    settings: AppSettings,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onToggleLock: (Note) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = Color(note.backgroundColor), label = "bg"
    )
    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(note) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(note.timestamp)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Row {
                    IconButton(
                        onClick = { onToggleLock(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "잠금",
                            modifier = Modifier.size(18.dp),
                            tint = if (note.isLocked) Color(0xFFE53935) else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { onDelete(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (note.isLocked) "🔒 잠긴 메모입니다" else note.content,
                fontSize = settings.fontSize.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                textAlign = when (settings.textAlign) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.End
                    else -> TextAlign.Start
                },
                modifier = Modifier.fillMaxWidth(),
                color = if (note.isLocked) Color.Gray else Color.Black
            )
        }
    }
}

// ============================================================
// AddNoteDialog
// ============================================================
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(noteColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ 새 메모", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("메모를 입력하세요...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("배경 색상", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    noteColors.forEach { color ->
                        ColorButton(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (content.isNotBlank()) onConfirm(content, selectedColor) }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ============================================================
// EditNoteDialog
// ============================================================
@Composable
fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var content by remember { mutableStateOf(note.content) }
    var selectedColor by remember { mutableStateOf(note.backgroundColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📝 메모 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("배경 색상", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    noteColors.forEach { color ->
                        ColorButton(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (content.isNotBlank()) onConfirm(content, selectedColor) }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ============================================================
// SettingsDialog
// ============================================================
@Composable
fun SettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (AppSettings) -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(settings.fontSize) }
    var noteSpacing by remember { mutableFloatStateOf(settings.noteSpacing) }
    var textAlign by remember { mutableStateOf(settings.textAlign) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ 설정", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("글자 크기: ${fontSize.toInt()}sp")
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..24f,
                    steps = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("메모 간격: ${noteSpacing.toInt()}dp")
                Slider(
                    value = noteSpacing,
                    onValueChange = { noteSpacing = it },
                    valueRange = 4f..20f,
                    steps = 7
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("텍스트 정렬")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    listOf("left" to "왼쪽", "center" to "가운데", "right" to "오른쪽").forEach { (align, label) ->
                        FilterChip(
                            selected = textAlign == align,
                            onClick = { textAlign = align },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(AppSettings(fontSize, noteSpacing, textAlign))
            }) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ============================================================
// ColorButton
// ============================================================
@Composable
fun ColorButton(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "선택됨",
                modifier = Modifier.size(16.dp),
                tint = Color.DarkGray
            )
        }
    }
}

// ============================================================
// SharedPreferences - Notes
// ============================================================
fun loadNotes(context: Context): List<Note> {
    val prefs = context.getSharedPreferences("notepad", Context.MODE_PRIVATE)
    val json = prefs.getString("notes", null) ?: return emptyList()
    return try {
        val type = object : TypeToken<List<Note>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveNotes(context: Context, notes: List<Note>) {
    val prefs = context.getSharedPreferences("notepad", Context.MODE_PRIVATE)
    prefs.edit().putString("notes", Gson().toJson(notes)).apply()
}

// ============================================================
// SharedPreferences - Settings
// ============================================================
fun loadSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("notepad", Context.MODE_PRIVATE)
    val json = prefs.getString("settings", null) ?: return AppSettings()
    return try {
        Gson().fromJson(json, AppSettings::class.java)
    } catch (e: Exception) {
        AppSettings()
    }
}

fun saveSettings(context: Context, settings: AppSettings) {
    val prefs = context.getSharedPreferences("notepad", Context.MODE_PRIVATE)
    prefs.edit().putString("settings", Gson().toJson(settings)).apply()
}
