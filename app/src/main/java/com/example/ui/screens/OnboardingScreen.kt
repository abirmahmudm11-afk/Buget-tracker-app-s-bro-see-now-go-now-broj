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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimatedLedgerHero
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
    var step1Visible by remember { mutableStateOf(false) }
    var step2Visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        step1Visible = true
        delay(140)
        step2Visible = true
    }

    val currencySpec = remember(selectedCurrency) { CurrencyCatalog.findByCode(selectedCurrency) }
    val semantic = SpendlyTheme.semantic

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        onClick = { onGetStarted(selectedCurrency, selectedLanguage, loadPreviewSample) },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_get_started"),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 4.dp,
                        color = Color.Transparent,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(semantic.heroCardStart, semantic.heroCardMid, semantic.heroCardEnd),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Get Started",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = semantic.heroAccent,
                                )
                            }
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
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpendlyBrandLogo()
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "CLOUD READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                // Animated Video-Like Motion Reel Hero
                AnimatedVisibility(
                    visible = step1Visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 8 },
                    ),
                ) {
                    AnimatedLedgerHero(badgeLabel = "LIVE LEDGER REEL")
                }

                AnimatedVisibility(
                    visible = step1Visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        initialOffsetY = { it / 6 },
                    ),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = buildAnnotatedString {
                                append("Organize manually.\n")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Understand automatically.")
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "A calm, deliberate personal ledger for accounts, budgets, notifications, and multi-currency clarity.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Staggered Feature Highlights & Setup
                AnimatedVisibility(
                    visible = step2Visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        initialOffsetY = { it / 5 },
                    ),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                BenefitRow(
                                    icon = Icons.Rounded.AccountBalanceWallet,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                                    title = "All Accounts, Manual & Transparent",
                                    subtitle = "Track bank accounts, cards, cash & wallets without sharing bank logins.",
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                BenefitRow(
                                    icon = Icons.Rounded.PieChart,
                                    iconTint = semantic.warning,
                                    iconBg = semantic.warningContainer,
                                    title = "Budgets & Smart Alerts",
                                    subtitle = "Real Android notifications before spending crosses monthly limits.",
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                BenefitRow(
                                    icon = Icons.Rounded.AutoGraph,
                                    iconTint = semantic.transfer,
                                    iconBg = semantic.transferContainer,
                                    title = "Multi-Currency Precision",
                                    subtitle = "Every record keeps its original currency with transparent conversion.",
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "INITIAL SETUP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                Surface(
                                    onClick = { showCurrencyPicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_currency_selector"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CurrencyExchange,
                                                    contentDescription = "Default currency",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(19.dp),
                                                )
                                            }
                                            Column {
                                                Text(
                                                    "Default Currency",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = "${currencySpec.code} (${currencySpec.symbol}) — ${currencySpec.displayName}",
                                                    style = MaterialTheme.typography.titleSmall,
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

                                Surface(
                                    onClick = { showLanguageDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_language_selector"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(semantic.warningContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Language,
                                                    contentDescription = "Language",
                                                    tint = semantic.warning,
                                                    modifier = Modifier.size(19.dp),
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = "Include starter sample ledger",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = "Preloads editable accounts & budgets so you can explore right away.",
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

                Spacer(Modifier.height(6.dp))
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
                            shape = RoundedCornerShape(14.dp),
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

@Composable
private fun BenefitRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
