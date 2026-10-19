package com.example.personalfitnessapp.domain

/**
 * Evidence-based training reference the coach reasons from (current hypertrophy/strength
 * consensus). Dense on purpose: it is resent on every tool round, and the free Groq models
 * have small per-minute token limits.
 */
object CoachKnowledge {
    val TEXT = """
        TRAINING KNOWLEDGE (adapt to the user's data and level)
        - Volume: hard sets (within ~3 reps of failure) per muscle per week: beginners 6-10, most lifters 10-20, over 2-3 sessions; returns diminish past ~6-10 hard sets for one muscle in one session, so split bigger volumes across days.
        - Effort: working sets at 0-3 RIR (RPE 10=0 RIR, 9=1, 8=2). Isolation/machines can hit failure; heavy squat/deadlift/bench stay 1-3 RIR.
        - Reps: 5-30 all grow muscle near failure. Defaults: big compounds 5-10, other compounds 8-12, isolation 10-20, laterals/calves/rear delts 12-25. Strength: 1-6 reps at 75-90% 1RM, 3-5 min rest.
        - Rest: compounds 2-4 min, isolation 1-2 min; never rush rest at the cost of reps.
        - Progression: double progression in a range (e.g. 8-12). All sets at the top with good form -> add the smallest step (barbell/machine ~2.5 kg, dumbbells 1-2 kg each) and restart at the bottom. Below the range two sessions running -> drop 5-10% or fix sleep/food/fatigue. e1RM (Epley) = weight x (1 + reps/30), only from sets of 10 reps or fewer (it is unreliable above that).
        - Beginners add load almost every session for months; intermediates week to week.
        - Recovery (studies): upper body and small muscles are typically back within 24-48 h, legs up to 72 h; taking sets to failure adds ~24 h. Training a muscle on consecutive days is fine at moderate volume.
        - Program: each muscle 2x/week. Full body 3x (beginners), upper/lower 4x, push/pull/legs 5-6x. Compounds first, then isolation; 4-7 exercises, 12-25 sets, 45-75 min.
        - Warm-up: 5 min easy cardio, then ramp the first big lift ~40%x8, 60%x5, 80%x2-3.
        - Deloads: no fixed schedule is proven; deload when several lifts stall and joints/motivation suffer (many coaches plan one every 4-8 weeks): one week, half the sets, same weights.
        - Plateaus: sleep 7-9 h, protein 1.6-2.2 g/kg/day, small surplus (~200-300 kcal) to grow, consistent technique, change rep range/variation after 6-12 stalled weeks.
        - Fat loss: 300-500 kcal deficit (0.5-1% bodyweight/week), keep lifting heavy, protein high. Moderate cardio doesn't kill gains; keep hard cardio away from leg day.
        - Stretch matters: loaded lengthened positions grow muscle well (incline curls, overhead triceps extensions, RDLs, deep squats/leg press, flys, seated leg curls, paused deep calf raises). Machines/cables build muscle as well as free weights.
        - Chest - incline 30-45 deg for upper chest; shoulder blades back/down, elbows 45-70 deg, flys/pec deck for stretch. Back - lats: elbows drive down to the hips (pulldowns, pull-ups, low/single-arm rows); upper back/rear delts: elbows wide and high, face pulls, reverse flys. Shoulders - presses cover front delts; side delts need lateral raises (lead with elbows, slight lean, to shoulder height). Arms - incline curls bias biceps long head, preacher the short head, hammers the brachialis; overhead extensions build the triceps long head, pushdowns/dips the rest. Legs - quads: squat, leg press, hack squat, extensions; hamstrings need a hinge (RDL) and a curl (seated is best); glutes: hip thrusts, deep squats/lunges; calves: full stretch, 1-2 s pause.
        - Cues - squat: brace, knees over toes, depth while neutral. Deadlift: bar over mid-foot, lats tight, push the floor, hips and shoulders rise together. Bench: feet planted, shoulder blades pinched, wrists over elbows, controlled touch. OHP: glutes/abs tight, bar close to the face. Pulldown: chest up, elbows to back pockets, full stretch on top. Rows: pause at the torso, no heaving. Assisted machines: less assistance or more reps = progress.
        - Fixes - presses in the shoulders: retract blades, tuck elbows, touch lower. Lat work in the biceps: lighter, thumbless grip/straps, think elbows not hands. Lower back on rows/RDLs: brace, reduce load, chest-supported variation. DOMS is normal; sharp or joint pain means stop.
        - Answer style: direct answer first with the user's numbers (e.g. "last time 20 kg x 12, 12, 13 -> 22 kg for 8-12, or stay at 20 kg and beat 13"), then a one-line why. Plans: sets x reps, RIR, rest. Point out problems you see in their data (a muscle with no direct work, big load jumps, too much volume in one session).
    """.trimIndent()
}
