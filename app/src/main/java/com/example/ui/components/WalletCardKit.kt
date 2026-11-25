package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.MoneyRowStyle
import com.example.util.CardArtPreferences
import com.example.util.MoneyFormatter
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

enum class CardFinishPreset(
    val id: String,
    val label: String,
    val swatchColor: Color,
    val startColor: Color,
    val midColor: Color,
    val endColor: Color,
    val backColor: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accentBadge: Color,
    val isLightSurface: Boolean,
    val goldChip: Boolean,
) {
    SAPPHIRE(
        id = "SAPPHIRE",
        label = "Sapphire",
        swatchColor = Color(0xFF1A4B9C),
        startColor = Color(0xFF0F2D6B),
        midColor = Color(0xFF1A52B8),
        endColor = Color(0xFF0A1D47),
        backColor = Color(0xFF0D2352),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFFC2D7FF),
        accentBadge = Color(0xFF8EC5FF),
        isLightSurface = false,
        goldChip = true,
    ),
    OBSIDIAN(
        id = "OBSIDIAN",
        label = "Obsidian",
        swatchColor = Color(0xFF1E2228),
        startColor = Color(0xFF1A1D23),
        midColor = Color(0xFF2B303A),
        endColor = Color(0xFF0D0F12),
        backColor = Color(0xFF13161B),
        textPrimary = Color(0xFFF8FAFC),
        textMuted = Color(0xFFB4BDC9),
        accentBadge = Color(0xFFE5C47B),
        isLightSurface = false,
        goldChip = true,
    ),
    CHAMPAGNE(
        id = "CHAMPAGNE",
        label = "Gold",
        swatchColor = Color(0xFFD5B363),
        startColor = Color(0xFFC59F4E),
        midColor = Color(0xFFE7CA7D),
        endColor = Color(0xFF967329),
        backColor = Color(0xFF8C6924),
        textPrimary = Color(0xFF231805),
        textMuted = Color(0xFF4F380D),
        accentBadge = Color(0xFF231805),
        isLightSurface = true,
        goldChip = false,
    ),
    TITANIUM(
        id = "TITANIUM",
        label = "Titanium",
        swatchColor = Color(0xFFDCE1E7),
        startColor = Color(0xFFDFE4EA),
        midColor = Color(0xFFF6F8FA),
        endColor = Color(0xFFBDC5CE),
        backColor = Color(0xFFC9D0D8),
        textPrimary = Color(0xFF141920),
        textMuted = Color(0xFF4A5565),
        accentBadge = Color(0xFF0F5C4D),
        isLightSurface = true,
        goldChip = false,
    ),
    CORAL(
        id = "CORAL",
        label = "Coral",
        swatchColor = Color(0xFFF15452),
        startColor = Color(0xFFDF3E44),
        midColor = Color(0xFFFF685B),
        endColor = Color(0xFFB1212A),
        backColor = Color(0xFFB82931),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFFFFD8D6),
        accentBadge = Color(0xFFFFFFFF),
        isLightSurface = false,
        goldChip = false,
    ),
    PLUM(
        id = "PLUM",
        label = "Plum",
        swatchColor = Color(0xFF4D2E87),
        startColor = Color(0xFF2D1952),
        midColor = Color(0xFF563494),
        endColor = Color(0xFF1B0E36),
        backColor = Color(0xFF221240),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFFD9C9FF),
        accentBadge = Color(0xFFCAB5FF),
        isLightSurface = false,
        goldChip = true,
    ),
    CRIMSON(
        id = "CRIMSON",
        label = "Crimson",
        swatchColor = Color(0xFF9C1D35),
        startColor = Color(0xFF6B1123),
        midColor = Color(0xFFA6203C),
        endColor = Color(0xFF420814),
        backColor = Color(0xFF4F0B1A),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFFFFC8D2),
        accentBadge = Color(0xFFFFB2C0),
        isLightSurface = false,
        goldChip = true,
    ),
    EMERALD(
        id = "EMERALD",
        label = "Emerald",
        swatchColor = Color(0xFF126350),
        startColor = Color(0xFF0C4336),
        midColor = Color(0xFF166F5A),
        endColor = Color(0xFF072A22),
        backColor = Color(0xFF09342A),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFFB8E2D7),
        accentBadge = Color(0xFF8EEBD1),
        isLightSurface = false,
        goldChip = true,
    );

    companion object {
        fun resolve(account: AccountRecord, customFinishId: String?): CardFinishPreset {
            entries.firstOrNull { it.id.equals(customFinishId, ignoreCase = true) }?.let { return it }
            val net = account.cardNetwork.trim().lowercase()
            val inst = account.institutionName.trim().lowercase()
            val name = account.name.trim().lowercase()
            return when {
                "amex" in net || "american" in net || "gold" in name -> CHAMPAGNE
                "master" in net || "mc" == net || "black" in name || "elite" in name -> OBSIDIAN
                "visa" in net || "chase" in inst || "sapphire" in name -> SAPPHIRE
                "discover" in net || "crimson" in name || "ruby" in name || "santander" in inst -> CRIMSON
                "monzo" in inst || "coral" in name -> CORAL
                "revolut" in inst || "nubank" in inst || "wise" in inst -> PLUM
                "apple" in inst || "platinum" in name || "titanium" in name -> TITANIUM
                account.type == AccountType.CREDIT_CARD -> OBSIDIAN
                account.type == AccountType.DEBIT_CARD -> CORAL
                account.type == AccountType.PREPAID_CARD -> PLUM
                account.type == AccountType.BANK_ACCOUNT -> SAPPHIRE
                account.type == AccountType.DIGITAL_WALLET -> TITANIUM
                account.type == AccountType.SAVINGS_ACCOUNT -> EMERALD
                account.type == AccountType.CASH_WALLET -> EMERALD
                else -> SAPPHIRE
            }
        }
    }
}

