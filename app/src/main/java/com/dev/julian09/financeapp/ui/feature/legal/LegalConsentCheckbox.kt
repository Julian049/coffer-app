package com.dev.julian09.financeapp.ui.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.LegalDocs
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** "Acepto los Términos de uso y la Política de privacidad", with both documents one tap away. */
@Composable
fun LegalConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CofferTheme.colors
    val link = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
    Row(
        modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = colors.onPrimary),
        )
        Text(
            buildAnnotatedString {
                append("Acepto los ")
                withLink(LinkAnnotation.Url(LegalDocs.TERMS_URL, link)) { append("Términos de uso") }
                append(" y la ")
                withLink(LinkAnnotation.Url(LegalDocs.PRIVACY_URL, link)) { append("Política de privacidad") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
    }
}
