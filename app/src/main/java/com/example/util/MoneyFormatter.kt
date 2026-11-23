package com.example.util

import com.example.model.CurrencySpec
import com.example.model.MonthPeriod
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object CurrencyCatalog {
    val supportedCurrencies: List<CurrencySpec> = listOf(
        CurrencySpec("USD", "$", "US Dollar", 2, 1.0, "Base Currency", "2026-11-23"),
        CurrencySpec("EUR", "€", "Euro", 2, 1.06, "ECB Reference (Preview)", "2026-11-23"),
        CurrencySpec("GBP", "£", "British Pound", 2, 1.26, "BOE Reference (Preview)", "2026-11-23"),
        CurrencySpec("BDT", "৳", "Bangladeshi Taka", 2, 0.0084, "Central Rate (Preview)", "2026-11-23"),
        CurrencySpec("CAD", "CA$", "Canadian Dollar", 2, 0.72, "BOC Reference (Preview)", "2026-11-23"),
        CurrencySpec("AUD", "A$", "Australian Dollar", 2, 0.65, "RBA Reference (Preview)", "2026-11-23"),
        CurrencySpec("JPY", "¥", "Japanese Yen", 0, 0.0065, "BOJ Reference (Preview)", "2026-11-23"),
        CurrencySpec("INR", "₹", "Indian Rupee", 2, 0.0118, "RBI Reference (Preview)", "2026-11-23"),
        CurrencySpec("SGD", "S$", "Singapore Dollar", 2, 0.74, "MAS Reference (Preview)", "2026-11-23"),
        CurrencySpec("CHF", "CHF", "Swiss Franc", 2, 1.13, "SNB Reference (Preview)", "2026-11-23"),
        // Intentionally includes a currency without an available rate to demonstrate honest missing-rate UI
        CurrencySpec("TRY", "₺", "Turkish Lira", 2, null, null, null),
    )

    fun findByCode(code: String): CurrencySpec =
        supportedCurrencies.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies.first()
}

sealed class ConversionResult {
    data class Available(
        val convertedMinor: Long,
        val baseCurrencyCode: String,
        val rateText: String,
        val source: String,
        val dateLabel: String,
    ) : ConversionResult()

    data class Unavailable(val reason: String) : ConversionResult()
}

object MoneyFormatter {
    fun formatMinor(
        amountMinor: Long,
        currencyCode: String,
        showPositivePlus: Boolean = false,
    ): String {
        val spec = CurrencyCatalog.findByCode(currencyCode)
        val divisor = 10.0.pow(spec.minorDecimals)
        val absMajor = abs(amountMinor) / divisor
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = spec.minorDecimals
            maximumFractionDigits = spec.minorDecimals
        }
        val sign = when {
            amountMinor < 0 -> "-"
            amountMinor > 0 && showPositivePlus -> "+"
            else -> ""
        }
        return "$sign${spec.symbol}${nf.format(absMajor)}"
    }

    fun parseMajorToMinor(input: String, currencyCode: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val spec = CurrencyCatalog.findByCode(currencyCode)
        val dotIndex = cleaned.indexOf('.')
        if (dotIndex >= 0) {
            val decimalsPart = cleaned.substring(dotIndex + 1)
            if (decimalsPart.length > spec.minorDecimals) return null
        }
        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (parsed < 0.0) return null
        val multiplier = 10.0.pow(spec.minorDecimals)
        return (parsed * multiplier).roundToLong()
    }

    fun convertMinor(
        amountMinor: Long,
        fromCurrencyCode: String,
        toCurrencyCode: String,
    ): ConversionResult {
        if (fromCurrencyCode == toCurrencyCode) {
            return ConversionResult.Available(
                convertedMinor = amountMinor,
                baseCurrencyCode = toCurrencyCode,
                rateText = "1.00",
                source = "Same Currency",
                dateLabel = "Direct",
            )
        }
        val fromSpec = CurrencyCatalog.findByCode(fromCurrencyCode)
        val toSpec = CurrencyCatalog.findByCode(toCurrencyCode)
        val fromRateToUsd = fromSpec.rateToUsd
        val toRateToUsd = toSpec.rateToUsd
        if (fromRateToUsd == null || toRateToUsd == null || toRateToUsd == 0.0) {
            return ConversionResult.Unavailable("Rate unavailable for $fromCurrencyCode → $toCurrencyCode")
        }
        val fromMajor = amountMinor / 10.0.pow(fromSpec.minorDecimals)
        val usdMajor = fromMajor * fromRateToUsd
        val targetMajor = usdMajor / toRateToUsd
        val targetMinor = (targetMajor * 10.0.pow(toSpec.minorDecimals)).roundToLong()
        val effectiveRate = fromRateToUsd / toRateToUsd
        return ConversionResult.Available(
            convertedMinor = targetMinor,
            baseCurrencyCode = toCurrencyCode,
            rateText = String.format(Locale.US, "%.4f", effectiveRate),
            source = fromSpec.rateSource ?: "Reference",
            dateLabel = fromSpec.rateDate ?: "",
        )
    }
}

object DateFormatters {
    private val monthYearFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val shortMonthFmt = SimpleDateFormat("MMM yyyy", Locale.US)
    private val dateHeaderFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private val shortDateFmt = SimpleDateFormat("MMM d", Locale.US)
    private val isoDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatMonthPeriod(period: MonthPeriod): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, period.year)
            set(Calendar.MONTH, period.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFmt.format(cal.time)
    }

    fun formatShortMonth(period: MonthPeriod): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, period.year)
            set(Calendar.MONTH, period.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return shortMonthFmt.format(cal.time)
    }

    fun formatDateGroupHeader(millis: Long): String {
        return dateHeaderFmt.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        return shortDateFmt.format(Date(millis))
    }

    fun formatIsoDate(millis: Long): String {
        return isoDateFmt.format(Date(millis))
    }

    fun toDayKey(millis: Long): String = isoDateFmt.format(Date(millis))

    fun monthPeriodOf(millis: Long): MonthPeriod {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return MonthPeriod(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
        )
    }
}