enum class CardNetworkBrand(val displayLabel: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("Amex"),
    DISCOVER("Discover"),
    RUPAY("RuPay"),
    UNIONPAY("UnionPay"),
    BANK("Bank"),
    ;

    companion object {
        fun resolve(network: String): CardNetworkBrand {
            val q = network.trim().lowercase()
            return when {
                "visa" in q -> VISA
                "master" in q || q == "mc" -> MASTERCARD
                "amex" in q || "american" in q -> AMEX
                "discover" in q -> DISCOVER
                "rupay" in q -> RUPAY
                "union" in q -> UNIONPAY
                else -> BANK
            }
        }
    }
}

/**
 * Interactive Google Wallet 3D Card with always-active tactile press tilt, optional 2-axis
 * drag tilt, tap-to-flip 180° with haptic feedback, real card photo / bank logo support,
 * and authentic card network marks.
 */
@Composable
fun Interactive3DWalletCard(
    account: AccountRecord,
    balanceMinor: Long,
    hideBalances: Boolean,
    artPrefs: CardArtPreferences,
    modifier: Modifier = Modifier,
    enableDragTilt: Boolean = true,
    externalTiltY: Float = 0f,
    subtitleOverride: String? = null,
    secondaryHint: String = "Tap card to flip",
    testTag: String = "wallet_card_${account.id}",
) {
    val haptic = LocalHapticFeedback.current
    val finish = remember(account, artPrefs.finishId) {
        CardFinishPreset.resolve(account, artPrefs.finishId)
    }
    val hasCustomPhoto = remember(artPrefs.photoPath) {
        !artPrefs.photoPath.isNullOrBlank() && File(artPrefs.photoPath).exists()
    }

    var isFlipped by remember(account.id) { mutableStateOf(false) }
    var isDragging by remember(account.id) { mutableStateOf(false) }
    var isPressed by remember(account.id) { mutableStateOf(false) }
    var pressOffset by remember(account.id) { mutableStateOf(Offset.Zero) }
    var cardBoxSize by remember(account.id) { mutableStateOf(IntSize.Zero) }
    var dragRotX by remember(account.id) { mutableFloatStateOf(0f) }
    var dragRotY by remember(account.id) { mutableFloatStateOf(0f) }

    val infinite = rememberInfiniteTransition(label = "walletCardIdle")
    val idlePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "idlePhase",
    )

    val idleRotY = if (isDragging || isPressed) 0f else sin(idlePhase) * 4.5f
    val idleRotX = if (isDragging || isPressed) 0f else sin(idlePhase * 0.7f + 0.9f) * 2.5f

    val normTouchX = if (cardBoxSize.width > 0) {
        ((pressOffset.x / cardBoxSize.width.toFloat()) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
    } else 0f
    val normTouchY = if (cardBoxSize.height > 0) {
        ((pressOffset.y / cardBoxSize.height.toFloat()) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
    } else 0f

    val touchTiltY = if (isPressed && !isDragging) normTouchX * 11f else 0f
    val touchTiltX = if (isPressed && !isDragging) -normTouchY * 8f else 0f

    val targetRotX = when {
        isDragging -> dragRotX
        isPressed -> touchTiltX
        else -> idleRotX
    }
    val targetRotY = (if (isFlipped) 180f else 0f) +
        when {
            isDragging -> dragRotY
            isPressed -> touchTiltY
            else -> idleRotY
        } +
        externalTiltY

    val animatedRotX by animateFloatAsState(
        targetValue = targetRotX,
        animationSpec = spring(
            dampingRatio = if (isDragging) Spring.DampingRatioNoBouncy else 0.60f,
            stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessMedium,
        ),
        label = "rotX",
    )
    val animatedRotY by animateFloatAsState(
        targetValue = targetRotY,
        animationSpec = spring(
            dampingRatio = if (isDragging) Spring.DampingRatioNoBouncy else 0.64f,
            stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessMedium,
        ),
        label = "rotY",
    )
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.024f
            isPressed -> 0.972f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "scale",
    )

    val normalizedY = ((animatedRotY % 360f) + 360f) % 360f
    val showFront = normalizedY <= 90f || normalizedY >= 270f
    val sheenBias = (((animatedRotY % 180f) + sin(idlePhase) * 8f + touchTiltY * 1.4f) / 28f).coerceIn(-1f, 1f)

    val effectiveTextPrimary = if (hasCustomPhoto) Color.White else finish.textPrimary
    val effectiveTextMuted = if (hasCustomPhoto) Color(0xFFE5ECE9) else finish.textMuted
    val effectiveIsLight = if (hasCustomPhoto) false else finish.isLightSurface

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val cardHeight = when {
            maxWidth < 360.dp -> 204.dp
            maxWidth < 500.dp -> 216.dp
            else -> 228.dp
        }

        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(cardHeight + 14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Dynamic 3D ground shadow reacting to tilt & press
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(cardHeight)
                    .graphicsLayer {
                        translationX = -animatedRotY.coerceIn(-25f, 25f) * 0.6f
                        translationY = 10.dp.toPx() + animatedRotX.coerceIn(-20f, 20f) * 0.35f
                        scaleX = if (isPressed) 0.92f else 0.94f
                        scaleY = if (isPressed) 0.92f else 0.94f
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = if (isDragging) 0.24f else 0.18f)),
            )

            // Non-consuming touch observer works seamlessly inside HorizontalPager too
            val touchObserverModifier = Modifier
                .onSizeChanged { cardBoxSize = it }
                .pointerInput(account.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        isPressed = true
                        pressOffset = down.position
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val active = event.changes.firstOrNull { it.pressed }
                            if (active != null) {
                                pressOffset = active.position
                            } else {
                                break
                            }
                        }
                        isPressed = false
                    }
                }

            val gestureModifier = if (enableDragTilt) {
                touchObserverModifier
                    .pointerInput(account.id) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isFlipped = !isFlipped
                            },
                        )
                    }
                    .pointerInput(account.id) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                dragRotX = 0f
                                dragRotY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragRotX = 0f
                                dragRotY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragRotY = (dragRotY + dragAmount.x * 0.22f).coerceIn(-28f, 28f)
                                dragRotX = (dragRotX - dragAmount.y * 0.18f).coerceIn(-20f, 20f)
                            },
                        )
                    }
            } else {
                touchObserverModifier.pointerInput(account.id) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = !isFlipped
                        },
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .testTag(testTag)
                    .graphicsLayer {
                        cameraDistance = 14f * density
                        rotationX = animatedRotX
                        rotationY = animatedRotY
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .then(gestureModifier),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    width = if (isPressed || isDragging) 1.4.dp else 1.dp,
                    color = when {
                        isPressed || isDragging -> Color(0xFFE0C078).copy(alpha = 0.72f)
                        effectiveIsLight -> Color.Black.copy(alpha = 0.14f)
                        else -> Color.White.copy(alpha = 0.26f)
                    },
                ),
            ) {
                if (showFront) {
                    WalletCardFront(
                        account = account,
                        finish = finish,
                        artPrefs = artPrefs,
                        hasCustomPhoto = hasCustomPhoto,
                        balanceMinor = balanceMinor,
                        hideBalances = hideBalances,
                        textPrimary = effectiveTextPrimary,
                        textMuted = effectiveTextMuted,
                        isLightSurface = effectiveIsLight,
                        sheenBias = sheenBias,
                        rotX = animatedRotX,
                        rotY = animatedRotY,
                        subtitleOverride = subtitleOverride,
                    )
                } else {
                    WalletCardBack(
                        account = account,
                        finish = finish,
                        balanceMinor = balanceMinor,
                        hideBalances = hideBalances,
                        secondaryHint = secondaryHint,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletCardFront(
    account: AccountRecord,
    finish: CardFinishPreset,
    artPrefs: CardArtPreferences,
    hasCustomPhoto: Boolean,
    balanceMinor: Long,
    hideBalances: Boolean,
    textPrimary: Color,
    textMuted: Color,
    isLightSurface: Boolean,
    sheenBias: Float,
    rotX: Float,
    rotY: Float,
    subtitleOverride: String?,
) {
    val context = LocalContext.current
    val brand = remember(account.cardNetwork) { CardNetworkBrand.resolve(account.cardNetwork) }
    val hasCustomLogo = remember(artPrefs.logoPath) {
        !artPrefs.logoPath.isNullOrBlank() && File(artPrefs.logoPath).exists()
    }
    val institutionLabel = account.institutionName.trim().ifBlank { account.name.trim().ifBlank { "My Card" } }
    val digitsDisplay = if (account.lastFour.isNotBlank()) {
        "••••  ••••  ••••  ${account.lastFour}"
    } else {
        "••••  ••••  ${account.type.displayName.uppercase()}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Base layer: user's real card photo OR physical card brushed metallic finish
        if (hasCustomPhoto && artPrefs.photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(artPrefs.photoPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "${account.name} card photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Legibility gradient scrim matching Google Wallet photo cards
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.56f),
                                Color.Black.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.68f),
                            )
                        )
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(finish.startColor, finish.midColor, finish.endColor),
                            start = Offset.Zero,
                            end = Offset(1050f, 650f),
                        )
                    ),
            )
        }

        // Physical card architectural lines & dynamic specular tilt highlight
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!hasCustomPhoto) {
                val patternInk = if (isLightSurface) {
                    Color.Black.copy(alpha = 0.055f)
                } else {
                    Color.White.copy(alpha = 0.07f)
                }
                // Subtle globe/wave guilloche arcs typical of real payment cards
                drawCircle(
                    color = patternInk,
                    radius = size.height * 0.92f,
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
                drawCircle(
                    color = patternInk,
                    radius = size.height * 0.64f,
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = patternInk,
                    radius = size.height * 0.36f,
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    style = Stroke(width = 0.9.dp.toPx()),
                )
            }

            // Dynamic tilt-driven light reflection sweep
            val sweepCenter = size.width * (0.5f + sheenBias * 0.45f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isLightSurface) 0.30f else 0.14f),
                        Color.Transparent,
                    ),
                    start = Offset(sweepCenter - size.width * 0.38f, 0f),
                    end = Offset(sweepCenter + size.width * 0.22f, size.height),
                ),
            )
        }

        // Foreground Google Wallet content with subtle Z-parallax
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .graphicsLayer {
                    translationX = rotY.coerceIn(-24f, 24f) * 0.22f
                    translationY = -rotX.coerceIn(-18f, 18f) * 0.22f
                },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Real Bank Logo / Uploaded Brand Logo + Card Name + NFC Contactless
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (hasCustomLogo && artPrefs.logoPath != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.08f)),
                            modifier = Modifier.size(36.dp),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(artPrefs.logoPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "$institutionLabel logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    } else {
                        InstitutionMonogramSeal(
                            institutionName = institutionLabel,
                            isLightSurface = isLightSurface,
                        )
                    }

                    Column {
                        Text(
                            text = account.name.ifBlank { institutionLabel },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitleOverride ?: if (account.institutionName.isNotBlank() && account.institutionName != account.name) {
                                account.institutionName
                            } else {
                                account.type.displayName
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isLightSurface) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = account.currencyCode,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Contactless,
                        contentDescription = "Contactless NFC card",
                        tint = textPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Middle Row: Realistic EMV Metallic Chip + Live Account Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmvChipGraphic(
                    goldChip = finish.goldChip && !hasCustomPhoto,
                    modifier = Modifier.size(width = 40.dp, height = 28.dp),
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (account.type.isLiability) "OUTSTANDING" else "AVAILABLE BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = textMuted,
                    )
                    Text(
                        text = if (hideBalances) "••••••" else MoneyFormatter.formatMinor(balanceMinor, account.currencyCode),
                        style = MoneyHeroStyle.copy(fontSize = 24.sp, lineHeight = 28.sp),
                        color = textPrimary,
                        maxLines = 1,
                    )
                }
            }

            // Bottom Row: Masked Card Number + Real Card Network Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = digitsDisplay,
                    style = MoneyRowStyle.copy(
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = textPrimary.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                CardNetworkLogo(
                    brand = brand,
                    isLightCard = isLightSurface,
                )
            }
        }
    }
}

