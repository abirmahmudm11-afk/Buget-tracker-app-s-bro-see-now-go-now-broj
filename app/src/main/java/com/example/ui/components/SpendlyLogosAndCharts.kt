package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AccountRecord
import com.example.model.AccountSummary
import com.example.model.AccountType
import com.example.model.BudgetSummary
import com.example.model.MonthPeriod
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.categoryColorPair
import com.example.util.CardArtPreferences
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import java.io.File
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Crisp geometric institution & payment-network logo badge sized for quick account cards.
 */
@Composable
fun InstitutionBrandLogo(
    account: AccountRecord,
    artPrefs: CardArtPreferences? = null,
    onHeroBackground: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val context = LocalContext.current
    val hasCustomLogo = remember(artPrefs?.logoPath) {
        !artPrefs?.logoPath.isNullOrBlank() && File(artPrefs!!.logoPath!!).exists()
    }
    val inst = account.institutionName.trim().lowercase()
    val net = account.cardNetwork.trim().lowercase()
    val name = account.name.trim().lowercase()

    if (hasCustomLogo && artPrefs?.logoPath != null) {
        Surface(
            modifier = modifier.size(size),
            shape = RoundedCornerShape(9.dp),
            color = Color.White,
            border = BorderStroke(0.8.dp, Color.Black.copy(alpha = 0.10f)),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(artPrefs.logoPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "${account.institutionName.ifBlank { account.name }} logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(7.dp)),
            )
        }
        return
    }

    val badgeBg = if (onHeroBackground) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.surfaceContainerLow
    val badgeBorder = if (onHeroBackground) Color.White.copy(alpha = 0.45f) else SpendlyTheme.semantic.surfaceBorder

    when {
        "visa" in net -> {
            Surface(
                modifier = modifier.height(size * 0.88f),
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "VISA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 0.6.sp,
                        ),
                        color = Color(0xFF1434CB),
                    )
                }
            }
        }
        "master" in net || net == "mc" -> {
            Surface(
                modifier = modifier.size(width = size * 1.25f, height = size * 0.88f),
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(width = 24.dp, height = 15.dp)) {
                        val r = this.size.height * 0.46f
                        drawCircle(Color(0xFFEB001B), r, Offset(this.size.width * 0.36f, this.size.height / 2f))
                        drawCircle(Color(0xFFF79E1B).copy(alpha = 0.92f), r, Offset(this.size.width * 0.64f, this.size.height / 2f))
                    }
                }
            }
        }
        "amex" in net || "american" in net || "american" in inst -> {
            Surface(
                modifier = modifier.height(size * 0.88f),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF006FCF),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.5f)),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "AMEX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
        "chase" in inst -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Geometric octagonal vault mark
                    Canvas(modifier = Modifier.size(size * 0.58f)) {
                        val w = this.size.width
                        val h = this.size.height
                        val t = w * 0.30f
                        val c = Color(0xFF117ACA)
                        drawRect(c, topLeft = Offset(t, 0f), size = Size(w - t, t * 0.82f))
                        drawRect(c, topLeft = Offset(w - t * 0.82f, t), size = Size(t * 0.82f, h - t))
                        drawRect(c, topLeft = Offset(0f, h - t * 0.82f), size = Size(w - t, t * 0.82f))
                        drawRect(c, topLeft = Offset(0f, 0f), size = Size(t * 0.82f, h - t))
                    }
                }
            }
        }
        "wise" in inst -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFF9FE870),
                border = BorderStroke(0.8.dp, Color(0xFF163300).copy(alpha = 0.16f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(size * 0.54f)) {
                        val path = Path().apply {
                            moveTo(this@Canvas.size.width * 0.15f, this@Canvas.size.height * 0.15f)
                            lineTo(this@Canvas.size.width * 0.88f, this@Canvas.size.height * 0.15f)
                            lineTo(this@Canvas.size.width * 0.56f, this@Canvas.size.height * 0.50f)
                            lineTo(this@Canvas.size.width * 0.82f, this@Canvas.size.height * 0.50f)
                            lineTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.88f)
                            lineTo(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.50f)
                            lineTo(this@Canvas.size.width * 0.15f, this@Canvas.size.height * 0.50f)
                            close()
                        }
                        drawPath(path, Color(0xFF163300))
                    }
                }
            }
        }
        "bank of america" in inst || "bofa" in inst -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(size * 0.56f)) {
                        val w = this.size.width
                        val h = this.size.height
                        drawLine(Color(0xFF012169), Offset(0f, h * 0.65f), Offset(w * 0.55f, h * 0.15f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color(0xFFE31837), Offset(w * 0.20f, h * 0.85f), Offset(w * 0.85f, h * 0.25f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color(0xFFE31837), Offset(w * 0.45f, h * 0.90f), Offset(w, h * 0.48f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
        }
        account.type == AccountType.CASH_WALLET || "cash" in name -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = account.name,
                        tint = Color(0xFF0D7A56),
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
            }
        }
        account.type == AccountType.BANK_ACCOUNT || account.type == AccountType.SAVINGS_ACCOUNT -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = account.institutionName.ifBlank { account.name },
                        tint = Color(0xFF0C5446),
                        modifier = Modifier.size(size * 0.54f),
                    )
                }
            }
        }
        else -> {
            val label = account.institutionName.ifBlank { account.name }.trim()
            val parts = label.split(" ").filter { it.isNotBlank() }
            val initials = when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                parts.isNotEmpty() -> parts[0].take(2).uppercase()
                else -> "SP"
            }
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(9.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp,
                        ),
                        color = Color(0xFF0C5446),
                    )
                }
            }
        }
    }
}

