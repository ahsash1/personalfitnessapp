package com.example.personalfitnessapp.data.nutrition

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/** A packaged food's label values per 100 g, from Open Food Facts. */
data class PackagedFood(
    val barcode: String,
    val name: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val servingGrams: Float?,
)

/**
 * Open Food Facts product API v2 (world.openfoodfacts.org/api/v2/product/{barcode}.json): free,
 * no key; they ask for a descriptive User-Agent.
 */
@Singleton
class FoodLookup @Inject constructor() {
    private val http = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Result {
        data class Found(val food: PackagedFood) : Result
        data object NotFound : Result
        data class Failed(val message: String) : Result
    }

    suspend fun byBarcode(barcode: String): Result = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,serving_quantity,nutriments")
            .header("User-Agent", "PersonalFitnessApp/1.0 (private, single user)")
            .build()
        runCatching {
            http.newCall(request).execute().use { response ->
                if (response.code == 404) return@runCatching Result.NotFound
                if (!response.isSuccessful) return@runCatching Result.Failed("Food database error ${response.code}")
                parse(barcode, json.parseToJsonElement(response.body.string()).jsonObject)
            }
        }.getOrElse { Result.Failed("Couldn't reach the food database. Check your connection.") }
    }

    internal fun parse(barcode: String, root: JsonObject): Result {
        if ((root["status"] as? JsonPrimitive)?.content != "1") return Result.NotFound
        val product = root["product"] as? JsonObject ?: return Result.NotFound
        val n = product["nutriments"] as? JsonObject ?: return Result.NotFound
        fun num(key: String) = (n[key] as? JsonPrimitive)?.content?.toFloatOrNull()
        val kcal = num("energy-kcal_100g") ?: num("energy_100g")?.div(4.184f) ?: return Result.NotFound
        val name = listOfNotNull(
            (product["product_name"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
            (product["brands"] as? JsonPrimitive)?.content?.substringBefore(',')?.takeIf { it.isNotBlank() }?.let { "($it)" },
        ).joinToString(" ").ifBlank { "Barcode $barcode" }
        return Result.Found(
            PackagedFood(
                barcode = barcode,
                name = name,
                kcalPer100g = kcal,
                proteinPer100g = num("proteins_100g") ?: 0f,
                carbsPer100g = num("carbohydrates_100g") ?: 0f,
                fatPer100g = num("fat_100g") ?: 0f,
                servingGrams = (product["serving_quantity"] as? JsonPrimitive)?.content?.toFloatOrNull()?.takeIf { it > 0 },
            )
        )
    }
}