@Composable
private fun WalletCardBack(
    account: AccountRecord,
    finish: CardFinishPreset,
    balanceMinor: Long,
    hideBalances: Boolean,
    secondaryHint: String,
) {
    val brand = remember(account.cardNetwork) { CardNetworkBrand.resolve(account.cardNetwork) }
    val inkPrimary = if (finish.isLightSurface) finish.textPrimary else Color.White
    val inkMuted = if (finish.isLightSurface) finish.textMuted else Color(0xFFC4D1CE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationY = 180f }
            .background(
                Brush.linearGradient(
                    colors = listOf(finish.backColor, finish.endColor),
                    start = Offset.Zero,
                    end = Offset(800f, 500f),
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Authentic magnetic strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF0A0C0F)),
            )

            // Signature strip + security digits + account balance details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.88f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = account.institutionName.ifBlank { account.name }.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "•••",
                            style = MoneyRowStyle.copy(fontStyle = FontStyle.Italic),
                            color = Color(0xFF0F172A),
                        )
                    }
                }

                if (account.creditLimitMinor != null && account.creditLimitMinor > 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LIMIT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = inkMuted,
                        )
                        Text(
                            text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(
                                account.creditLimitMinor,
                                account.currencyCode,
                            ),
                            style = MoneyRowStyle,
                            color = inkPrimary,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "OPENING",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = inkMuted,
                        )
                        Text(
                            text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(
                                account.openingBalanceMinor,
                                account.currencyCode,
                            ),
                            style = MoneyRowStyle,
                            color = inkPrimary,
                        )
                    }
                }
            }

            // Bottom back row: current balance + tap to flip hint + network mark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlipCameraAndroid,
                        contentDescription = null,
                        tint = inkMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "$secondaryHint • ${if (hideBalances) "••••" else MoneyFormatter.formatMinor(balanceMinor, account.currencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = inkMuted,
                    )
                }
                CardNetworkLogo(brand = brand, isLightCard = finish.isLightSurface)
            }
        }
    }
}

