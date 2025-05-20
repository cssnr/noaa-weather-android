package org.cssnr.noaaweather.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Dao
interface StationDao {
    //@Query("SELECT * FROM WeatherStation")
    //fun getAll(): List<WeatherStation>
    @Query("SELECT * FROM WeatherStation ORDER BY ROWID")
    fun getAll(): List<WeatherStation>

    @Query("SELECT * FROM WeatherStation WHERE active = 1 LIMIT 1")
    fun getActive(): WeatherStation?

    @Query("SELECT * FROM WeatherStation WHERE stationId = :stationId LIMIT 1")
    fun getById(stationId: String): WeatherStation?

    @Upsert
    fun add(station: WeatherStation)

    @Query("UPDATE WeatherStation SET active = 0 WHERE active = 1")
    fun deactivateAllStations()

    @Query("UPDATE WeatherStation SET active = 1 WHERE stationId = :stationId")
    fun activate(stationId: String)

    //@Query("UPDATE WeatherStation SET active = 1 WHERE stationId = (SELECT stationId FROM WeatherStation ORDER BY ROWID ASC LIMIT 1)")
    //fun activateFirstStation()
    @Query("UPDATE WeatherStation SET active = 1 WHERE ROWID = (SELECT ROWID FROM WeatherStation LIMIT 1)")
    fun activateFirstStation()

    @Delete
    fun delete(station: WeatherStation)
}


@Entity
data class WeatherStation(
    @PrimaryKey val stationId: String,
    val active: Boolean = false,
    val name: String = "",
    val coordinates: String? = null,
    val elevation: String? = null,
    val forecast: String? = null,
    val station: String? = null,

    var timestamp: String? = null,
    var rawMessage: String?? = null,
    var textDescription: String? = null,
    var icon: String? = null,

    var temperature: Double? = null,
    var dewpoint: Double? = null,
    var windDirection: Double? = null,
    var windSpeed: Double? = null,
    var windGust: Double? = null,
    var barometricPressure: Double? = null,
    var seaLevelPressure: Double? = null,
    var visibility: Double? = null,
    var relativeHumidity: Double? = null,
    //val cloudLayers: List<CloudLayer>? = null,
)


@Database(entities = [WeatherStation::class], version = 3)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var instance: StationDatabase? = null

        fun getInstance(context: Context): StationDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StationDatabase::class.java,
                    "station-database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
    }
}
