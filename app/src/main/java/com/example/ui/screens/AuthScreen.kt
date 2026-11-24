package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleSignInButton
import com.example.auth.attemptAutoSignIn
import com.example.ui.components.AnimatedLedgerHero
import com.example.ui.components.SpendlyBrandLogo
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(120)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope,
        )
    }

    val semantic = SpendlyTheme.semantic

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
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
                    Text(
                        text = "CLOUD SYNC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it / 8 },
                ),
            ) {
                AnimatedLedgerHero(badgeLabel = "ANIMATED OVERVIEW")
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    initialOffsetY = { it / 5 },
                ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("Your personal finance,\n")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("synced & effortless.")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Sign in with Google to back up your accounts, budgets, and transactions across devices in real time.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AuthFeatureRow(
                        icon = Icons.Rounded.CloudDone,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        title = "Real-Time Cloud Persistence",
                        subtitle = "Every expense, budget, and account stays safely backed up.",
                    )
                    AuthFeatureRow(
                        icon = Icons.Rounded.NotificationsActive,
                        iconTint = semantic.warning,
                        iconBg = semantic.warningContainer,
                        title = "Smart Budget Alerts",
                        subtitle = "Instant Android alerts before monthly limits run over.",
                    )
                    AuthFeatureRow(
                        icon = Icons.Rounded.Shield,
                        iconTint = semantic.income,
                        iconBg = semantic.incomeContainer,
                        title = "Private Per-User Isolation",
                        subtitle = "Zero bank-login scraping — only your authenticated ledger.",
                    )
                }
            }

            if (authError != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.expenseContainer,
                ) {
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.expense,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            GoogleSignInButton(
                onAuthSuccess = {
                    authError = null
                    onAuthSuccess()
                },
                onAuthError = { msg -> authError = msg },
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