@Composable
fun InstitutionMonogramSeal(
    institutionName: String,
    isLightSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    val initials = remember(institutionName) {
        val parts = institutionName.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "SP"
        }
    }
    Surface(
        modifier = modifier.size(34.dp),
        shape = CircleShape,
        color = if (isLightSurface) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.18f),
        border = BorderStroke(
            0.8.dp,
            if (isLightSurface) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.32f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                ),
                color = if (isLightSurface) Color(0xFF141920) else Color.White,
            )
        }
    }
}

@Composable
fun EmvChipGraphic(
    goldChip: Boolean,
    modifier: Modifier = Modifier,
) {
    val fillStart = if (goldChip) Color(0xFFF3DC9B) else Color(0xFFE7ECF2)
    val fillEnd = if (goldChip) Color(0xFFC29B44) else Color(0xFFB1BBC7)
    val traceColor = if (goldChip) Color(0xFF705315) else Color(0xFF475569)

    Canvas(modifier = modifier) {
        val r = 6.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(fillStart, fillEnd),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(r, r),
        )
        drawRoundRect(
            color = traceColor.copy(alpha = 0.45f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 0.9.dp.toPx()),
        )
        // Micro-circuit contact traces
        val strokeW = 0.9.dp.toPx()
        val lineAlpha = traceColor.copy(alpha = 0.52f)
        drawLine(
            color = lineAlpha,
            start = Offset(size.width * 0.34f, 0f),
            end = Offset(size.width * 0.34f, size.height),
            strokeWidth = strokeW,
        )
        drawLine(
            color = lineAlpha,
            start = Offset(size.width * 0.66f, 0f),
            end = Offset(size.width * 0.66f, size.height),
            strokeWidth = strokeW,
        )
        drawLine(
            color = lineAlpha,
            start = Offset(0f, size.height * 0.36f),
            end = Offset(size.width * 0.34f, size.height * 0.36f),
            strokeWidth = strokeW,
        )
        drawLine(
            color = lineAlpha,
            start = Offset(0f, size.height * 0.66f),
            end = Offset(size.width * 0.34f, size.height * 0.66f),
            strokeWidth = strokeW,
        )
        drawLine(
            color = lineAlpha,
            start = Offset(size.width * 0.66f, size.height * 0.36f),
            end = Offset(size.width, size.height * 0.36f),
            strokeWidth = strokeW,
        )
        drawLine(
            color = lineAlpha,
            start = Offset(size.width * 0.66f, size.height * 0.66f),
            end = Offset(size.width, size.height * 0.66f),
            strokeWidth = strokeW,
        )
        drawRoundRect(
            color = lineAlpha,
            topLeft = Offset(size.width * 0.34f, size.height * 0.28f),
            size = Size(size.width * 0.32f, size.height * 0.44f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = strokeW),
        )
    }
}

