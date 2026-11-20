package com.cairn.emergency.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cairn.emergency.R
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.theme.LocalCairnHighContrast

internal val JourneyNavy = Color(0xFF102B4E)
internal val JourneyPurple = Color(0xFF7553D8)
internal val JourneyTeal = Color(0xFF159B86)
internal val JourneyPaper: Color
    @Composable get() = if (LocalCairnHighContrast.current) Color.White else Color(0xFFF5F7FB)
internal val JourneyOutline: Color
    @Composable get() = if (LocalCairnHighContrast.current) JourneyNavy else Color(0xFFD5DEE9)

@Composable
internal fun JourneyScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = JourneyNavy,
        topBar = {
            val back = stringResource(R.string.back)
            Row(
                Modifier.fillMaxWidth().background(JourneyNavy).statusBarsPadding()
                    .heightIn(min = 56.dp).padding(end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = back }) {
                    CairnGlyph(CairnIcon.Back, Color.White, Modifier.size(21.dp))
                }
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = JourneyPaper,
            ) { content() }
        }
    }
}
