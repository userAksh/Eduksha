package com.dobi.app.feature.mylearnings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NewsprintBackground = Color(0xFF161616)
private val NewsprintPaper = Color(0xFFFAFAF6)
private val NewsprintPaperEdge = Color(0xFFE3E0D6)
private val NewsprintInk = Color(0xFF111111)
private val NewsprintMuted = Color(0xFF555555)
private val NewsprintRule = Color(0xFF111111)
private val NewsprintAccent = Color(0xFF111111)

@Composable
fun NewspaperOverlay(
    stories: List<CurrentAffair>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) return
    val safeStart = startIndex.coerceIn(0, stories.size - 1)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        NewspaperContent(
            stories = stories,
            startIndex = safeStart,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun NewspaperContent(
    stories: List<CurrentAffair>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { stories.size }
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date()) }
    val romanEdition = remember(stories.size) { toRoman(stories.size) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(6.dp),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(6.dp))
                .background(NewsprintBackground)
                .padding(2.dp)
                .background(NewsprintPaperEdge)
                .padding(2.dp)
                .background(NewsprintPaper)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Masthead(
                    dateLabel = today,
                    romanEdition = romanEdition,
                    pageNumber = pagerState.currentPage + 1,
                    totalPages = stories.size,
                    onClose = onDismiss
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) { page ->
                        val pageOffset =
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        NewspaperPage(
                            story = stories[page],
                            pageNumber = page + 1,
                            totalPages = stories.size,
                            onTapPrev = {
                                scope.launch {
                                    val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    if (target != pagerState.currentPage) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            },
                            onTapNext = {
                                scope.launch {
                                    val target = (pagerState.currentPage + 1)
                                        .coerceAtMost(stories.size - 1)
                                    if (target != pagerState.currentPage) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (pageOffset >= 0f) 1f else 0f)
                                .graphicsLayer {
                                    // Pin every page so they stack like real
                                    // newspaper sheets instead of sliding past.
                                    translationX = pageOffset * size.width
                                    cameraDistance = 14f * density

                                    if (pageOffset > 0f) {
                                        val clamped = pageOffset.coerceAtMost(1f)
                                        rotationY = -135f * clamped
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        val fade = 1f - clamped * 0.3f
                                        alpha = fade
                                        val s = 1f - clamped * 0.05f
                                        scaleX = s
                                        scaleY = s
                                    }
                                }
                        )
                    }
                }

                FooterBar(
                    currentPage = pagerState.currentPage,
                    totalPages = stories.size,
                    onPrev = {
                        scope.launch {
                            val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                            if (target != pagerState.currentPage) {
                                pagerState.animateScrollToPage(target)
                            }
                        }
                    },
                    onNext = {
                        scope.launch {
                            val target = (pagerState.currentPage + 1)
                                .coerceAtMost(stories.size - 1)
                            if (target != pagerState.currentPage) {
                                pagerState.animateScrollToPage(target)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Masthead(
    dateLabel: String,
    romanEdition: String,
    pageNumber: Int,
    totalPages: Int,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        // Top hairline rule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(1.dp)
                .background(NewsprintRule)
        )
        Spacer(Modifier.height(4.dp))

        // Top row: VOL. NO. -- close button -- price/edition
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOL. I · NO. $romanEdition",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                color = NewsprintInk,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "DAILY EDITION  ·  ₹0",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                color = NewsprintInk,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))

        // The masthead title block
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "The",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = NewsprintInk
                )
                Text(
                    text = "EDUKSHA TIMES",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = NewsprintInk,
                    letterSpacing = 3.sp
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = NewsprintInk
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "✦  ALL THE NEWS THAT'S KID TO PRINT  ✦",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 9.sp,
            color = NewsprintMuted,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))

        // Double-rule under masthead
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(2.5.dp)
                .background(NewsprintRule)
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(0.7.dp)
                .background(NewsprintRule)
        )
        Spacer(Modifier.height(4.dp))

        // Date / page strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel.uppercase(),
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                color = NewsprintInk,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "☀  CLEAR & CURIOUS",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 9.sp,
                color = NewsprintMuted,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "PAGE $pageNumber / $totalPages",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                color = NewsprintInk,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(0.7.dp)
                .background(NewsprintRule)
        )
    }
}

@Composable
private fun NewspaperPage(
    story: CurrentAffair,
    pageNumber: Int,
    totalPages: Int,
    onTapPrev: () -> Unit,
    onTapNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(10.dp))
            .background(NewsprintPaper)
            .border(1.dp, NewsprintInk.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (offset.x < size.width / 2f) onTapPrev() else onTapNext()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            // Category strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(NewsprintAccent)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = story.category.uppercase(),
                        color = NewsprintPaper,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "PAGE $pageNumber OF $totalPages",
                    color = NewsprintMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = story.headline,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                color = NewsprintInk
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NewsprintInk.copy(alpha = 0.4f))
            )
            Spacer(Modifier.height(14.dp))

            // "Photo" — emoji block in a framed card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NewsprintInk.copy(alpha = 0.06f),
                                NewsprintInk.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, NewsprintInk.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = story.emoji, fontSize = 84.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "— Illustration —",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 10.sp,
                color = NewsprintMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // First letter as a drop cap, rest of body inline.
            val body = story.body
            if (body.isNotBlank()) {
                val firstChar = body.first().toString()
                val rest = body.drop(1)
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = firstChar,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 56.sp,
                        lineHeight = 56.sp,
                        color = NewsprintInk,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = rest,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = NewsprintInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NewsprintInk.copy(alpha = 0.25f))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Curated for Eduksha readers • Stay curious!",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = NewsprintMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FooterBar(
    currentPage: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val canPrev = currentPage > 0
    val canNext = currentPage < totalPages - 1
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(0.7.dp)
                .background(NewsprintInk.copy(alpha = 0.6f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrev,
                enabled = canPrev,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous page",
                    tint = if (canPrev) NewsprintInk else NewsprintInk.copy(alpha = 0.25f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(totalPages) { i ->
                    val active = i == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) NewsprintAccent else NewsprintInk.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            IconButton(
                onClick = onNext,
                enabled = canNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next page",
                    tint = if (canNext) NewsprintInk else NewsprintInk.copy(alpha = 0.25f)
                )
            }
        }
    }
}

private fun toRoman(n: Int): String {
    if (n <= 0) return "I"
    val values = intArrayOf(10, 9, 5, 4, 1)
    val symbols = arrayOf("X", "IX", "V", "IV", "I")
    val sb = StringBuilder()
    var num = n
    for (i in values.indices) {
        while (num >= values[i]) {
            sb.append(symbols[i]); num -= values[i]
        }
    }
    return sb.toString()
}
