package app.lawnchair.search.algorithms.engine.provider

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.search.algorithms.data.Calculation
import app.lawnchair.search.algorithms.data.calculator.Expressions
import app.lawnchair.search.algorithms.engine.SearchProvider
import app.lawnchair.search.algorithms.engine.SearchResult
import java.math.BigDecimal
import java.math.MathContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object CalculatorSearchProvider : SearchProvider {

    override val id: String = "calculator"

    override fun search(
        context: Context,
        query: String,
    ): Flow<List<SearchResult>> = flow {
        val legacyPrefs = PreferenceManager.getInstance(context)

        if (query.isBlank() || !legacyPrefs.searchResultCalculator.get()) {
            emit(emptyList())
            return@flow
        }

        // Try timezone conversion first
        val tzConversion = tryTimezoneConversion(query)
        if (tzConversion != null && tzConversion.isValid) {
            emit(listOf(SearchResult.Calculation(data = tzConversion)))
            return@flow
        }

        // Try unit/currency conversion next
        val conversion = tryUnitConversion(query)
        if (conversion != null && conversion.isValid) {
            emit(listOf(SearchResult.Calculation(data = conversion)))
            return@flow
        }

        val calculation = calculateEquationFromString(query)

        if (calculation.isValid) {
            val searchResult = SearchResult.Calculation(data = calculation)
            emit(listOf(searchResult))
        } else {
            emit(emptyList())
        }
    }

    private fun tryTimezoneConversion(query: String): Calculation? {
        val cleanQuery = query.trim().lowercase(java.util.Locale.getDefault())
        val tzRegex = Regex("^([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?\\s+([a-zA-Z]{3,4})\\s+(?:to|in)\\s+([a-zA-Z]{3,4})$")
        val match = tzRegex.matchEntire(cleanQuery) ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        val ampm = match.groupValues[3].trim()
        val fromTz = match.groupValues[4].trim()
        val toTz = match.groupValues[5].trim()

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null

        val offsets = mapOf(
            "utc" to 0.0, "gmt" to 0.0,
            "est" to -5.0, "edt" to -4.0,
            "cst" to -6.0, "cdt" to -5.0,
            "mst" to -7.0, "mdt" to -6.0,
            "pst" to -8.0, "pdt" to -7.0,
            "cet" to 1.0, "cest" to 2.0,
            "eet" to 2.0, "eest" to 3.0,
            "ist" to 5.5, "jst" to 9.0,
            "aest" to 10.0, "aedt" to 11.0,
            "sgt" to 8.0, "bst" to 1.0,
            "ast" to -4.0, "nst" to -3.5,
        )

        if (fromTz !in offsets || toTz !in offsets) return null

        // Normalize hour for AM/PM if provided
        if (ampm.isNotEmpty()) {
            if (hour > 12) return null
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
        }

        val sourceMinutes = hour * 60 + minute
        val fromOffsetMin = (offsets[fromTz]!! * 60).toInt()
        val toOffsetMin = (offsets[toTz]!! * 60).toInt()

        var targetMinutes = sourceMinutes - fromOffsetMin + toOffsetMin
        targetMinutes = (targetMinutes % 1440 + 1440) % 1440

        val targetHour24 = targetMinutes / 60
        val targetMin = targetMinutes % 60

        val srcAmPm = if (hour >= 12) "PM" else "AM"
        val srcHour12 = if (hour % 12 == 0) 12 else hour % 12
        val srcFormatted = "%d:%02d %s %s".format(srcHour12, minute, srcAmPm, fromTz.uppercase())

        val tgtAmPm = if (targetHour24 >= 12) "PM" else "AM"
        val tgtHour12 = if (targetHour24 % 12 == 0) 12 else targetHour24 % 12
        val tgtFormatted = "%d:%02d %s %s".format(tgtHour12, targetMin, tgtAmPm, toTz.uppercase())

        return Calculation(equation = srcFormatted, result = "= $tgtFormatted", isValid = true)
    }

    private fun tryUnitConversion(query: String): Calculation? {
        val cleanQuery = query.trim().lowercase(java.util.Locale.getDefault())
        // Regex pattern: quantity followed by source unit, "to/in", and target unit
        val regex = Regex("^([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z°\\u00b0\\s]+?)\\s+(?:to|in)\\s+([a-zA-Z°\\u00b0\\s]+)$")
        val match = regex.matchEntire(cleanQuery) ?: return null

        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val fromUnit = match.groupValues[2].trim()
        val toUnit = match.groupValues[3].trim()

        // 1. Temperature conversion
        if (isTemp(fromUnit) && isTemp(toUnit)) {
            val converted = convertTemp(value, fromUnit, toUnit) ?: return null
            val formattedResult = "%.2f %s".format(converted, toUnit.uppercase())
            val formattedEquation = "%.2f %s".format(value, fromUnit.uppercase())
            return Calculation(equation = formattedEquation, result = "= $formattedResult", isValid = true)
        }

        // 2. Currency conversion (2026 approximate rates)
        val currencyRates = mapOf(
            "usd" to 1.0,
            "eur" to 0.92,
            "gbp" to 0.78,
            "jpy" to 155.0,
            "cad" to 1.36,
            "aud" to 1.50,
            "inr" to 83.5,
            "cny" to 7.25,
            "chf" to 0.89,
            "brl" to 5.40,
        )
        if (fromUnit in currencyRates && toUnit in currencyRates) {
            val valueInUsd = value / currencyRates[fromUnit]!!
            val converted = valueInUsd * currencyRates[toUnit]!!
            val formattedResult = "%.2f %s".format(converted, toUnit.uppercase())
            val formattedEquation = "%.2f %s".format(value, fromUnit.uppercase())
            return Calculation(equation = formattedEquation, result = "= $formattedResult", isValid = true)
        }

        // 3. Length conversion
        val lengthInMeters = mapOf(
            "m" to 1.0,
            "meter" to 1.0,
            "meters" to 1.0,
            "km" to 1000.0,
            "kilometer" to 1000.0,
            "kilometers" to 1000.0,
            "cm" to 0.01,
            "centimeter" to 0.01,
            "centimeters" to 0.01,
            "mm" to 0.001,
            "millimeter" to 0.001,
            "millimeters" to 0.001,
            "mi" to 1609.34,
            "mile" to 1609.34,
            "miles" to 1609.34,
            "yd" to 0.9144,
            "yard" to 0.9144,
            "yards" to 0.9144,
            "ft" to 0.3048,
            "foot" to 0.3048,
            "feet" to 0.3048,
            "in" to 0.0254,
            "inch" to 0.0254,
            "inches" to 0.0254,
        )
        if (fromUnit in lengthInMeters && toUnit in lengthInMeters) {
            val valueInMeters = value * lengthInMeters[fromUnit]!!
            val converted = valueInMeters / lengthInMeters[toUnit]!!
            val formattedResult = "%.3f %s".format(converted, toUnit)
            val formattedEquation = "%.2f %s".format(value, fromUnit)
            return Calculation(equation = formattedEquation, result = "= $formattedResult", isValid = true)
        }

        // 4. Weight conversion
        val weightInKg = mapOf(
            "kg" to 1.0,
            "kilogram" to 1.0,
            "kilograms" to 1.0,
            "g" to 0.001,
            "gram" to 0.001,
            "grams" to 0.001,
            "lb" to 0.45359237,
            "lbs" to 0.45359237,
            "pound" to 0.45359237,
            "pounds" to 0.45359237,
            "oz" to 0.028349523,
            "ounce" to 0.028349523,
            "ounces" to 0.028349523,
        )
        if (fromUnit in weightInKg && toUnit in weightInKg) {
            val valueInKg = value * weightInKg[fromUnit]!!
            val converted = valueInKg / weightInKg[toUnit]!!
            val formattedResult = "%.3f %s".format(converted, toUnit)
            val formattedEquation = "%.2f %s".format(value, fromUnit)
            return Calculation(equation = formattedEquation, result = "= $formattedResult", isValid = true)
        }

        // 5. Volume conversion
        val volumeInLiters = mapOf(
            "l" to 1.0,
            "liter" to 1.0,
            "liters" to 1.0,
            "ml" to 0.001,
            "milliliter" to 0.001,
            "milliliters" to 0.001,
            "gal" to 3.78541,
            "gallon" to 3.78541,
            "gallons" to 3.78541,
            "qt" to 0.946353,
            "quart" to 0.946353,
            "quarts" to 0.946353,
            "pt" to 0.473176,
            "pint" to 0.473176,
            "pints" to 0.473176,
            "cup" to 0.236588,
            "cups" to 0.236588,
            "fl oz" to 0.0295735,
            "floz" to 0.0295735,
        )
        if (fromUnit in volumeInLiters && toUnit in volumeInLiters) {
            val valueInLiters = value * volumeInLiters[fromUnit]!!
            val converted = valueInLiters / volumeInLiters[toUnit]!!
            val formattedResult = "%.3f %s".format(converted, toUnit)
            val formattedEquation = "%.2f %s".format(value, fromUnit)
            return Calculation(equation = formattedEquation, result = "= $formattedResult", isValid = true)
        }

        return null
    }

    private fun isTemp(unit: String): Boolean {
        return unit == "c" || unit == "celcius" || unit == "celsius" ||
            unit == "f" || unit == "fahrenheit" ||
            unit == "k" || unit == "kelvin" ||
            unit == "°c" || unit == "°f" || unit == "°k"
    }

    private fun convertTemp(value: Double, from: String, to: String): Double? {
        val f = from.replace("°", "")
        val t = to.replace("°", "")

        val tempInCelsius = when (f) {
            "c", "celcius", "celsius" -> value
            "f", "fahrenheit" -> (value - 32.0) * 5.0 / 9.0
            "k", "kelvin" -> value - 273.15
            else -> return null
        }

        return when (t) {
            "c", "celcius", "celsius" -> tempInCelsius
            "f", "fahrenheit" -> tempInCelsius * 9.0 / 5.0 + 32.0
            "k", "kelvin" -> tempInCelsius + 273.15
            else -> null
        }
    }

    private fun calculateEquationFromString(
        query: String,
    ): Calculation {
        return try {
            val evaluatedValue = Expressions().eval(query)
            val roundedValue = evaluatedValue.round(MathContext.DECIMAL64)
            val formattedValue = roundedValue.stripTrailingZeros()
            val absoluteValue = formattedValue.abs()
            val threshold = BigDecimal("9999999999999999")

            val result = if (absoluteValue > threshold) {
                formattedValue.toString()
            } else {
                formattedValue.toPlainString()
            }

            Calculation(
                equation = query,
                result = result,
                isValid = true,
            )
        } catch (_: Exception) {
            Calculation(
                equation = "",
                result = "",
                isValid = false,
            )
        }
    }
}
