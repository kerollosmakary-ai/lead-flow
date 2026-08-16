package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Parsing fallback matching Gemini output layout if the API key is missing.
     * Extracts details using regular expressions.
     */
    fun localRegexFallback(inputText: String): StructuredLeadResponse {
        var name = "Unknown Client"
        var phone = "Unknown Phone"
        var budget = "Any"
        var propType = "Apartment"
        var loc = "Any"
        val keywords = mutableListOf<String>()
        var summary = inputText

        // Regex for phone numbers (looks for multiple digits)
        val phoneRegex = "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3,4}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{3,4}".toRegex()
        val phoneMatch = phoneRegex.find(inputText)
        if (phoneMatch != null) {
            phone = phoneMatch.value.trim()
            keywords.add("Phone: $phone")
        }

        // Try to identify a potential name (e.g. "I'm Sarah" or "Client John" or "Name: Sam")
        val namePatterns = listOf(
            "name[\\s:]+([A-Za-z\\s]{2,15})".toRegex(RegexOption.IGNORE_CASE),
            "client[\\s:]+([A-Za-z\\s]{2,15})".toRegex(RegexOption.IGNORE_CASE),
            "(?:call|called|with|for)[\\s]+([A-Z][A-Za-z]{1,10}[\\s]?[A-Z]?[a-z]*)".toRegex(),
            "([A-Z][a-z]+[\\s]+[A-Z][a-z]+)".toRegex() // capitalized pair
        )
        for (pattern in namePatterns) {
            val match = pattern.find(inputText)
            if (match != null) {
                val candidate = match.groupValues.last().trim()
                if (candidate.isNotEmpty() && !candidate.contains("phone", true) && !candidate.contains("lead", true)) {
                    name = candidate
                    break
                }
            }
        }

        // Identify property type keywords
        if (inputText.contains("villa", ignoreCase = true)) {
            propType = "Villa"
            keywords.add("Villa")
        } else if (inputText.contains("apartment", ignoreCase = true) || inputText.contains("flat", ignoreCase = true)) {
            propType = "Apartment"
            keywords.add("Apartment")
        } else if (inputText.contains("duplex", ignoreCase = true)) {
            propType = "Duplex"
            keywords.add("Duplex")
        } else if (inputText.contains("office", ignoreCase = true) || inputText.contains("commercial", ignoreCase = true)) {
            propType = "Commercial"
            keywords.add("Commercial Office")
        } else if (inputText.contains("land", ignoreCase = true) || inputText.contains("plot", ignoreCase = true)) {
            propType = "Land"
            keywords.add("Land Plot")
        }

        // Identify Locations (Common Real Estate markets or parsed tags)
        val locationList = listOf("New Cairo", "Downtown", "Beverly Hills", "Sheikh Zayed", "New Capital", "Fifth Settlement")
        for (l in locationList) {
            if (inputText.contains(l, ignoreCase = true)) {
                loc = l
                keywords.add(l)
                break
            }
        }

        // Identify Budget (e.g., "$500,000", "500k", "3 million", "3M")
        val budgetRegex = "(?:\\d{1,3}[,\\d]*)?\\s*(?:million|M|thousand|k|USD|EGP|\\$|LE)\\b".toRegex(RegexOption.IGNORE_CASE)
        val budgetMatch = budgetRegex.find(inputText)
        if (budgetMatch != null) {
            budget = budgetMatch.value.trim()
            keywords.add("Budget: $budget")
        }

        // Extract raw tags based on keywords mentioned
        val tags = listOf("pool", "garden", "ready", "off-plan", "3-bedroom", "luxury", "installment", "cash", "rental", "urgent")
        for (tag in tags) {
            if (inputText.contains(tag, ignoreCase = true)) {
                keywords.add(tag)
            }
        }

        if (summary.length > 80) {
            summary = summary.substring(0, 77) + "..."
        }

        return StructuredLeadResponse(
            name = name,
            phone = phone,
            budgetRange = budget,
            propertyType = propType,
            locationOfInterest = loc,
            keywords = keywords,
            notesSummary = summary
        )
    }
}
