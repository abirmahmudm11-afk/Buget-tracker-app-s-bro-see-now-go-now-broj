package com.example.util

import com.example.model.CsvImportRow
import com.example.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

object CsvUtils {
    /**
     * Formats minor units into major decimal string matching the currency's minor decimals
     * (e.g. 1500 JPY -> "1500", 1250 USD -> "12.50").
     */
    fun formatMinorForCsv(amountMinor: Long, currencyCode: String): String {
        val spec = CurrencyCatalog.findByCode(currencyCode)
        val divisor = if (spec.minorDecimals == 0) 1.0 else 10.0.pow(spec.minorDecimals.toDouble())
        return String.format(Locale.US, "%.${spec.minorDecimals}f", amountMinor / divisor)
    }

    /**
     * Escapes a CSV field according to RFC 4180 and neutralizes formula injection prefixes
     * so spreadsheet apps do not evaluate imported text cells.
     */
    fun escapeCsvField(raw: String): String {
        val sanitized = if (raw.isNotEmpty() && raw[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            "'$raw"
        } else {
            raw
        }
        val escaped = sanitized.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Parses a single RFC 4180 CSV line supporting quoted fields and escaped quotes ("").
     */
    fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        fields.add(current.toString().trim())
        return fields
    }

    /**
     * Parses CSV statement text (header row + data rows) into validated [CsvImportRow] entries.
     */
    fun parseCsvRows(
        raw: String,
        currencyCode: String,
        destAccountId: String,
        isDuplicateCandidate: (Long, Long, String, String) -> Boolean,
    ): List<CsvImportRow> {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

        return lines.drop(1).mapIndexed { index, line ->
            val cols = parseCsvLine(line)
            val dateText = cols.getOrNull(0) ?: ""
            val desc = cols.getOrNull(1) ?: ""
            val amtText = cols.getOrNull(2) ?: ""
            val typeText = cols.getOrNull(3) ?: "Expense"

            val parsedDate = if (dateText.length == 10) {
                runCatching { sdf.parse(dateText)?.time }.getOrNull()
            } else null
            val parsedMinor = MoneyFormatter.parseMajorToMinor(amtText, currencyCode)
            val type = if (typeText.equals("Income", ignoreCase = true)) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }

            val err = when {
                parsedDate == null -> "Invalid date format (expected YYYY-MM-DD)"
                desc.isBlank() -> "Missing description"
                parsedMinor == null || parsedMinor <= 0L -> "Amount must be greater than zero"
                else -> null
            }

            val dup = if (err == null && parsedDate != null && parsedMinor != null) {
                isDuplicateCandidate(parsedDate, parsedMinor, desc, destAccountId)
            } else false

            CsvImportRow(
                rowNumber = index + 1,
                dateText = dateText,
                parsedDateMillis = parsedDate,
                description = desc.take(160),
                amountText = amtText,
                parsedAmountMinor = parsedMinor,
                type = type,
                categoryId = if (type == TransactionType.INCOME) "cat_salary" else "cat_groceries",
                isDuplicate = dup,
                errorMessage = err,
            )
        }
    }
}
