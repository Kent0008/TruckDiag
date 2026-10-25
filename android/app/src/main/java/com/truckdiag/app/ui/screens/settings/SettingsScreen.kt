package com.truckdiag.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.data.remote.ApiClient
import com.truckdiag.app.ui.components.DiagnosticCard
import com.truckdiag.app.ui.components.SimulatedBadge
import com.truckdiag.app.ui.navigation.Screen
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "ru"
    val currentLanguageName = stringResource(
        if (currentLanguage == "ky") R.string.language_kyrgyz else R.string.language_russian
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiagnosticCard(title = stringResource(R.string.language_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentLanguageName, color = TextPrimary)
                    OutlinedButton(onClick = { showLanguageDialog = true }) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.language_change_action))
                        }
                    }
            }

            DiagnosticCard(title = stringResource(R.string.pref_network_title)) {
                Text(
                    text = stringResource(R.string.current_api_address, ApiClient.getBaseUrl()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Connection.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SettingsEthernet, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_open_connection))
                }
            }

            DiagnosticCard(title = stringResource(R.string.pref_about_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.android_client_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    SimulatedBadge()
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.app_version_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.protocol_support_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.app_architecture_text) + "\n\n" +
                        stringResource(R.string.app_diagnostic_stack_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_dialog_title), color = TextPrimary) },
            text = {
                Column {
                    listOf(
                        "ru" to R.string.language_russian,
                        "ky" to R.string.language_kyrgyz
                    ).forEach { (languageTag, nameResource) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(languageTag)
                                    )
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == languageTag,
                                onClick = {
                                    showLanguageDialog = false
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(languageTag)
                                    )
                                }
                            )
                            Text(
                                text = stringResource(nameResource),
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
