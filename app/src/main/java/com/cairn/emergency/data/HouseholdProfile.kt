package com.cairn.emergency.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "household_profile")
data class HouseholdProfile(
    @PrimaryKey val id: Int = 1,
    val members: String = "",
    val medicalNeeds: String = "",
    val pets: String = "",
    val housing: String = "",
    val accessibility: String = "",
    val originalDescription: String = "",
    val adults: Int? = null,
    val children: Int? = null,
    val infants: Int? = null,
    val petCount: Int? = null,
)

@Dao
interface HouseholdProfileDao {
    @Query("SELECT * FROM household_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): HouseholdProfile?

    @Query("SELECT * FROM household_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<HouseholdProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: HouseholdProfile)
}

@Database(entities = [HouseholdProfile::class, KitCheck::class, SimulationRun::class], version = 4, exportSchema = false)
abstract class CairnDatabase : RoomDatabase() {
    abstract fun householdProfileDao(): HouseholdProfileDao
    abstract fun kitCheckDao(): KitCheckDao
    abstract fun simulationRunDao(): SimulationRunDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE household_profile ADD COLUMN adults INTEGER")
                db.execSQL("ALTER TABLE household_profile ADD COLUMN children INTEGER")
                db.execSQL("ALTER TABLE household_profile ADD COLUMN infants INTEGER")
                db.execSQL("ALTER TABLE household_profile ADD COLUMN petCount INTEGER")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE kit_check (itemId TEXT NOT NULL PRIMARY KEY, planKey TEXT NOT NULL)")
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE simulation_run (scenarioId TEXT NOT NULL PRIMARY KEY, decisions TEXT NOT NULL, feedbackPending INTEGER NOT NULL)")
            }
        }

        @Volatile private var instance: CairnDatabase? = null

        fun get(context: Context): CairnDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CairnDatabase::class.java, "cairn.db")
                .addMigrations(migration1To2, migration2To3, migration3To4)
                .build()
                .also { instance = it }
        }
    }
}
