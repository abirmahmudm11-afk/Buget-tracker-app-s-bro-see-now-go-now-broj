package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.SpendlyBrandLogo
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.CurrencyCatalog
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    initialCurrencyCode: String,
    initialLanguage: String,
    onGetStarted: (currencyCode: String, language: String, loadPreviewSample: Boolean) -> Unit,
) {
    var selectedCurrency by remember { mutableStateOf(initialCurrencyCode) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var loadPreviewSample by remember { mutableStateOf(true) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60)
        visible = true
    }

    val currencySpec = remember(selectedCurrency) { CurrencyCatalog.findByCode(selectedCurrency) }
    val semantic = SpendlyTheme.semantic

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.LuxuryVaultBackdrop(prominent = true)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Box(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 18.dp),
                        ) {
                            Button(
                                onClick = { onGetStarted(selectedCurrency, selectedLanguage, loadPreviewSample) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("onboarding_get_started"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = semantic.heroSurface,
                                    contentColor = semantic.onHeroSurface,
                                ),
                            ) {
                                Text(
                                    text = "Enter Spendly",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Spacer(Modifier.size(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = semantic.heroGold,
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                SpendlyBrandLogo()

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 10 },
                    ),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Tailor your\n")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("private ledger.")
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        initialOffsetY = { it / 6 },
                    ),
                ) {
                    LedgerGroupCard {
                        Surface(
                            onClick = { showCurrencyPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_currency_selector"),
                            color = Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CurrencyExchange,
                                            contentDescription = "Default currency",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Base Currency",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "${currencySpec.code} (${currencySpec.symbol}) — ${currencySpec.displayName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        Surface(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_language_selector"),
                            color = Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(semantic.warningContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Language,
                                            contentDescription = "Language",
                                            tint = semantic.warning,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Language",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = selectedLanguage,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            ) {
                                Text(
                                    text = "Preload client portfolio",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Starts with curated cards, accounts, and budgets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = loadPreviewSample,
                                onCheckedChange = { loadPreviewSample = it },
                                modifier = Modifier.testTag("onboarding_sample_switch"),
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showCurrencyPicker) {
        SearchableCurrencySheet(
            currentCurrencyCode = selectedCurrency,
            onDismiss = { showCurrencyPicker = false },
            onConfirmCurrency = {
                selectedCurrency = it.code
                showCurrencyPicker = false
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "English" to "Active default language",
                        "বাংলা (Bangla)" to "Regional preference saved to profile",
                    ).forEach { (lang, desc) ->
                        val active = selectedLanguage == lang
                        Surface(
                            onClick = {
                                selectedLanguage = lang
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (active) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lang, style = MaterialTheme.typography.titleSmall)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (active) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
