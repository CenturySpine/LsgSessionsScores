package fr.centuryspine.lsgscores.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Centralized location for all Room database migrations.
 * Each migration should be documented with what changes it makes.
 */
object Migrations {

    /**
     * Example migration for testing infrastructure
     * This is a no-op migration that doesn't change anything
     * Can be removed once real migrations are added
     */
    val MIGRATION_5_6_TEST = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op migration for testing
            // This will be replaced with actual weather data migration
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