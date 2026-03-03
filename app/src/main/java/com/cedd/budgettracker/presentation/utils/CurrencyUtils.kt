package com.cedd.budgettracker.presentation.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    /**
     * Philippines locale — produces ₱ symbol with comma-thousands separator.
     * e.g.  15000.5 → "₱15,000.50"
     */
    private val phpFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    }

    fun formatPhp(amount: Double): String = phpFormatter.format(amount)

    /** Safe parse — strips commas before parsing. Returns 0.0 for blank/invalid strings. */
    fun parseAmount(raw: String): Double =
        raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    /**
     * Filters an amount text-field input to digits + at most one decimal point.
     * Call this in every amount field's onValueChange so the state stays clean.
     */
    fun cleanAmountInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' }
        val dotIdx = filtered.indexOf('.')
        return if (dotIdx >= 0)
            filtered.substring(0, dotIdx + 1) + filtered.substring(dotIdx + 1).filter { it.isDigit() }
        else
            filtered
    }
}

/**
 * VisualTransformation that displays numeric input with thousands comma separators.
 * The underlying TextField state remains as raw digits — no commas stored.
 *
 * Example: stored "10000000" → displayed "10,000,000"
 *
 * Use as a singleton (object) so Compose's equality checks skip unnecessary re-applies.
 */
object ThousandSeparatorTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw      = text.text
        val dotIndex = raw.indexOf('.')
        val intPart  = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val decPart  = if (dotIndex >= 0) raw.substring(dotIndex)    else ""

        // Insert commas every 3 digits from the right in the integer part
        val formattedInt = if (intPart.isEmpty()) ""
        else intPart.reversed().chunked(3).joinToString(",").reversed()

        val transformed = formattedInt + decPart
        val intLen      = intPart.length
        val totalCommas = if (intLen > 0) (intLen - 1) / 3 else 0

        val offsetMapping = object : OffsetMapping {

            /** Map a cursor position in the original (raw) string → transformed (comma) string. */
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= intLen) {
                    val remaining = intLen - offset
                    // Use (remaining - 1) / 3 so that when remaining is an exact multiple of 3
                    // (e.g. "100" → remaining=3) we correctly get 0 commas-to-right instead of 1,
                    // preventing a negative return value that would crash the TextField.
                    val groups    = if (remaining > 0) (remaining - 1) / 3 else 0
                    return offset + (totalCommas - groups)
                }
                // Cursor is in the decimal part — shift by the full formattedInt length
                return formattedInt.length + (offset - intLen)
            }

            /** Map a cursor position in the transformed (comma) string → original (raw) string. */
            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceAtMost(transformed.length)
                return if (clamped <= formattedInt.length) {
                    clamped - formattedInt.take(clamped).count { it == ',' }
                } else {
                    intLen + (clamped - formattedInt.length)
                }
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
