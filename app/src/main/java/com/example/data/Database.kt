package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PetListingDao {
    // --- Pet Listings ---
    @Query("SELECT * FROM pet_listings ORDER BY timestamp DESC")
    fun getAllListings(): Flow<List<PetListing>>

    @Query("SELECT * FROM pet_listings WHERE category = :category ORDER BY timestamp DESC")
    fun getListingsByCategory(category: String): Flow<List<PetListing>>

    @Query("SELECT * FROM pet_listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: Int): PetListing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: PetListing)

    @Query("DELETE FROM pet_listings WHERE id = :id")
    suspend fun deleteListingById(id: Int)

    // --- Bookmarking / Favorites ---
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteItem>>

    @Query("SELECT COUNT(*) FROM favorites WHERE id = :id")
    fun isFavorite(id: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteItem)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)
}

@Database(entities = [PetListing::class, FavoriteItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetListingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pet_market_database"
                )
                // Fallback to destructive migration to simplify development schema changes
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
