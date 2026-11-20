package com.cairn.emergency.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@Composable
internal fun CairnScreenHeader(title: String, onBack: () -> Unit) {
    val label = stringResource(R.string.back)
    val ink = Color(0xFF102B4E)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = label }) {
            CairnGlyph(CairnIcon.Back, ink, Modifier.size(22.dp))
        }
        Text(title, color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}
