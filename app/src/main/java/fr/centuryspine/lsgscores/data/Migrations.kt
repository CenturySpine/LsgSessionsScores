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
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op migration for testing
            // This will be replaced with actual weather data migration
            db.execSQL("ALTER TABLE sessions ADD COLUMN weatherData TEXT")
        }
    }

    /**
     * Migration 6 to 7: Remove foreign key constraints
     * - Removes FK constraints from holes and sessions tables
     * - Data is preserved, only constraints are removed
     * - This enables better offline/sync capabilities
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Step 1: Recreate holes table without foreign key
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS holes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                gameZoneId INTEGER NOT NULL,
                description TEXT,
                distance INTEGER,
                par INTEGER NOT NULL,
                startPhotoUri TEXT,
                endPhotoUri TEXT
            )
        """)

            db.execSQL("""
            INSERT INTO holes_new (id, name, gameZoneId, description, distance, par, startPhotoUri, endPhotoUri)
            SELECT id, name, gameZoneId, description, distance, par, startPhotoUri, endPhotoUri FROM holes
        """)

            db.execSQL("DROP TABLE holes")
            db.execSQL("ALTER TABLE holes_new RENAME TO holes")

            // Recreate index for holes
            db.execSQL("CREATE INDEX IF NOT EXISTS index_holes_gameZoneId ON holes (gameZoneId)")

            // Step 2: Recreate sessions table without foreign key
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS sessions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateTime TEXT NOT NULL,
                endDateTime TEXT,
                sessionType TEXT NOT NULL,
                scoringModeId INTEGER NOT NULL,
                gameZoneId INTEGER NOT NULL,
                comment TEXT,
                isOngoing INTEGER NOT NULL,
                weatherData TEXT
            )
        """)

            db.execSQL("""
            INSERT INTO sessions_new (id, dateTime, endDateTime, sessionType, scoringModeId, gameZoneId, comment, isOngoing, weatherData)
            SELECT id, dateTime, endDateTime, sessionType, scoringModeId, gameZoneId, comment, isOngoing, weatherData FROM sessions
        """)

            db.execSQL("DROP TABLE sessions")
            db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

            // Recreate index for sessions
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_gameZoneId ON sessions (gameZoneId)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create cities table
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS cities (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
        """)

            // Create unique index on city name
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cities_name ON cities (name)")

            // Insert Lyon as default city
            db.execSQL("INSERT INTO cities (id, name) VALUES (1, 'Lyon')")

            // Add cityId column to players table
            db.execSQL("ALTER TABLE players ADD COLUMN cityId INTEGER NOT NULL DEFAULT 1")

            // Add cityId column to game_zones table
            db.execSQL("ALTER TABLE game_zones ADD COLUMN cityId INTEGER NOT NULL DEFAULT 1")

            // Add cityId column to sessions table
            db.execSQL("ALTER TABLE sessions ADD COLUMN cityId INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Empty migration - schema already correct from MIGRATION_7_8
            // This is just to fix the version mismatch issue
        }
    }

    /**
     * List of all migrations to be applied to the database
     * Add new migrations here as the schema evolves
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_5_6_TEST,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )}