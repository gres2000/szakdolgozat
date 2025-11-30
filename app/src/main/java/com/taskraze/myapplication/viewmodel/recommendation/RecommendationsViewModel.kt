package com.taskraze.myapplication.viewmodel.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.model.recommendation.TagData
import com.taskraze.myapplication.view.recommendation.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    userId: String
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userDocRef = db
        .collection("tags")
        .document(userId)

    private val recommendedCollection = db.collection("recommended_items")

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations

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


    private suspend fun getUserTags(): List<TagData> {
        return try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                (snapshot.get("tags") as? List<Map<String, Any>>)?.map {
                    TagData(
                        tag = it["tag"] as String,
                        weight = (it["weight"] as Long).toInt(),
                        refId = it["refId"] as String
                    )
                } ?: emptyList()
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

    fun resetRecommendations() {
        _recommendations.value = emptyList()
    }

    private suspend fun getRecommendedTags(): Set<String> {
        val tags = getUserTags()
        if (tags.isEmpty()) return emptySet()

        val k = minOf(3, tags.size)
        val clusters = kMeans(tags, k)

        return clusters.values.flatten().map { it.tag.trim() }.toSet()
    }

    private fun kMeans(tags: List<TagData>, kClusters: Int, maxIterations: Int = 100): Map<Int, List<TagData>> {
        val allTagNames = tags.map { it.tag }.distinct()
        val vectors = tags.map { tag ->
            allTagNames.map { name -> if (name == tag.tag) tag.weight.toDouble() else 0.0 }
        }

        val centroids = vectors.shuffled().take(kClusters).toMutableList()
        var clusters: Map<Int, MutableList<TagData>> = (0 until kClusters).associateWith { mutableListOf<TagData>() }

        repeat(maxIterations) {
            clusters = (0 until kClusters).associateWith { mutableListOf<TagData>() }

            for ((i, vec) in vectors.withIndex()) {
                val closest = centroids.mapIndexed { index, centroid ->
                    index to euclideanDistance(vec, centroid)
                }.minByOrNull { it.second }!!.first
                clusters[closest]?.add(tags[i])
            }

            for (i in 0 until kClusters) {
                val clusterList = clusters[i]
                if (!clusterList.isNullOrEmpty()) {
                    val clusterVectors = clusterList.map { tag ->
                        allTagNames.map { name -> if (name == tag.tag) tag.weight.toDouble() else 0.0 }
                    }
                    centroids[i] = clusterVectors.reduce { acc, vec -> acc.zip(vec) { a, b -> a + b } }
                        .map { it / clusterVectors.size }
                }
            }
        }

        return clusters
    }


    private fun euclideanDistance(a: List<Double>, b: List<Double>): Double {
        return kotlin.math.sqrt(a.zip(b).sumOf { (x, y) -> (x - y) * (x - y) })
    }

    fun loadRecommendationsForUser() {
        viewModelScope.launch {
            try {
                val recommendedTags = getRecommendedTags()
                val allItems = fetchAllRecommendations()

                if (recommendedTags.isEmpty()) {
                    _recommendations.value = allItems
                    return@launch
                }

                val scored = allItems.map { item ->
                    val matchCount = item.tags.count { it.trim() in recommendedTags }
                    item to matchCount
                }

                val filteredSorted = scored
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .map { it.first }

                _recommendations.value = filteredSorted.ifEmpty { allItems }

            } catch (e: Exception) {
                Log.e("RecommendationsVM", "Failed to load recommendations: ${e.message}")
                _recommendations.value = emptyList()
            }
        }
    }

    private suspend fun fetchAllRecommendations(): List<Recommendation> {
        return try {
            val snapshot = recommendedCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(Recommendation::class.java) }
        } catch (e: Exception) {
            Log.e("RecommendationsVM", "Error fetching recommended items: ${e.message}")
            emptyList()
        }
    }
}