/**
 * Crisp, recognizable vector-rendered Card Network Logos (Visa, Mastercard interlocking circles,
 * American Express badge, Discover orange circle, RuPay, UnionPay, and Bank seal).
 */
@Composable
fun CardNetworkLogo(
    brand: CardNetworkBrand,
    isLightCard: Boolean,
    modifier: Modifier = Modifier,
) {
    when (brand) {
        CardNetworkBrand.MASTERCARD -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Canvas(modifier = Modifier.size(width = 44.dp, height = 26.dp)) {
                    val radius = size.height * 0.48f
                    val centerY = size.height / 2f
                    val leftX = size.width * 0.36f
                    val rightX = size.width * 0.64f
                    drawCircle(
                        color = Color(0xFFEB001B),
                        radius = radius,
                        center = Offset(leftX, centerY),
                    )
                    drawCircle(
                        color = Color(0xFFF79E1B).copy(alpha = 0.92f),
                        radius = radius,
                        center = Offset(rightX, centerY),
                    )
                    // Center overlapping orange lens accent
                    drawOval(
                        color = Color(0xFFFF5F00),
                        topLeft = Offset((leftX + rightX) / 2f - radius * 0.38f, centerY - radius * 0.80f),
                        size = Size(radius * 0.76f, radius * 1.60f),
                    )
                }
                Text(
                    text = "mastercard",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                    ),
                    color = if (isLightCard) Color(0xFF1F2937) else Color.White.copy(alpha = 0.92f),
                )
            }
        }

        CardNetworkBrand.VISA -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Canvas(modifier = Modifier.size(width = 10.dp, height = 16.dp)) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.22f)
                        lineTo(size.width * 0.85f, size.height * 0.22f)
                        lineTo(size.width, size.height * 0.55f)
                        lineTo(size.width * 0.3f, size.height * 0.55f)
                        close()
                    }
                    drawPath(path, color = Color(0xFFF7B600))
                }
                Text(
                    text = "VISA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 21.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = if (isLightCard) Color(0xFF1434CB) else Color.White,
                )
            }
        }

        CardNetworkBrand.AMEX -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(5.dp),
                color = Color(0xFF006FCF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "AMERICAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            lineHeight = 8.sp,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "EXPRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            lineHeight = 8.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }

        CardNetworkBrand.DISCOVER -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "DISC",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = if (isLightCard) Color(0xFF111827) else Color.White,
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6000)),
                )
                Text(
                    text = "VER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = if (isLightCard) Color(0xFF111827) else Color.White,
                )
            }
        }

        CardNetworkBrand.RUPAY -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "RuPay",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = if (isLightCard) Color(0xFF0F539D) else Color.White,
                )
                Canvas(modifier = Modifier.size(10.dp)) {
                    val tri1 = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.65f, size.height * 0.5f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(tri1, color = Color(0xFFF26522))
                    val tri2 = Path().apply {
                        moveTo(size.width * 0.42f, 0f)
                        lineTo(size.width, size.height * 0.5f)
                        lineTo(size.width * 0.42f, size.height)
                        close()
                    }
                    drawPath(tri2, color = Color(0xFF138808))
                }
            }
        }

        CardNetworkBrand.UNIONPAY -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(width = 4.dp, height = 13.dp).background(Color(0xFFE21836)))
                    Box(Modifier.size(width = 4.dp, height = 13.dp).background(Color(0xFF00447C)))
                    Box(Modifier.size(width = 4.dp, height = 13.dp).background(Color(0xFF007B84)))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "UnionPay",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color(0xFF111827),
                    )
                }
            }
        }

        CardNetworkBrand.BANK -> {
            Text(
                text = "WALLET",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
                color = if (isLightCard) Color(0xFF334155) else Color.White.copy(alpha = 0.85f),
                modifier = modifier,
            )
        }
    }
}

