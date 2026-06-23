package com.focusisland.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers

@JsonClass(generateAdapter = true)
data class RadioStationResponse(
    @Json(name = "stationuuid") val stationuuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url_resolved") val urlResolved: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "homepage") val homepage: String?,
    @Json(name = "favicon") val favicon: String?,
    @Json(name = "tags") val tags: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "clickcount") val clickcount: Int?,
    @Json(name = "votes") val votes: Int?
)

interface RadioBrowserApi {
    @Headers("User-Agent: FocusFlow/1.0")
    @GET("stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("country") country: String? = null,
        @Query("limit") limit: Int = 40,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<RadioStationResponse>

    @Headers("User-Agent: FocusFlow/1.0")
    @GET("stations/topclick")
    suspend fun getTopClick(
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<RadioStationResponse>

    @Headers("User-Agent: FocusFlow/1.0")
    @GET("stations/topvote")
    suspend fun getTopVote(
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<RadioStationResponse>
}

object RadioBrowserClient {
    private const val BASE_URL = "https://de1.api.radio-browser.info/json/"

    val api: RadioBrowserApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
    }
}
