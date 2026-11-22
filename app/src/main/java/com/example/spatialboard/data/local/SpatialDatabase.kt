package com.example.spatialboard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BoardEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class SpatialDatabase : RoomDatabase() {

    abstract fun boardDao(): BoardDao

    companion object {
        @Volatile
        private var instance: SpatialDatabase? = null

        fun getInstance(context: Context): SpatialDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpatialDatabase::class.java,
                    "spatial_board.db"
                ).build().also { instance = it }
            }
        }
    }
}
