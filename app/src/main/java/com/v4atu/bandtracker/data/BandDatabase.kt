package com.v4atu.bandtracker.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [AccountEntity::class, EntryEntity::class], version = 1, exportSchema = true)
abstract class BandDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun entryDao(): EntryDao
}