/**
 * Spendly's signature atmospheric balance hero card with custom layered sunrise/horizon landscape.
 * Bright misty spruce-ivory sunrise in Light Mode; twilight spruce in Dark Mode.
 */
@Composable
fun SpendlyHorizonHeroCard(
    profileName: String,
    balanceMinor: Long,
    monthlyIncomeMinor: Long,
    monthlyExpenseMinor: Long,
    currencyCode: String,
    hideBalances: Boolean,
    onTogglePrivacy: () -> Unit,
    onIncomeClick: () -> Unit = {},
    onExpenseClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val isDark = semantic.isDark
    val firstName = profileName.trim().substringBefore(" ").ifBlank { "there" }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour, firstName) {
        when {
            hour < 12 -> "Good morning, $firstName"
            hour < 17 -> "Good afternoon, $firstName"
            else -> "Good evening, $firstName"
        }
    }
    val isEvening = hour >= 18 || hour < 5

    val textPrimary = if (isDark) Color(0xFFF4F8F6) else Color(0xFF0E2620)
    val textMuted = if (isDark) Color(0xFFAEC4BD) else Color(0xFF42635A)
    val pillBg = if (isDark) Color(0xFF0B1613).copy(alpha = 0.56f) else Color.White.copy(alpha = 0.80f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFF0C5446).copy(alpha = 0.12f)
    val cardBorder = if (isDark) semantic.heroGold.copy(alpha = 0.28f) else Color(0xFFBBD7CC)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .touchSpringCard(pressScale = 0.985f, maxTiltDeg = 4.5f, hapticOnPress = true)
            .testTag("home_header_card"),
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color(0xFF142420) else Color(0xFFE7F3EE),
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Layered Sunrise / Twilight Landscape Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF172B26), Color(0xFF1E3731), Color(0xFF12211D))
                        } else {
                            listOf(Color(0xFFF3F8F5), Color(0xFFE2F0EA), Color(0xFFCDE3DA))
                        },
                    ),
                )

                val orbCenter = Offset(w * 0.80f, h * 0.28f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFFE5C47B).copy(alpha = 0.34f), Color.Transparent)
                        } else {
                            listOf(Color(0xFFF5C96B).copy(alpha = 0.52f), Color.Transparent)
                        },
                        center = orbCenter,
                        radius = h * 0.52f,
                    ),
                    radius = h * 0.52f,
                    center = orbCenter,
                )
                drawCircle(
                    color = if (isDark) Color(0xFFE8C97E).copy(alpha = 0.72f) else Color(0xFFF5CB6E),
                    radius = h * 0.115f,
                    center = orbCenter,
                )

                val cloudColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.58f)
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.58f, h * 0.14f),
                    size = Size(w * 0.14f, 10.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(99f, 99f),
                )
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.84f, h * 0.44f),
                    size = Size(w * 0.11f, 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(99f, 99f),
                )

                val farRidge = Path().apply {
                    moveTo(0f, h * 0.68f)
                    cubicTo(w * 0.22f, h * 0.52f, w * 0.40f, h * 0.66f, w * 0.62f, h * 0.50f)
                    cubicTo(w * 0.78f, h * 0.39f, w * 0.90f, h * 0.54f, w, h * 0.46f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    farRidge,
                    color = if (isDark) Color(0xFF1F3D35).copy(alpha = 0.75f) else Color(0xFFA9CEC0).copy(alpha = 0.55f),
                )

                val midRidge = Path().apply {
                    moveTo(0f, h * 0.80f)
                    cubicTo(w * 0.28f, h * 0.66f, w * 0.52f, h * 0.78f, w * 0.76f, h * 0.60f)
                    cubicTo(w * 0.88f, h * 0.52f, w * 0.95f, h * 0.64f, w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    midRidge,
                    color = if (isDark) Color(0xFF142B25).copy(alpha = 0.90f) else Color(0xFF7FAFA0).copy(alpha = 0.36f),
                )

                val treeColor = if (isDark) Color(0xFF0D1D19).copy(alpha = 0.92f) else Color(0xFF3E7868).copy(alpha = 0.26f)
                fun drawSpruce(cx: Float, baseY: Float, height: Float, halfW: Float) {
                    val p = Path().apply {
                        moveTo(cx, baseY - height)
                        lineTo(cx + halfW, baseY)
                        lineTo(cx - halfW, baseY)
                        close()
                    }
                    drawPath(p, treeColor)
                }
                drawSpruce(w * 0.82f, h * 0.92f, h * 0.30f, w * 0.036f)
                drawSpruce(w * 0.88f, h * 0.94f, h * 0.38f, w * 0.044f)
                drawSpruce(w * 0.94f, h * 0.96f, h * 0.26f, w * 0.032f)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (isEvening) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFE8C97E) else Color(0xFFD48806),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            onClick = onTogglePrivacy,
                            shape = CircleShape,
                            color = pillBg,
                            border = BorderStroke(0.8.dp, pillBorder),
                            modifier = Modifier
                                .pressBounce(pressScale = 0.86f, hapticOnPress = true)
                                .testTag("home_privacy_toggle"),
                        ) {
                            Box(
                                modifier = Modifier.size(30.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (hideBalances) "Show balances" else "Hide balances",
                                    tint = textPrimary.copy(alpha = 0.78f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = pillBg,
                            border = BorderStroke(0.8.dp, pillBorder),
                        ) {
                            Text(
                                text = currencyCode,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "LIQUID BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = textMuted,
                    )

                    if (hideBalances) {
                        Text(
                            text = "••••••",
                            style = MoneyHeroStyle.copy(fontSize = 34.sp, lineHeight = 40.sp),
                            color = textPrimary,
                            modifier = Modifier.testTag("home_total_balance_value"),
                        )
                    } else {
                        val formatted = MoneyFormatter.formatMinor(balanceMinor, currencyCode)
                        val dotIdx = formatted.lastIndexOf('.')
                        val annotated = if (dotIdx > 0 && formatted.length - dotIdx == 3) {
                            buildAnnotatedString {
                                append(formatted.substring(0, dotIdx))
                                withStyle(
                                    SpanStyle(
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary.copy(alpha = 0.74f),
                                    )
                                ) {
                                    append(formatted.substring(dotIdx))
                                }
                            }
                        } else {
                            buildAnnotatedString { append(formatted) }
                        }
                        Text(
                            text = annotated,
                            style = MoneyHeroStyle.copy(fontSize = 34.sp, lineHeight = 40.sp),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("home_total_balance_value"),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = pillBg,
                    border = BorderStroke(0.8.dp, pillBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .pressBounce(pressScale = 0.93f, hapticOnPress = true)
                                .clickable(onClick = onIncomeClick)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("home_monthly_income_tile"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = semantic.income,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (hideBalances) "Income ••••" else "Income +${MoneyFormatter.formatMinor(monthlyIncomeMinor, currencyCode)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.income,
                                maxLines = 1,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 12.dp)
                                .background(textMuted.copy(alpha = 0.35f)),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .pressBounce(pressScale = 0.93f, hapticOnPress = true)
                                .clickable(onClick = onExpenseClick)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("home_monthly_spending_tile"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = semantic.expense,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (hideBalances) "Expense ••••" else "Expense -${MoneyFormatter.formatMinor(monthlyExpenseMinor, currencyCode)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.expense,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact 2-column Quick Account Card with unified clean surface styling and crisp bank/card logos.
 */
@Composable
fun QuickAccountSummaryCard(
    summary: AccountSummary,
    artPrefs: CardArtPreferences?,
    isFeatured: Boolean = false,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = summary.account
    val semantic = SpendlyTheme.semantic
    val isLiability = account.type.isLiability

    val titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = when {
        isLiability && summary.calculatedBalanceMinor > 0L -> semantic.expense
        !isLiability && summary.calculatedBalanceMinor < 0L -> semantic.expense
        else -> MaterialTheme.colorScheme.onSurface
    }
    val displayBalance = if (hideBalances) {
        "••••"
    } else if (isLiability && summary.calculatedBalanceMinor > 0L) {
        "-${MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode)}"
    } else if (!isLiability && summary.calculatedBalanceMinor > 0L) {
        "+${MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode)}"
    } else {
        MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(122.dp)
            .touchSpringCard(pressScale = 0.955f, maxTiltDeg = 6.5f, hapticOnPress = true)
            .testTag("home_account_pill_${account.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (semantic.isDark) 0.92f else 0.96f),
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InstitutionBrandLogo(
                        account = account,
                        artPrefs = artPrefs,
                        onHeroBackground = false,
                        size = 30.dp,
                    )
                    if (account.lastFour.isNotBlank()) {
                        Text(
                            text = "••${account.lastFour}",
                            style = MaterialTheme.typography.labelSmall,
                            color = titleColor,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displayBalance,
                        style = MoneyRowStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = valueColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Signature Spending Analytics card featuring the semi-circular inflow-spent arc gauge,
 * Surplus/Deficit status badge, Inflow/Outflow pills, and ranked #1/#2/#3 category rows.
 */
@Composable
fun SpendingAnalyticsArcCard(
    period: MonthPeriod,
    incomeMinor: Long,
    expenseMinor: Long,
    currencyCode: String,
    slices: List<CategoryChartSlice>,
    hideBalances: Boolean,
    onCategoryClick: (String) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val netMinor = incomeMinor - expenseMinor
    val isDeficit = netMinor < 0L
    val spentRatio = when {
        incomeMinor > 0L -> (expenseMinor.toFloat() / incomeMinor.toFloat()).coerceIn(0f, 1f)
        expenseMinor > 0L -> 1f
        else -> 0f
    }
    val spentPercentDisplay = when {
        incomeMinor > 0L -> ((expenseMinor.toFloat() / incomeMinor.toFloat()) * 100f).toInt().coerceAtMost(999)
        expenseMinor > 0L -> 100
        else -> 0
    }

    var revealed by remember(incomeMinor, expenseMinor, period) { mutableStateOf(false) }
    LaunchedEffect(incomeMinor, expenseMinor, period) { revealed = true }

    val animatedArc by animateFloatAsState(
        targetValue = if (revealed) spentRatio else 0f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "analyticsArcSweep",
    )

    val gaugeColor = when {
        isDeficit || spentRatio >= 1f -> semantic.expense
        spentRatio >= 0.80f -> semantic.warning
        else -> semantic.income
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (semantic.isDark) 0.55f else 0.9f)
    val totalExpenseForRank = remember(slices) { slices.sumOf { it.amountMinor }.coerceAtLeast(1L) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_category_pie_card"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Card Header + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Spending Analytics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Monthly net & top categories • ${DateFormatters.formatShortMonth(period)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDeficit) semantic.expenseContainer else semantic.incomeContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isDeficit) semantic.expense else semantic.income),
                        )
                        Text(
                            text = if (isDeficit) "Deficit" else "Surplus",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeficit) semantic.expense else semantic.income,
                        )
                    }
                }
            }

            // Inner Gauge Sub-Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (semantic.isDark) 0.72f else 0.75f),
                border = BorderStroke(0.8.dp, semantic.surfaceBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Semi-Circular Arc Gauge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(width = 216.dp, height = 122.dp)
                                .semantics {
                                    contentDescription = "$spentPercentDisplay percent of inflow spent"
                                },
                        ) {
                            val strokeW = 14.dp.toPx()
                            val diameter = (size.width - strokeW).coerceAtMost((size.height - strokeW) * 2f)
                            val topLeft = Offset((size.width - diameter) / 2f, strokeW / 2f + 4.dp.toPx())
                            val arcSize = Size(diameter, diameter)

                            // 210-degree arch from 165 deg
                            val startDeg = 165f
                            val totalSweep = 210f

                            drawArc(
                                color = trackColor,
                                startAngle = startDeg,
                                sweepAngle = totalSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            )

                            if (animatedArc > 0f) {
                                drawArc(
                                    color = gaugeColor,
                                    startAngle = startDeg,
                                    sweepAngle = (totalSweep * animatedArc).coerceAtLeast(6f),
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Text(
                                text = when {
                                    incomeMinor == 0L && expenseMinor == 0L -> "NO ACTIVITY YET"
                                    incomeMinor == 0L -> "NO INFLOW LOGGED"
                                    else -> "$spentPercentDisplay% OF INFLOW SPENT"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (hideBalances) {
                                    "••••"
                                } else {
                                    MoneyFormatter.formatMinor(netMinor, currencyCode, showPositivePlus = true)
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 23.sp,
                                ),
                                color = if (isDeficit) semantic.expense else semantic.income,
                            )
                        }
                    }

                    // Inflow & Outflow Pill Pair
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            onClick = onOpenReports,
                            modifier = Modifier
                                .weight(1f)
                                .pressBounce(pressScale = 0.94f, hapticOnPress = true),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, semantic.surfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Inflow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(incomeMinor, currencyCode),
                                    style = MoneyRowStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = semantic.income,
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenReports,
                            modifier = Modifier
                                .weight(1f)
                                .pressBounce(pressScale = 0.94f, hapticOnPress = true),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, semantic.surfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Outflow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(expenseMinor, currencyCode),
                                    style = MoneyRowStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = semantic.expense,
                                )
                            }
                        }
                    }
                }
            }

            // Ranked Top Categories (#1, #2, #3...)
            if (slices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No expenses recorded for ${DateFormatters.formatMonthPeriod(period)} yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slices.take(4).forEachIndexed { idx, slice ->
                        val pct = ((slice.amountMinor.toFloat() / totalExpenseForRank.toFloat()) * 100f).toInt()
                        val resolvedIconKey = slice.iconKey.ifBlank { slice.categoryId.removePrefix("cat_") }
                        val (catTint, catBg) = categoryColorPair(resolvedIconKey, semantic.isDark)

                        Surface(
                            onClick = { onCategoryClick(slice.categoryId) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (semantic.isDark) 0.72f else 0.65f),
                            border = BorderStroke(0.8.dp, semantic.surfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .touchSpringCard(pressScale = 0.968f, maxTiltDeg = 4.5f, hapticOnPress = true)
                                .minimumInteractiveComponentSize()
                                .testTag("analytics_rank_${idx + 1}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(22.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(catBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = categoryIconFor(resolvedIconKey),
                                            contentDescription = slice.label,
                                            tint = catTint,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = slice.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(slice.amountMinor, currencyCode),
                                        style = MoneyRowStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onOpenReports,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .pressBounce(pressScale = 0.92f, hapticOnPress = true)
                                .testTag("home_categories_reports"),
                        ) {
                            Text(
                                text = "Full Reports",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact modal sheet opened from the header bell icon showing active budget alerts & quick preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAlertsBottomSheet(
    alerts: List<BudgetSummary>,
    hideBalances: Boolean,
    onTogglePrivacy: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = SpendlyTheme.semantic

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Ledger Alerts & Quick Tools",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.pressBounce(pressScale = 0.92f, hapticOnPress = true),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Settings")
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.8.dp, semantic.surfaceBorder),
                modifier = Modifier.pressBounce(pressScale = 0.97f, hapticOnPress = true),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePrivacy() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Masking", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Hide balances across dashboard cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = hideBalances, onCheckedChange = { onTogglePrivacy() })
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (alerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.incomeContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "All active budgets are within healthy thresholds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = semantic.income,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            } else {
                alerts.forEach { b ->
                    val isOver = b.isOverspent
                    Surface(
                        onClick = {
                            onDismiss()
                            onOpenBudgets()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOver) semantic.expenseContainer else semantic.warningContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressBounce(pressScale = 0.96f, hapticOnPress = true),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = if (isOver) "Over Budget: ${b.budget.name}" else "Approaching Limit: ${b.budget.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isOver) semantic.expense else semantic.warning,
                            )
                            Text(
                                text = "${(b.progress * 100).toInt()}% used • Limit ${MoneyFormatter.formatMinor(b.budget.limitMinor, b.budget.currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressBounce(pressScale = 0.95f, hapticOnPress = true),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Done")
            }
        }
    }
}
