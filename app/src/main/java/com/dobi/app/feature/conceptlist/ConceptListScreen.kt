package com.dobi.app.feature.conceptlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dobi.app.core.navigation.ConceptListMode
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.SFProBold
import com.dobi.app.core.ui.theme.SFProSemiBold
import com.dobi.app.core.ui.theme.SFProMedium
import com.dobi.app.core.ui.theme.SFProRegular
import com.dobi.app.core.ui.theme.SFProLight
import com.dobi.app.core.ui.theme.DobiPrimary
import com.dobi.app.core.ui.theme.DobiTextPrimary
import com.dobi.app.core.ui.theme.DobiTextSecondary
import com.dobi.app.core.ui.theme.DobiTheme

// ── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun ConceptListScreen(
    mode: ConceptListMode = ConceptListMode.DEFAULT,
    onNavigateBack: () -> Unit = {},
    onConceptClick: (String) -> Unit = {},
    onUnitClick: (String) -> Unit = {},
    viewModel: ConceptListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DobiBackground,
        topBar = {
            ConceptListTopBar(
                title = if (mode == ConceptListMode.PRACTISE_LESSON) "Pick a Lesson" else "Concepts",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!)
                uiState.subjectGroups.isEmpty() -> EmptyState(uiState.searchQuery)
                else -> ConceptTree(
                    groups = uiState.subjectGroups,
                    mode = mode,
                    onSubjectToggled = viewModel::onSubjectToggled,
                    onUnitToggled = viewModel::onUnitToggled,
                    onUnitClick = onUnitClick,
                    onConceptClick = onConceptClick
                )
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConceptListTopBar(title: String, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = DobiTextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DobiTextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DobiTextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DobiBackground)
    )
}

// ── Search bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                "Search concepts, subjects…",
                color = DobiTextSecondary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = DobiTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = DobiTextPrimary,
            unfocusedTextColor = DobiTextPrimary,
            cursorColor = DobiPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Collapsible tree ───────────────────────────────────────────────────────────

@Composable
private fun ConceptTree(
    groups: List<SubjectGroup>,
    mode: ConceptListMode,
    onSubjectToggled: (String) -> Unit,
    onUnitToggled: (String, String) -> Unit,
    onUnitClick: (String) -> Unit,
    onConceptClick: (String) -> Unit
) {
    val isPractise = mode == ConceptListMode.PRACTISE_LESSON
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            // Subject row
            item(key = "subject_${group.subject}") {
                SubjectRow(
                    group = group,
                    onClick = { onSubjectToggled(group.subject) }
                )
            }

            if (group.isExpanded) {
                group.units.forEach { unit ->
                    item(key = "unit_${unit.unitId}") {
                        Box(modifier = Modifier.background(Color(0xFFF8FAFF))) {
                            UnitRow(
                                unit = unit,
                                isPractise = isPractise,
                                onClick = {
                                    if (isPractise) onUnitClick(unit.unitId)
                                    else onUnitToggled(group.subject, unit.unitId)
                                }
                            )
                        }
                    }
                    // Practise mode keeps concepts visible so a lesson OR concept can be
                    // picked with one tap. Default mode expands only when the unit is toggled.
                    if (isPractise || unit.isExpanded) {
                        items(
                            items = unit.concepts,
                            key = { concept -> "concept_${concept.id}" }
                        ) { concept ->
                            Box(modifier = Modifier.background(Color(0xFFF8FAFF))) {
                                ConceptRow(
                                    name = concept.name,
                                    onClick = { onConceptClick(concept.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Subject row ────────────────────────────────────────────────────────────────

@Composable
private fun SubjectRow(group: SubjectGroup, onClick: () -> Unit) {
    val bgColor = parseHexColor(group.subjectColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored subject icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = group.subjectIcon, fontSize = 18.sp)
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = group.subject,
            style = MaterialTheme.typography.titleMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${group.units.size} Units",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary
        )

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = if (group.isExpanded) Icons.Default.KeyboardArrowUp
                          else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = DobiTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }

    // Divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}

// ── Unit row ───────────────────────────────────────────────────────────────────

@Composable
private fun UnitRow(unit: UnitGroup, isPractise: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small accent line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DobiPrimary.copy(alpha = 0.4f))
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = unit.unitName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = SFProSemiBold,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${unit.concepts.size} Concepts",
            style = MaterialTheme.typography.bodySmall,
            color = DobiTextSecondary
        )

        Spacer(Modifier.width(8.dp))

        val icon = when {
            isPractise -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            unit.isExpanded -> Icons.Default.KeyboardArrowUp
            else -> Icons.Default.KeyboardArrowDown
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DobiTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 40.dp)
            .background(Color(0xFFF1F5F9))
    )
}

// ── Concept row ────────────────────────────────────────────────────────────────

@Composable
private fun ConceptRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 52.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = DobiTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DobiTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 52.dp)
            .background(Color(0xFFF1F5F9))
    )
}

// ── States ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DobiPrimary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) "No concepts found" else "No results for \"$query\"",
            color = DobiTextSecondary
        )
    }
}

// ── Util ───────────────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF22C55E))

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConceptListPreview() {
    DobiTheme {
        ConceptTree(
            groups = listOf(
                SubjectGroup(
                    subject = "Science",
                    subjectColor = "#22C55E",
                    subjectIcon = "🔬",
                    isExpanded = true,
                    units = listOf(
                        UnitGroup(
                            unitId = "sci6_u01",
                            unitName = "Food: Where Does It Come From?",
                            isExpanded = true,
                            concepts = emptyList()
                        ),
                        UnitGroup(
                            unitId = "sci6_u02",
                            unitName = "Components of Food",
                            concepts = emptyList()
                        )
                    )
                )
            ),
            mode = ConceptListMode.DEFAULT,
            onSubjectToggled = {},
            onUnitToggled = { _, _ -> },
            onUnitClick = {},
            onConceptClick = {}
        )
    }
}
