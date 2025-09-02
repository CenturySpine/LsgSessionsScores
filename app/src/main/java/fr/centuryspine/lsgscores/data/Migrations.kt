package fr.centuryspine.lsgscores.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Centralized location for all Room database migrations.
 * Each migration should be documented with what changes it makes.
 */
object Migrations {

    /**
     * Migration 5 to 6: Add weather data to sessions
     * Adds a nullable weatherData column to store weather information as JSON
     * captured at session creation time
     */
    val MIGRATION_5_6_TEST = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op migration for testing
            // This will be replaced with actual weather data migration
            database.execSQL("ALTER TABLE sessions ADD COLUMN weatherData TEXT")
        }
    }

    /**
     * List of all migrations to be applied to the database
     * Add new migrations here as the schema evolves
     */
    val ALL_MIGRATIONS = arrayOf(
        // Add migrations here as needed
         MIGRATION_5_6_TEST // Commented out until we implement real migration
    )
}