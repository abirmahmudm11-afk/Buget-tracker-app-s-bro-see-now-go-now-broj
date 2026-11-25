package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.LuxuryVaultBackdrop
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

    Box(modifier = Modifier.fillMaxSize()) {
        LuxuryVaultBackdrop(prominent = true)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
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
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
                                append("Private wealth,\n")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("curated cleanly.")
                                }
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 8 },
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedLedgerHero(badgeLabel = "Private Vault")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (authError != null) {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = semantic.expenseContainer,
                        border = BorderStroke(1.dp, semantic.expense.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = authError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.expense,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
        }
    }
}
