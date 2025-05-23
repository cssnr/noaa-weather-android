package org.cssnr.noaaweather.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.cssnr.noaaweather.R
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.util.Locale

class WeatherApi(val context: Context) {

    val api: ApiService

    private lateinit var cookieJar: SimpleCookieJar
    private lateinit var client: OkHttpClient

    init {
        api = createRetrofit().create(ApiService::class.java)
    }

    //suspend fun getPoints(latitude: Double, longitude: Double): Response<PointData> {
    //    return api.getPointData(latitude, longitude)
    //}

    suspend fun getLatest(station: String): Response<ObservationResponse> {
        return api.getObservationData(station)
    }

    suspend fun getStationFromPoint(
        latitude: Double,
        longitude: Double
    ): Response<ObservationStationsResponse>? {
        Log.d("getStationFromPoint", "$latitude / $longitude")
        val formatted = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        Log.d("getStationFromPoint", "formatted: $formatted")
        val pointResponse = api.getPointData(formatted)
        Log.d("getStationFromPoint", "pointResponse: $pointResponse")
        val pointData = pointResponse.body()
        Log.d("getStationFromPoint", "pointData: $pointData")
        if (pointData != null) {
            val stationsResponse = api.getStationData(
                pointData.properties.gridId,
                "${pointData.properties.gridX},${pointData.properties.gridY}"
            )
            Log.d("getStationFromPoint", "stationsResponse: $stationsResponse")
            return stationsResponse
        }
        return null // TODO: not this...
    }

    @JsonClass(generateAdapter = true)
    data class PointData(
        val properties: Properties
    ) {
        @JsonClass(generateAdapter = true)
        data class Properties(
            val gridId: String,
            val gridX: Int,
            val gridY: Int
        )
    }

    @JsonClass(generateAdapter = true)
    data class ObservationStationsResponse(
        val features: List<Feature>
    ) {
        @JsonClass(generateAdapter = true)
        data class Feature(
            val id: String,
            val geometry: Geometry,
            val properties: Properties
        )

        @JsonClass(generateAdapter = true)
        data class Geometry(
            val type: String,
            val coordinates: List<Double>
        )

        @JsonClass(generateAdapter = true)
        data class Properties(
            val stationIdentifier: String,
            val name: String,
            val elevation: Elevation,
            val timeZone: String,
            val forecast: String? = null,
            val county: String? = null,
            val fireWeatherZone: String? = null
        )

        @JsonClass(generateAdapter = true)
        data class Elevation(
            val unitCode: String,
            val value: Double
        )
    }


    @JsonClass(generateAdapter = true)
    data class ObservationResponse(
        val properties: Properties
    ) {
        @JsonClass(generateAdapter = true)
        data class Properties(
            val station: String,
            val timestamp: String,
            val rawMessage: String?,
            val textDescription: String?,
            val icon: String?,
            val temperature: Value?,
            val dewpoint: Value?,
            val windDirection: Value?,
            val windSpeed: Value?,
            val windGust: Value?,
            val barometricPressure: Value?,
            val seaLevelPressure: Value?,
            val visibility: Value?,
            val relativeHumidity: Value?,
            val cloudLayers: List<CloudLayer>?
        ) {
            @JsonClass(generateAdapter = true)
            data class Value(
                val unitCode: String,
                val value: Double?
            )

            @JsonClass(generateAdapter = true)
            data class CloudLayer(
                val base: Value?,
                val amount: String?
            )
        }
    }


    interface ApiService {
        @GET("gridpoints/{gridId}/{gridXY}/stations")
        suspend fun getStationData(
            @Path("gridId") gridId: String,
            @Path("gridXY") gridXy: String
        ): Response<ObservationStationsResponse>

        @GET("points/{latitudeLongitude}")
        suspend fun getPointData(
            @Path("latitudeLongitude") latitudeLongitude: String,
        ): Response<PointData>

        @GET("stations/{stationId}/observations/latest")
        suspend fun getObservationData(
            @Path("stationId") stationId: String,
        ): Response<ObservationResponse>
    }

    private fun createRetrofit(): Retrofit {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        Log.d("createRetrofit", "versionName: $versionName")
        val githubUrl = context.getString(R.string.github_url)
        val userAgent = "NOAAWeather Android/${versionName} - $githubUrl"
        Log.d("createRetrofit", "userAgent: $userAgent")

        val cacheDirectory = File(context.cacheDir, "weather_cache")
        val cache = Cache(cacheDirectory, 50 * 1024 * 1024)

        cookieJar = SimpleCookieJar()
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
                chain.proceed(request)
            }
            .cache(cache)
            .build()

        val moshi = Moshi.Builder().build()
        return Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }

    inner class SimpleCookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }

        //fun setCookie(url: HttpUrl, rawCookie: String) {
        //    val cookies = Cookie.parseAll(url, Headers.headersOf("Set-Cookie", rawCookie))
        //    cookieStore[url.host] = cookies
        //}
    }
}
