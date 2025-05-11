package com.momentum.habittracker.database

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.initialize
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HabitRepository(private val context: Context) {
    private val db: FirebaseFirestore by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Firebase.initialize(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("HabitRepository", "Firebase initialization failed", e)
            throw IllegalStateException("Firebase must be initialized first", e)
        }
    }

    private val habitsCollection by lazy { db.collection("habits") }

    suspend fun saveHabit(habit: Habit): String {
        return try {
            if (habit.id.isEmpty()) {
                val docRef = habitsCollection.add(habit).await()
                docRef.id
            } else {
                habitsCollection.document(habit.id)
                    .set(habit, SetOptions.merge())
                    .await()
                habit.id
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error saving habit", e)
            throw e
        }
    }

    fun getAllHabits(): Flow<List<Habit>> = callbackFlow {
        val listener = habitsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val habits = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Habit::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(habits)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getHabitById(id: String): Habit? {
        return try {
            habitsCollection.document(id).get().await()
                ?.toObject(Habit::class.java)
                ?.copy(id = id)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting habit $id", e)
            null
        }
    }

    suspend fun deleteHabit(id: String): Boolean {
        return try {
            habitsCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error deleting habit $id", e)
            false
        }
    }

    fun getHabitsByCategory(category: String): Flow<List<Habit>> = callbackFlow {
        val listener = habitsCollection
            .whereEqualTo("category", category)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val habits = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Habit::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(habits)
            }
        awaitClose { listener.remove() }
    }
}