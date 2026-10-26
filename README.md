# Hevy clone w/ calorie tracking

My own Android workout tracker. The layout and flow copy Hevy (which I used before), with the
social and paid stuff left out, plus a calorie tracker and an AI coach. It's built for one person
(me), works offline, and keeps everything on the phone.

Not affiliated with Hevy. No Hevy images, icons or code are used.

## What it does

**Workouts**
- Log sets with weight/reps, warm-ups, drop sets, supersets, rest timer and plate maths
- Routines, a weekly schedule, and timed circuits (e.g. 50 s work / 10 s rest) with an interval player
- Cardio with treadmill incline and bike resistance
- Progressive overload suggestions, PRs and estimated 1RM, exercise history and charts
- Recovery body map based on published recovery studies
- Imports a Hevy CSV export

**Food**
- Log food by text or photo (AI estimate, with notes), barcode (Open Food Facts) or by hand
- Calories, protein, carbs and fat against targets that follow your goal
- Calories burned: resting (Mifflin-St Jeor), everyday movement, steps from Samsung Health
  (via Health Connect), workouts and digestion, with nothing counted twice
- Daily balance that locks in at midnight, plus a 7-day view
- Tell the AI what changed ("only ate half") and it fixes the logged entries

**Coach**
- Chat with an AI coach that can see your training history and the workout you're doing

## Stack

Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Coroutines/Flow, OkHttp, Health
Connect, ML Kit code scanner. minSdk 26.

## Running it

1. Open the project in Android Studio.
2. Add your API keys to `local.properties` (not committed):
   ```
   GEMINI_API_KEY=...        # required for AI features
   GROQ_API_KEY=...          # optional backup
   OPENROUTER_API_KEY=...    # optional backup
   ```
3. Run on a phone. For steps, allow Samsung Health to share Steps with Health Connect.

Everything except the AI features works without keys.

## Credits

- Exercises: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (public domain)
- Food data: [Open Food Facts](https://world.openfoodfacts.org/) (ODbL)
- Calorie maths: 2024 Compendium of Physical Activities, ACSM walking/running equations,
  Mifflin-St Jeor
