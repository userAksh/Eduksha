package com.dobi.app.feature.parenthome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dobi.app.core.ui.theme.DobiBackground
import com.dobi.app.core.ui.theme.DobiTextPrimary

@Composable
fun ParentHomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DobiBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Parent Home — Coming Soon", color = DobiTextPrimary)
    }
}
