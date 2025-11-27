package com.taskraze.myapplication.viewmodel.recommendation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


    suspend fun getRecommendedItems(): List<String> {
        val tags = getUserTags()
        if (tags.isEmpty()) return emptyList()

        val k = minOf(3, tags.size)
        val clusters = kMeans(tags, k)
        // Example: pick the cluster of the last tag
        val lastTag = tags.last()
        val cluster = clusters.values.find { it.contains(lastTag) } ?: return emptyList()

        return cluster.map { it.tag }.distinct().filter { it != lastTag.tag }
    }

    private fun kMeans(tags: List<TagData>, kClusters: Int, maxIterations: Int = 100): Map<Int, List<TagData>> {
        // 1. Convert tags to vectors (one-hot or weighted)
        val allTagNames = tags.map { it.tag }.distinct()
        val vectors = tags.map { tag ->
            allTagNames.map { name -> if (name == tag.tag) tag.weight.toDouble() else 0.0 }
        }

        // 2. Randomly initialize k centroids
        val centroids = vectors.shuffled().take(kClusters).toMutableList()
        var clusters: Map<Int, MutableList<TagData>> = (0 until kClusters).associateWith { mutableListOf<TagData>() }

        repeat(maxIterations) {
            clusters = (0 until kClusters).associateWith { mutableListOf<TagData>() }

            // 3. Assign vectors to nearest centroid
            for ((i, vec) in vectors.withIndex()) {
                val closest = centroids.mapIndexed { index, centroid ->
                    index to euclideanDistance(vec, centroid)
                }.minByOrNull { it.second }!!.first
                clusters[closest]?.add(tags[i])
            }

            // 4. Update centroids
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
}
