package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.util.CardArtPreferences
import com.example.util.MoneyFormatter
import kotlin.math.roundToLong

private data class ShowcaseDeckCard(
    val account: AccountRecord,
    val balanceMinor: Long,
    val art: CardArtPreferences,
    val chipTitle: String,
    val chipColor: Color,
)

/**
 * Interactive stacked Google Wallet 3D card deck showcase used on Auth & Onboarding.
 * Replaces single-color cards with multi-card real network styles (Visa Sapphire,
 * Mastercard Obsidian, Amex Champagne Gold) with drag-to-tilt, tap-to-flip, and deck switching.
 */
@Composable
fun AnimatedLedgerHero(
    modifier: Modifier = Modifier,
    badgeLabel: String = "Google Wallet Style",
) {
    val deckCards = remember {
        listOf(
            ShowcaseDeckCard(
                account = AccountRecord(
                    id = "showcase_visa",
                    name = "Sapphire Checking",
                    type = AccountType.DEBIT_CARD,
                    institutionName = "Chase Bank",
                    cardNetwork = "Visa",
                    lastFour = "4821",
                    currencyCode = "USD",
                    openingBalanceMinor = 642_580L,
                ),
                balanceMinor = 642_580L,
                art = CardArtPreferences(finishId = CardFinishPreset.SAPPHIRE.id),
                chipTitle = "Visa • 4821",
                chipColor = Color(0xFF1A52B8),
            ),
            ShowcaseDeckCard(
                account = AccountRecord(
                    id = "showcase_mc",
                    name = "World Elite",
                    type = AccountType.CREDIT_CARD,
                    institutionName = "Citi Private",
                    cardNetwork = "Mastercard",
                    lastFour = "9014",
                    currencyCode = "USD",
                    openingBalanceMinor = 84_210L,
                    creditLimitMinor = 1_200_000L,
                ),
                balanceMinor = 84_210L,
                art = CardArtPreferences(finishId = CardFinishPreset.OBSIDIAN.id),
                chipTitle = "Mastercard • 9014",
                chipColor = Color(0xFF1E2228),
            ),
            ShowcaseDeckCard(
                account = AccountRecord(
                    id = "showcase_amex",
                    name = "Gold Reserve",
                    type = AccountType.CREDIT_CARD,
                    institutionName = "American Express",
                    cardNetwork = "Amex",
                    lastFour = "3008",
                    currencyCode = "USD",
                    openingBalanceMinor = 41_900L,
                    creditLimitMinor = 800_000L,
                ),
                balanceMinor = 41_900L,
                art = CardArtPreferences(finishId = CardFinishPreset.CHAMPAGNE.id),
                chipTitle = "Amex • 3008",
                chipColor = Color(0xFFC59F4E),
            ),
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val activeCard = deckCards[selectedIndex]
    val backCardOne = deckCards[(selectedIndex + 1) % deckCards.size]
    val backCardTwo = deckCards[(selectedIndex + 2) % deckCards.size]

    Column(
        modifier = modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Google Wallet Stacked Back-Card Peeks + Active Interactive 3D Front Card
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Back card 2 (deepest peek edge)
            val back2Top by animateDpAsState(0.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "b2")
            Box(
                modifier = Modifier
                    .padding(top = back2Top, start = 28.dp, end = 28.dp)
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(backCardTwo.art.finishId.let {
                                CardFinishPreset.resolve(backCardTwo.account, it).startColor
                            }, CardFinishPreset.resolve(backCardTwo.account, backCardTwo.art.finishId).midColor)
                        )
                    ),
            )

            // Back card 1 (mid peek edge)
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CardFinishPreset.resolve(backCardOne.account, backCardOne.art.finishId).startColor,
                                CardFinishPreset.resolve(backCardOne.account, backCardOne.art.finishId).midColor,
                            )
                        )
                    ),
            )

            // Interactive 3D Front Card
            AnimatedContent(
                targetState = activeCard,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                        .togetherWith(fadeOut(tween(150)))
                },
                modifier = Modifier.padding(top = 24.dp),
                label = "activeWalletDeckCard",
            ) { current ->
                Interactive3DWalletCard(
                    account = current.account,
                    balanceMinor = current.balanceMinor,
                    hideBalances = false,
                    artPrefs = current.art,
                    enableDragTilt = true,
                    secondaryHint = "Drag tilt • Tap flip",
                    testTag = "animated_3d_card",
                )
            }
        }

        // Tactile Wallet Stack Switcher Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            deckCards.forEachIndexed { idx, card ->
                val selected = idx == selectedIndex
                Surface(
                    onClick = { selectedIndex = idx },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.testTag("wallet_deck_tab_$idx"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(card.chipColor),
                        )
                        Text(
                            text = card.chipTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMoneyText(
    amountMinor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showPositivePlus: Boolean = false,
    hidden: Boolean = false,
    hiddenText: String = "••••••",
) {
    val animatedFloat by animateFloatAsState(
        targetValue = amountMinor.toFloat(),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "animatedMoney",
    )
    val displayText = if (hidden) {
        hiddenText
    } else {
        MoneyFormatter.formatMinor(
            amountMinor = animatedFloat.roundToLong(),
            currencyCode = currencyCode,
            showPositivePlus = showPositivePlus,
        )
    }
    Text(
        text = displayText,
        style = style,
        color = color,
        modifier = modifier,
    )
}
