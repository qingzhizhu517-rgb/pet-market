package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "pet_listings")
@JsonClass(generateAdapter = true)
data class PetListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val breedName: String,
    val category: String, // "Cat" or "Dog"
    val age: String,
    val gender: String, // "公" (Male) / "母" (Female)
    val priceText: String, // e.g., "免费领养" or "¥1,200"
    val city: String,
    val description: String,
    val imageUrl: String, // URL or local illustration code
    val contactPhone: String,
    val contactWeChat: String,
    val isUserPosted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val id: String, // Format: "Breed:Ragdoll" or "Listing:5"
    val resourceType: String, // "Breed" or "Listing"
    val timestamp: Long = System.currentTimeMillis()
)

data class BreedInfo(
    val name: String,
    val englishName: String,
    val category: String, // "Cat" or "Dog"
    val origin: String, // e.g., "英国", "中国", "加拿大"
    val lifespan: String, // e.g., "12-15年"
    val sizeCategory: String, // e.g., "重型/大型", "中型", "小型"
    val coatLength: String, // e.g., "短毛", "长毛", "卷毛"
    val energyLevel: Int, // 1 to 5 Stars
    val friendliness: Int, // 1 to 5 Stars
    val groomingNeed: Int, // 1 to 5 Stars
    val intelligence: Int, // 1 to 5 Stars
    val temperamentTags: List<String>,
    val description: String,
    val feedingGuide: String,
    val imageUrl: String
)
