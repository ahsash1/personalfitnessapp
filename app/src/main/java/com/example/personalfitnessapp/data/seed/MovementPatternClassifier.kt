package com.example.personalfitnessapp.data.seed

/**
 * Best-effort movement-pattern label derived from the dataset's category/force
 * fields plus name keywords, since free-exercise-db has no such field itself.
 * Approximate by nature; exercises are user-editable so a wrong guess isn't
 * permanent.
 */
object MovementPatternClassifier {
    private val SQUAT = Regex("squat", RegexOption.IGNORE_CASE)
    private val HINGE = Regex("deadlift|rdl|good morning|hinge", RegexOption.IGNORE_CASE)
    private val LUNGE = Regex("lunge|split squat|step[- ]up", RegexOption.IGNORE_CASE)
    private val CARRY = Regex("carry|farmer", RegexOption.IGNORE_CASE)
    private val ROTATION = Regex(
        "twist|rotation|wood chop|russian twist|pallof",
        RegexOption.IGNORE_CASE,
    )
    private val ISOMETRIC = Regex("plank|isometric|static hold|wall sit", RegexOption.IGNORE_CASE)

    fun classify(name: String, category: String, force: String?): String = when {
        category == "stretching" -> "stretch"
        category == "cardio" -> "cardio"
        category == "plyometrics" -> "plyometric"
        SQUAT.containsMatchIn(name) -> "squat"
        HINGE.containsMatchIn(name) -> "hinge"
        LUNGE.containsMatchIn(name) -> "lunge"
        CARRY.containsMatchIn(name) -> "carry"
        ROTATION.containsMatchIn(name) -> "rotation"
        ISOMETRIC.containsMatchIn(name) || force == "static" -> "isometric"
        force == "push" -> "push"
        force == "pull" -> "pull"
        else -> "other"
    }
}
