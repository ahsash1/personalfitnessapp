package com.example.personalfitnessapp.domain

import kotlin.math.roundToInt

/**
 * All weights/heights are stored in kg/cm internally. This is the
 * only place imperial conversion happens, for display and for parsing user input
 * when they've chosen lb/ft-in in Settings.
 */
object UnitConverter {
    private const val KG_PER_LB = 0.45359237f
    private const val CM_PER_INCH = 2.54f

    fun kgToLb(kg: Float): Float = kg / KG_PER_LB
    fun lbToKg(lb: Float): Float = lb * KG_PER_LB

    fun cmToInches(cm: Float): Float = cm / CM_PER_INCH
    fun inchesToCm(inches: Float): Float = inches * CM_PER_INCH

    /** Splits a total height into whole feet + remaining inches (rounded to the nearest inch). */
    fun cmToFeetAndInches(cm: Float): Pair<Int, Int> {
        val totalInches = cmToInches(cm).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return feet to inches
    }

    fun feetAndInchesToCm(feet: Int, inches: Int): Float = inchesToCm((feet * 12 + inches).toFloat())
}
