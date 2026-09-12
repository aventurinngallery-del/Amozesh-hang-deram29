package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingTrackDao {
    @Query("SELECT * FROM recording_tracks ORDER BY createdAt DESC")
    fun getAllRecordingTracks(): Flow<List<RecordingTrackEntity>>

    @Query("SELECT * FROM recording_tracks WHERE id = :id LIMIT 1")
    suspend fun getRecordingTrackById(id: String): RecordingTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordingTrack(track: RecordingTrackEntity)

    @Query("DELETE FROM recording_tracks WHERE id = :id")
    suspend fun deleteRecordingTrackById(id: String)

    @Query("DELETE FROM recording_tracks")
    suspend fun deleteAllRecordingTracks()
}