/**
 * Miniature Google Wallet card thumbnail used in Accounts list rows and pickers.
 */
@Composable
fun WalletMiniCardThumbnail(
    account: AccountRecord,
    artPrefs: CardArtPreferences,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 36.dp,
) {
    val context = LocalContext.current
    val finish = remember(account, artPrefs.finishId) {
        CardFinishPreset.resolve(account, artPrefs.finishId)
    }
    val hasCustomPhoto = remember(artPrefs.photoPath) {
        !artPrefs.photoPath.isNullOrBlank() && File(artPrefs.photoPath).exists()
    }
    val hasCustomLogo = remember(artPrefs.logoPath) {
        !artPrefs.logoPath.isNullOrBlank() && File(artPrefs.logoPath).exists()
    }
    val brand = remember(account.cardNetwork) { CardNetworkBrand.resolve(account.cardNetwork) }

    Surface(
        modifier = modifier.size(width = width, height = height),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(0.6.dp, Color.Black.copy(alpha = 0.14f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCustomPhoto && artPrefs.photoPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(artPrefs.photoPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "${account.name} thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(finish.startColor, finish.midColor, finish.endColor),
                            )
                        ),
                )
            }

            if (hasCustomLogo && artPrefs.logoPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(artPrefs.logoPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 5.dp, top = 6.dp)
                        .size(width = 10.dp, height = 7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (finish.goldChip) Color(0xFFE7CA7D) else Color(0xFFE2E8F0)),
                )
            }

            when (brand) {
                CardNetworkBrand.MASTERCARD -> {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(width = 16.dp, height = 10.dp),
                    ) {
                        val r = size.height * 0.45f
                        drawCircle(Color(0xFFEB001B), r, Offset(size.width * 0.35f, size.height / 2f))
                        drawCircle(Color(0xFFF79E1B).copy(alpha = 0.9f), r, Offset(size.width * 0.65f, size.height / 2f))
                    }
                }
                CardNetworkBrand.VISA -> {
                    Text(
                        text = "VISA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = if (finish.isLightSurface && !hasCustomPhoto) Color(0xFF1434CB) else Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                else -> {
                    if (account.lastFour.isNotBlank()) {
                        Text(
                            text = account.lastFour,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = if (finish.isLightSurface && !hasCustomPhoto) finish.textPrimary else Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
