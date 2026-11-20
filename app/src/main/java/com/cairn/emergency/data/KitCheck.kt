package com.cairn.emergency.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "kit_check")
data class KitCheck(@PrimaryKey val itemId: String, val planKey: String)

@Dao
interface KitCheckDao {
    @Query("SELECT * FROM kit_check")
    fun observe(): Flow<List<KitCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun check(check: KitCheck)

    @Query("DELETE FROM kit_check WHERE itemId = :itemId AND planKey = :planKey")
    suspend fun uncheck(itemId: String, planKey: String)
}
