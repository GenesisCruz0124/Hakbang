package ph.hakbang.app.util

/** Pure calculation helpers, kept separate so calorie/distance formulas stay easy to tune. */
object StepCalculations {

    /**
     * Calories per step constant. Tunable: calories ≈ steps × CALORIES_PER_STEP_FACTOR × (weightKg / REFERENCE_WEIGHT_KG).
     * The factor 0.04 and reference weight of 70kg approximate ~0.04 kcal per step for an average adult.
     */
    private const val CALORIES_PER_STEP_FACTOR = 0.04
    private const val REFERENCE_WEIGHT_KG = 70.0

    fun distanceMeters(steps: Int, strideMeters: Double): Double = steps * strideMeters

    fun distanceKm(steps: Int, strideMeters: Double): Double = distanceMeters(steps, strideMeters) / 1000.0

    fun calories(steps: Int, weightKg: Double): Double =
        steps * CALORIES_PER_STEP_FACTOR * (weightKg / REFERENCE_WEIGHT_KG)
}
