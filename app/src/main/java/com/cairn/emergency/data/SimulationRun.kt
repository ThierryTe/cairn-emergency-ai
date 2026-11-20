package com.cairn.emergency.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "simulation_run")
data class SimulationRun(
    @PrimaryKey val scenarioId: String,
    val decisions: String,
    val feedbackPending: Boolean,
)

@Dao
interface SimulationRunDao {
    @Query("SELECT * FROM simulation_run")
    suspend fun load(): List<SimulationRun>

    @Upsert
    suspend fun save(run: SimulationRun)
}
