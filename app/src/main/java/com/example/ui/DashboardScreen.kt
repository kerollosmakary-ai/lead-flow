package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.Lead
import com.example.util.AudioRecorder
import java.io.File
import kotlinx.coroutines.delay

// --- Elegant Cosmic luxury Slate color tokens ---
val SpaceDark = Color(0xFF0B0D14)
val CardSlate = Color(0xFF131722)
val AccentGreen = Color(0xFF10B981)
val WarmAmethyst = Color(0xFF8B5CF6)
val ElectricCrimson = Color(0xFFEC4899)
val CosmicGold = Color(0xFFFBBF24)
val GrayBorder = Color(0xFF242938)
val SubtitleGray = Color(0xFF8F9BB3)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DashboardScreen(viewModel: LeadViewModel) {
    val leads by viewModel.leads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val approvedClientName by viewModel.approvedClientName.collectAsState()
    val isClientApproved by viewModel.isClientApproved.collectAsState()
    val pendingStructuredLead by viewModel.pendingStructuredLead.collectAsState()
    val pendingRawNoteText by viewModel.pendingRawNoteText.collectAsState()
    val pendingSource by viewModel.pendingSource.collectAsState()

    val oracleQuery by viewModel.oracleQuery.collectAsState()
    val oracleResponse by viewModel.oracleResponse.collectAsState()
    val isOracleLoading by viewModel.isOracleLoading.collectAsState()

    var targetClientNameInput by remember { mutableStateOf("") }
    var localOracleQueryInput by remember { mutableStateOf("") }
    var simpleTextEntryInput by remember { mutableStateOf("") }

    // Filter leads matching the query
    val filteredLeads = remember(searchQuery, leads) {
        if (searchQuery.isBlank()) {
            leads
        } else {
            leads.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Determine if we need to offer the "New + Auto-Log" action
    val exactMatchExists = remember(searchQuery, leads) {
        searchQuery.isBlank() || leads.any { it.name.equals(searchQuery.trim(), ignoreCase = true) }
    }

    var selectedLeadId by remember { mutableStateOf<Int?>(null) }
    val activeLead = remember(filteredLeads, selectedLeadId) {
        filteredLeads.find { it.id == selectedLeadId } ?: filteredLeads.firstOrNull()
    }

    // Handle initial suggestion on selection
    LaunchedEffect(activeLead) {
        if (activeLead != null) {
            targetClientNameInput = activeLead.name
        }
    }

    // Audio Note Capturing States and Setup
    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordDurationSeconds by remember { mutableStateOf(0) }
    val isProcessing by viewModel.isProcessing.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.showStatus("Microphone access authorized!")
        } else {
            viewModel.showStatus("Permission denied. Set mic permissions in settings.")
        }
    }

    // Timer ticking during active dictation
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDurationSeconds = 0
            while (isRecording) {
                delay(1000)
                recordDurationSeconds++
            }
        }
    }

    val formattedTime = remember(recordDurationSeconds) {
        val mins = recordDurationSeconds / 60
        val secs = recordDurationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    var transcriptionResultText by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.cleanup()
        }
    }

    var intakeMode by remember { mutableStateOf(0) } // 0 = Voice, 1 = Raw Unstructured Text

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
    ) {
        // Space / Telemetry Ambient Lights
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(WarmAmethyst.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.2f),
                            radius = size.minDimension * 0.7f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricCrimson.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.8f),
                            radius = size.minDimension * 0.6f
                        )
                    )
                }
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Bricks Realty Logo",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BRICKS REALTY",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                            }
                            Text(
                                text = "Behavior Telemetry & Lead Workspace",
                                fontSize = 11.sp,
                                color = SubtitleGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Compact Offline / Online Security Shield Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isOnline) Color(0xFF1E293B) else Color(0xFF381A1A))
                                .clickable { viewModel.toggleOnlineMode() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) AccentGreen else ElectricCrimson)
                            )
                            Text(
                                text = if (isOnline) "SECURE SYNC" else "LOCAL ONLY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Top Search / Input bar ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search telemetry or enter a name...",
                            color = SubtitleGray.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = SubtitleGray
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = SubtitleGray
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSlate,
                        unfocusedContainerColor = CardSlate,
                        focusedBorderColor = WarmAmethyst,
                        unfocusedBorderColor = GrayBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                // --- Behavior Workspace List ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("leads_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    // Scenario: Search query is active but lead does not exist yet -> show "New" status option to auto-log
                    if (searchQuery.isNotBlank() && !exactMatchExists) {
                        item {
                            NewLeadAutoLogNode(
                                query = searchQuery.trim(),
                                onAutoLog = {
                                    viewModel.autoLogNewLead(searchQuery)
                                    viewModel.setSearchQuery("") // Clear query so user can view it in main list
                                }
                            )
                        }
                    }

                    if (filteredLeads.isEmpty()) {
                        item {
                            EmptyRegistryBanner()
                        }
                    } else {
                        items(filteredLeads, key = { it.id }) { lead ->
                            BehaviorNodeCard(
                                lead = lead,
                                isSelected = (activeLead?.id == lead.id),
                                onSelect = { selectedLeadId = lead.id },
                                onDelete = { viewModel.deleteLead(lead) },
                                onSync = {
                                    if (isOnline) {
                                        viewModel.triggerSync()
                                    } else {
                                        viewModel.showStatus("System offline. Sync is queued.")
                                    }
                                }
                            )
                        }
                    }
                }

                // --- Global CRM Analytics AI Oracle Dashboard ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WarmAmethyst.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(Color(0xFF131722), Color(0xFF0F111A))))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Oracle Analytics icon",
                                    tint = CosmicGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "GLOBAL ANALYTICS & AI ORACLE (اسم العميل غير مطلوب)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CosmicGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DASHBOARD WIDE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CosmicGold
                                )
                            }
                        }

                        Text(
                            text = "Analyze hotspots, trends, requested property categories, or average budgets across all recorded leads in the CRM database without targeting a specific folder.",
                            color = SubtitleGray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = localOracleQueryInput,
                            onValueChange = { localOracleQueryInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g., ما هي أكثر العقارات المطلوبة؟ / Summarize average budgets...",
                                    fontSize = 12.sp,
                                    color = SubtitleGray.copy(alpha = 0.5f)
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SpaceDark,
                                unfocusedContainerColor = SpaceDark,
                                focusedBorderColor = CosmicGold,
                                unfocusedBorderColor = GrayBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("oracle_query_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (localOracleQueryInput.isNotBlank()) {
                                        viewModel.askGlobalOracle(localOracleQueryInput)
                                    }
                                },
                                enabled = !isOracleLoading && localOracleQueryInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicGold,
                                    disabledContainerColor = GrayBorder,
                                    contentColor = SpaceDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("query_oracle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isOracleLoading) {
                                        CircularProgressIndicator(color = SpaceDark, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Query Trends", modifier = Modifier.size(14.dp))
                                    }
                                    Text("Query Dynamic Trends", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (oracleResponse != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearOracle()
                                        localOracleQueryInput = ""
                                    },
                                    border = BorderStroke(1.dp, GrayBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }

                        if (oracleResponse != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SpaceDark, RoundedCornerShape(10.dp))
                                    .border(1.dp, GrayBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "LIVE CRM ANALYTICS DIAGNOSTICS:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CosmicGold
                                    )
                                    Text(
                                        text = oracleResponse ?: "",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                // --- CRM Client Folder & Ingestion Core (With Twin-Approval Protocol) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isClientApproved) AccentGreen.copy(alpha = 0.6f) else WarmAmethyst.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Twin Approval Secure icon",
                                    tint = if (isClientApproved) AccentGreen else WarmAmethyst,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TWIN-APPROVAL SECURE CRM INTAKE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isClientApproved) AccentGreen.copy(alpha = 0.15f) else WarmAmethyst.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isClientApproved) "PHASE 2: CONTENT" else "PHASE 1: CLIENT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isClientApproved) AccentGreen else WarmAmethyst
                                )
                            }
                        }

                        // PHASE 1: Client Gateway / Approval Gate (مرحلة ابروف العميل)
                        if (!isClientApproved) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "مرحلة اعتماد العميل أولاً: اكتب اسم العميل أو اضغط على أي عميل في القائمة لتفعيل إمكانية المراجعة والتسجيل.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "To enter any notes or record voice dictation, you MUST first verify and approve the client's file identity.",
                                    color = SubtitleGray,
                                    fontSize = 11.sp
                                )

                                OutlinedTextField(
                                    value = targetClientNameInput,
                                    onValueChange = { targetClientNameInput = it },
                                    placeholder = {
                                        Text(
                                            text = "e.g., Sarah Connor / ياسمين كامل...",
                                            fontSize = 12.sp,
                                            color = SubtitleGray.copy(alpha = 0.5f)
                                        )
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SpaceDark,
                                        unfocusedContainerColor = SpaceDark,
                                        focusedBorderColor = WarmAmethyst,
                                        unfocusedBorderColor = GrayBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("approval_client_name_input")
                                )

                                Button(
                                    onClick = {
                                        if (targetClientNameInput.isNotBlank()) {
                                            viewModel.approveClient(targetClientNameInput)
                                        }
                                    },
                                    enabled = targetClientNameInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarmAmethyst,
                                        disabledContainerColor = GrayBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("approve_client_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Approve icon", modifier = Modifier.size(14.dp))
                                        Text("Approve Client Profile & Proceed (اعتماد الاسم)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // PHASE 2 UNLOCKED: Approved Client Identity Info Ribbon & Content Recording
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Green verification ribbon banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AccentGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Approved", tint = AccentGreen, modifier = Modifier.size(16.dp))
                                        Column {
                                            Text(
                                                text = "CLIENT IDENTITY APPROVED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AccentGreen
                                            )
                                            Text(
                                                text = approvedClientName?.uppercase() ?: "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.revokeClientApproval() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear approval", tint = ElectricCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (pendingStructuredLead == null) {
                                    // Custom visual tab selector for Voice vs. Unstructured Text Ingestion Mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F111A), RoundedCornerShape(10.dp))
                                            .padding(3.dp)
                                    ) {
                                        // Voice Dictation Mode Tab
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (intakeMode == 0) WarmAmethyst else Color.Transparent)
                                                .clickable { intakeMode = 0 }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Voice Mode Tab", tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("VOICE INGEST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                        }

                                        // Unstructured Text Tab
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (intakeMode == 1) WarmAmethyst else Color.Transparent)
                                                .clickable { intakeMode = 1 }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Text Mode Tab", tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("TEXT INGEST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                        }
                                    }

                                    if (intakeMode == 0) {
                                        // Voice Ingest Layout
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Recording status icon",
                                                    tint = if (isRecording) ElectricCrimson else WarmAmethyst,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "VOICE INGEST CAPTURE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isRecording) ElectricCrimson.copy(alpha = 0.2f)
                                                        else if (isProcessing) WarmAmethyst.copy(alpha = 0.2f)
                                                        else GrayBorder
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isRecording) "RECORDING" else if (isProcessing) "TRANSCRIBING..." else "IDLE",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRecording) ElectricCrimson else if (isProcessing) WarmAmethyst else SubtitleGray
                                                )
                                            }
                                        }

                                        if (isRecording) {
                                            // Simulated jumping bars
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = formattedTime,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElectricCrimson,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(end = 12.dp)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val infiniteTransition = rememberInfiniteTransition()
                                                    val h1 by infiniteTransition.animateFloat(
                                                        initialValue = 6f, targetValue = 24f,
                                                        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse)
                                                    )
                                                    val h2 by infiniteTransition.animateFloat(
                                                        initialValue = 18f, targetValue = 8f,
                                                        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse)
                                                    )
                                                    val h3 by infiniteTransition.animateFloat(
                                                        initialValue = 8f, targetValue = 22f,
                                                        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse)
                                                    )
                                                    listOf(h1, h2, h3, h2, h1).forEach { h ->
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .height(h.dp)
                                                                .clip(RoundedCornerShape(50))
                                                                .background(ElectricCrimson)
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (isProcessing) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(color = WarmAmethyst, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("AI Smart Transcript & Extraction compiling...", color = Color.LightGray, fontSize = 11.sp)
                                            }
                                        } else {
                                            Text(
                                                text = "Click to record client wishes about ${approvedClientName}. Gemini AI will transpile, clean up audio waves and structure details before validation review.",
                                                color = SubtitleGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (!hasAudioPermission) {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    if (isRecording) {
                                                        val file = audioRecorder.stopRecording()
                                                        isRecording = false
                                                        if (file != null && file.exists()) {
                                                            viewModel.processPendingAudioForReview(file)
                                                        } else {
                                                            viewModel.showStatus("Recording error.")
                                                        }
                                                    } else {
                                                        val file = audioRecorder.startRecording()
                                                        if (file != null) {
                                                            isRecording = true
                                                            viewModel.showStatus("Dictation active. Describe client wishes clearly...")
                                                        } else {
                                                            viewModel.showStatus("Failed opening audio recording path.")
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isProcessing,
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) ElectricCrimson else WarmAmethyst),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("pending_voice_ingest_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isRecording) Icons.Default.AddCircle else Icons.Default.PlayArrow,
                                                    contentDescription = "Voice recorder control",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(text = if (isRecording) "Stop & Extrapolate Ingestion" else "Record Live Speech Dialog", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        // Unstructured Text Ingest Layout
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Type or paste raw chats describing the client's wishes:",
                                                color = SubtitleGray,
                                                fontSize = 11.sp
                                            )

                                            OutlinedTextField(
                                                value = simpleTextEntryInput,
                                                onValueChange = { simpleTextEntryInput = it },
                                                placeholder = {
                                                    Text(
                                                        text = "e.g., Wants a 3-bedroom villa in Fifth Settlement, budget 15 million, urgent contact phone +201012345678...",
                                                        fontSize = 11.sp,
                                                        color = SubtitleGray.copy(alpha = 0.5f)
                                                    )
                                                },
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = SpaceDark,
                                                    unfocusedContainerColor = SpaceDark,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = WarmAmethyst,
                                                    unfocusedBorderColor = GrayBorder
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().height(90.dp).testTag("pending_text_ingest_field"),
                                                maxLines = 4
                                            )

                                            Button(
                                                onClick = {
                                                    if (simpleTextEntryInput.isNotBlank()) {
                                                        viewModel.submitPendingEntryForReview(simpleTextEntryInput, "Unstructured Text") {
                                                            simpleTextEntryInput = ""
                                                        }
                                                    }
                                                },
                                                enabled = !isProcessing && simpleTextEntryInput.isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = WarmAmethyst, disabledContainerColor = GrayBorder),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("pending_text_ingest_button")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Parse text", modifier = Modifier.size(14.dp))
                                                    Text("AI Parse & Extract Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // PHASE 3: REVIEW PROPOSAL AND EXPLICIT APPROVAL TO COMMIT
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SpaceDark, RoundedCornerShape(12.dp))
                                            .border(1.dp, DynamicColorAccent(pendingSource ?: ""), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Review Proposal icon",
                                                    tint = AccentGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "SECURE INTELLIGENCE PROPOSAL",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = AccentGreen,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AccentGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PENDING APPROVAL",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentGreen
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = GrayBorder, thickness = 1.dp)

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ProposalInfoRow(label = "NAME DIRECTORY", valText = pendingStructuredLead?.name ?: "")
                                            ProposalInfoRow(label = "PHONE ACCESS", valText = pendingStructuredLead?.phone ?: "")
                                            ProposalInfoRow(label = "BUDGET ASSIGNED", valText = pendingStructuredLead?.budgetRange ?: "", isBadge = true, color = cosmicGoldToAccentGreen(pendingStructuredLead?.budgetRange ?: ""))
                                            ProposalInfoRow(label = "PROPERTY ZONE INTEREST", valText = "${pendingStructuredLead?.propertyType} in ${pendingStructuredLead?.locationOfInterest}")
                                            ProposalInfoRow(label = "EXTRACTED CRM SUMMATION", valText = pendingStructuredLead?.notesSummary ?: "", isSummary = true)
                                        }

                                        HorizontalDivider(color = GrayBorder, thickness = 1.dp)

                                        Text(
                                            text = "يرجى تأكيد دقة هذه البيانات المستخرجة للموافقة عليها وحفظها نهائياً بقاعدة البيانات لملف العميل المعني.",
                                            color = AccentGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.commitPendingEntry() },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = SpaceDark),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1.3f).testTag("approve_proposal_button")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(14.dp))
                                                    Text("Approve & Save (موافق وحفظ)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.rejectPendingEntry() },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCrimson),
                                                border = BorderStroke(1.dp, ElectricCrimson.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(0.7f).testTag("reject_proposal_button")
                                            ) {
                                                Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Action Toast Overlays
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .padding(horizontal = 24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GrayBorder),
                modifier = Modifier.shadow(8.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Modern node displaying a Lead that doesn't exist in DB with immediate auto-log triggers.
 */
@Composable
fun NewLeadAutoLogNode(
    query: String,
    onAutoLog: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricCrimson.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricCrimson)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = query,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No recorded database entry matched",
                    color = SubtitleGray,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onAutoLog,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .defaultMinSize(minHeight = 38.dp)
            ) {
                Text(
                    text = "Auto-Log",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Beautiful visual representation of a clean "Behavior Analytics Display Node"
 * Focusing ONLY on: last calls count, avg duration, behavior spikes, normal rate.
 * NO percentages, NO unit names, NO irrelevant data clutter.
 */
@Composable
fun BehaviorNodeCard(
    lead: Lead,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onSync: () -> Unit
) {
    // Determine active behavior warning spike state
    val isSpikeActive = lead.behaviorSpikes > lead.normalRate

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) WarmAmethyst else if (isSpikeActive) ElectricCrimson.copy(alpha = 0.4f) else GrayBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Name / Contact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = lead.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lead.phone,
                        color = SubtitleGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSpikeActive) {
                        // High impact visual pulse symbol for spikes focus
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ElectricCrimson.copy(alpha = 0.15f))
                                .border(1.dp, ElectricCrimson.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                GlowingPulseDot()
                                Text(
                                    text = "SPIKE DETECTED",
                                    color = ElectricCrimson,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove entry",
                            tint = SubtitleGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Four strict numerical behavioral metrics nodes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BehaviorMetricColumn(
                    title = "LAST CALLS",
                    value = lead.lastCallsCount.toString(),
                    color = Color.White
                )
                BehaviorMetricColumn(
                    title = "AVG DURATION",
                    value = lead.avgDuration.toString(),
                    color = AccentGreen
                )
                BehaviorMetricColumn(
                    title = "SPIKES",
                    value = lead.behaviorSpikes.toString(),
                    color = if (isSpikeActive) ElectricCrimson else CosmicGold
                )
                BehaviorMetricColumn(
                    title = "NORMAL RATE",
                    value = lead.normalRate.toString(),
                    color = SubtitleGray
                )
            }

            // Behavior Analysis Focus: Visual Sparkline Spark Line Canvas showing the activity spike peaks!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BEHAVIOR PATTERN",
                    fontSize = 8.sp,
                    color = SubtitleGray,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                // High-End, lightweight telemetry drawing illustrating the peaks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(SpaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val path = Path()
                        
                        // Draw simulated spike waves depending on whether lead behavior is spiked right now
                        val points = if (isSpikeActive) {
                            listOf(0.2f, 0.15f, 0.95f, 0.1f, 0.85f, 0.3f, 0.90f) // Sharp peaked high-frequency spike wave 
                        } else {
                            listOf(0.2f, 0.25f, 0.35f, 0.3f, 0.40f, 0.32f, 0.28f) // Flat, smooth, baseline wave
                        }

                        val stepX = width / (points.size - 1)
                        points.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = height - (value * height)
                            if (idx == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = if (isSpikeActive) ElectricCrimson else AccentGreen,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Sync status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (lead.isSynced) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = "Sync Lock indicator",
                        tint = if (lead.isSynced) AccentGreen else SubtitleGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (lead.isSynced) "SYNCED" else "LOCKED",
                        color = if (lead.isSynced) AccentGreen else SubtitleGray.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { if (!lead.isSynced) onSync() }
                    )
                }
            }

            // Client Journal update summary manually logged
            if (lead.notesSummary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F111A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CLIENT JOURNAL LOGS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = WarmAmethyst,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = lead.notesSummary,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BehaviorMetricColumn(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = SubtitleGray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Glowing live pulse dot indicator animation
 */
@Composable
fun GlowingPulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(ElectricCrimson.copy(alpha = alpha))
    )
}

@Composable
fun EmptyRegistryBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "info logo icon",
                tint = SubtitleGray.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Workspace Idle",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter a name above to log their action metrics.",
                color = SubtitleGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProposalInfoRow(label: String, valText: String, isBadge: Boolean = false, color: Color = Color.White, isSummary: Boolean = false) {
    if (isSummary) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = label, color = SubtitleGray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131722), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(text = valText, color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = SubtitleGray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            if (isBadge) {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = valText, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = valText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

fun DynamicColorAccent(source: String): Color {
    return if (source == "Voice Recording") Color(0xFF8B5CF6) else Color(0xFF10B981)
}

fun cosmicGoldToAccentGreen(budget: String): Color {
    return if (budget.contains("any", ignoreCase = true) || budget.contains("unknown", ignoreCase = true)) Color.Gray else Color(0xFFFBBF24)
}
