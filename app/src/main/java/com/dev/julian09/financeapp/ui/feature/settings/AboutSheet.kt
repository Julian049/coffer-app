package com.dev.julian09.financeapp.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.LegalDocs
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** S14-UI03: version, the two legal documents and the open-source notices. */
@Composable
fun AboutSheet(
    version: String,
    onDismiss: () -> Unit,
) {
    val colors = CofferTheme.colors
    val uriHandler = LocalUriHandler.current
    AppBottomSheet(onDismiss = onDismiss, title = "Acerca de Coffer", subtitle = "Versión $version") {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Coffer es una app de finanzas personales hecha por un desarrollador independiente. El código, la marca y la " +
                    "mascota tienen todos los derechos reservados. El logo y la mascota se generaron con ChatGPT.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            listOf("Términos de uso" to LegalDocs.TERMS_URL, "Política de privacidad" to LegalDocs.PRIVACY_URL).forEach { (label, url) ->
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) }.padding(vertical = 4.dp),
                )
            }
            Text("Código abierto de terceros", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            THIRD_PARTY_NOTICES.forEach { notice ->
                Column {
                    Text(notice.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text(notice.license, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}
