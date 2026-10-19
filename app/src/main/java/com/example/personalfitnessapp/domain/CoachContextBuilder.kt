package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.UserProfileEntity
import java.time.Instant
import java.time.ZoneId

data class CoachContextInput(
    val profile: UserProfileEntity?,
    val trainingLines: List<String>,
    val recovery: List<MuscleRecovery>,
    val routineLines: List<String>,
    val scheduleLines: List<String>,
)

/**
 * Builds the compact system instruction sent with every coach request.
 * Deliberately limited to a summarized profile: no name, no sex, no raw history, since
 * free-tier data may be used by Google.
 */
object CoachContextBuilder {
    private const val MAX_TRAINING_EXERCISES = 12

    private val RULES = """
        You are a concise, practical strength-training coach inside a personal fitness app.
        Rules:
        - Answer any fitness question directly, using general training knowledge plus the user's data: technique, muscle engagement, programming, equipment, recovery. Do not deflect just because no tool covers it.
        - When the user feels an exercise in the wrong place (e.g. shoulders during a lat pulldown), troubleshoot it: call get_exercise_details for the target muscles and form cues, name the 2-4 most likely causes for that exercise, give one specific cue for each, and suggest one thing to try on the next set. Ask a short clarifying question only if the answer would change your advice.
        - Tell apart "feeling it in the wrong muscle" (burn, fatigue, tension: coach the technique) from pain (sharp, pinching, joint pain, numbness, or pain that lingers after training). For pain or injury, tell the user to stop that movement and get it checked instead of pushing through. If it is unclear which one it is, give the technique fix and ask which it is.
        - You are not a medical professional and cannot diagnose.
        - Weights are in kg. Keep answers short and actionable; go longer only when the user asks for a plan or an explanation.
        - Only recommend exercises that exist in the app's database. Look them up with search_exercises and mention them as [Exercise Name](exercise:EXERCISE_ID) using the real ID.
        - For "what should I hit today" or sets/reps questions, base targets on the user's real numbers below (LAST WORKOUTS, CURRENT WORKOUT, or get_workout_history): if every working set last time reached the top of the rep range, add the smallest weight step (about 2.5 kg on barbells/machines, 1-2 kg per dumbbell); otherwise keep the weight and aim for one more rep. Say which session the numbers come from. Never invent numbers when there is no data; give a rep range and RPE instead.
        - When CURRENT WORKOUT is shown, questions like "how many more sets", "what weight next" or "what should I do next" are about that workout: use its sets, what is done and what is left.
        - For "what should I train today", look at LAST WORKOUTS, recovery and the weekly plan, and suggest muscles or a routine that the RECOVERY section shows as ready.
        - To suggest a routine, call propose_routine. It only shows the user a proposal; nothing is saved until they confirm it, so never say a routine was saved.
        - Use get_exercise_details, get_workout_history, get_personal_records, get_recovery_status and get_current_plan for details not in the summary below.
    """.trimIndent()

    fun build(input: CoachContextInput): String = buildString {
        appendLine(RULES)
        appendLine()
        appendLine(CoachKnowledge.TEXT)
        appendLine()
        appendLine("USER PROFILE")
        val p = input.profile
        if (p == null) {
            appendLine("- Not set up yet.")
        } else {
            appendLine("- Age ${p.age}, ${p.heightCm.toInt()} cm, ${"%.1f".format(p.weightKg)} kg")
            appendLine("- Experience: ${p.experienceLevel.name.lowercase()}, goal: ${p.goal.name.lowercase()}, ${p.daysPerWeek} days/week")
            appendLine("- Injuries/limitations: ${p.injuriesOrLimitations.ifBlank { "none reported" }}")
        }
        appendLine()
        appendLine("RECENT TRAINING (last 4 weeks)")
        appendLines(input.trainingLines, empty = "- No workouts logged.")
        appendLine()
        appendLine("RECOVERY")
        val notReady = input.recovery.filter { it.status != RecoveryStatus.READY }
        if (notReady.isEmpty()) {
            appendLine("- All muscles ready.")
        } else {
            notReady.forEach { appendLine("- ${it.muscle.displayName}: ${it.status.name.lowercase()}") }
            appendLine("- All other muscles ready.")
        }
        appendLine()
        appendLine("ROUTINES")
        appendLines(input.routineLines, empty = "- None saved.")
        appendLine()
        appendLine("WEEKLY PLAN")
        appendLines(input.scheduleLines, empty = "- Nothing scheduled.")
    }.trimEnd()

    /** One line per exercise, most-trained first, capped so the context stays small. */
    fun trainingSummary(
        setLogs: List<SetLogEntity>,
        exerciseNames: Map<String, String>,
        zone: ZoneId,
    ): List<String> = setLogs
        .groupBy { it.exerciseId }
        .filterKeys { it in exerciseNames }
        .entries
        .sortedByDescending { (_, sets) -> sets.map { it.sessionId }.distinct().size }
        .take(MAX_TRAINING_EXERCISES)
        .map { (exerciseId, sets) ->
            val sessions = sets.map { it.sessionId }.distinct().size
            val last = Instant.ofEpochMilli(sets.maxOf { it.completedAt }).atZone(zone).toLocalDate()
            val best = sets.maxWith(compareBy<SetLogEntity> { it.weightKg }.thenBy { it.reps })
            "- ${exerciseNames.getValue(exerciseId)} (id $exerciseId): $sessions sessions, last $last, " +
                "best set ${formatKg(best.weightKg)} kg x ${best.reps}"
        }

    private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

    private fun StringBuilder.appendLines(lines: List<String>, empty: String) {
        if (lines.isEmpty()) appendLine(empty) else lines.forEach { appendLine(it) }
    }
}
