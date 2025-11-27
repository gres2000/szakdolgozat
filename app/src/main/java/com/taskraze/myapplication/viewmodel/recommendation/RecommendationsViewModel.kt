package com.taskraze.myapplication.viewmodel.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.model.recommendation.TagData
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    userId: String
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val userDocRef = db
        .collection("tags")
        .document(userId)

    fun saveTag(parentId: String, tag: String, weight: Int = 1) {
        val newTag = TagData(tag = tag, weight = weight, refId = parentId)

        viewModelScope.launch {
            try {
                val snapshot = userDocRef.get().await()
                val currentTags = if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    (snapshot.get("tags") as? List<Map<String, Any>>)?.map {
                        TagData(
                            tag = it["tag"] as String,
                            weight = (it["weight"] as Long).toInt(),
                            refId = it["refId"] as String
                        )
                    }?.toMutableList() ?: mutableListOf()
                } else {
                    mutableListOf()
                }

                val existingIndex = currentTags.indexOfFirst { it.refId == parentId }

                if (existingIndex != -1) {
                    currentTags[existingIndex] = newTag
                } else {
                    currentTags.add(newTag)
                }

                userDocRef.set(mapOf("tags" to currentTags)).await()

            } catch (e: Exception) {
                Log.e("RecommendationViewModel", "Error saving tag: ${e.message}")
            }
        }
    }


    suspend fun getUserTags(): List<TagData> {
        return try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                snapshot.get("tags") as? List<TagData> ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RecommendationViewModel", "Failed to load tags: ${e.message}")
            emptyList()
        }
    }

    data class UserTagsWrapper(
        val tags: List<TagData> = emptyList()
    )

    suspend fun getTagForId(itemId: String): String? {
        return try {
            val snapshot = userDocRef.get().await()
            val tags = snapshot.toObject(UserTagsWrapper::class.java)?.tags ?: emptyList()
            tags.firstOrNull { it.refId == itemId }?.tag
        } catch (e: Exception) {
            Log.e("RecommendationsViewModel", "Failed to get tag: ${e.message}")
            null
        }
    }


    suspend fun getRecommendedItems(): List<String> {
        val tags = getUserTags()

        // TODO:
        // K-Means clustering logic goes here
        // return list of recommended categories / tasks / etc

        return emptyList()
    }
}
