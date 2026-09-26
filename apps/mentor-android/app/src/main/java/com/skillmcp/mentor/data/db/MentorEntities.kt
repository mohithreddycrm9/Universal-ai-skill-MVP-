package com.skillmcp.mentor.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.RoomDatabase
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow

@JsonClass(generateAdapter = true)
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goal: String,
    val updatedAt: Long,
    val pinned: Boolean = false,
)

@Entity(
    tableName = "conversation_skills",
    primaryKeys = ["conversationId", "skillId"],
)
data class ConversationSkillEntity(
    val conversationId: String,
    val skillId: String,
    val enabled: Boolean,
)

@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val repo: String,
    val ref: String,
    val title: String,
    val markdown: String,
    val addedAt: Long,
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "llm_profiles")
data class LlmProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val baseUrl: String,
    val model: String,
    val inputCostPer1M: Double,
    val outputCostPer1M: Double,
    val isBuiltIn: Boolean,
)

@Entity(tableName = "llm_usage")
data class LlmUsageEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val providerName: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val estimatedUsd: Double,
    val latencyMs: Long,
    val success: Boolean,
    val errorMessage: String?,
    val createdAt: Long,
)

data class UsageByModelAggregate(
    val providerName: String,
    val model: String,
    val requestCount: Int,
    val totalTokens: Long,
    val estimatedUsd: Double,
)

data class UsageByDayAggregate(
    val dayKey: String,
    val requestCount: Int,
    val totalTokens: Long,
    val estimatedUsd: Double,
)

data class UsageTotalsAggregate(
    val requestCount: Int,
    val promptTokens: Long,
    val completionTokens: Long,
    val estimatedUsd: Double,
)

@Entity(tableName = "build_events")
data class BuildEventEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val kind: String,
    val summary: String?,
    val createdAt: Long,
)

@Dao
interface MentorDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeMessages(projectId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("SELECT * FROM conversation_skills WHERE conversationId = :conversationId")
    suspend fun conversationSkills(conversationId: String): List<ConversationSkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversationSkill(row: ConversationSkillEntity)

    @Query("SELECT * FROM saved_prompts ORDER BY createdAt DESC")
    fun observeSavedPrompts(): Flow<List<SavedPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedPrompt(prompt: SavedPromptEntity)

    @Query("DELETE FROM saved_prompts WHERE id = :id")
    suspend fun deleteSavedPrompt(id: String)

    @Query(
        """
        SELECT COALESCE(SUM(estimatedUsd), 0) FROM llm_usage
        WHERE createdAt >= :sinceMs AND success = 1
        """,
    )
    suspend fun sumSpendSince(sinceMs: Long): Double

    @Query("SELECT * FROM skills ORDER BY addedAt DESC")
    fun observeSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkill(id: String)

    @Query("SELECT * FROM build_events WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 24")
    fun observeBuildEvents(projectId: String): Flow<List<BuildEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildEvent(event: BuildEventEntity)

    @Query("SELECT * FROM chat_messages")
    suspend fun allMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM projects")
    suspend fun allProjects(): List<ProjectEntity>

    @Query("SELECT * FROM skills")
    suspend fun allSkills(): List<SkillEntity>

    @Query("SELECT * FROM llm_profiles ORDER BY name ASC")
    fun observeLlmProfiles(): Flow<List<LlmProfileEntity>>

    @Query("SELECT * FROM llm_profiles")
    suspend fun allLlmProfiles(): List<LlmProfileEntity>

    @Query("SELECT * FROM llm_profiles WHERE id = :id LIMIT 1")
    suspend fun getLlmProfile(id: String): LlmProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLlmProfile(profile: LlmProfileEntity)

    @Query("DELETE FROM llm_profiles WHERE id = :id")
    suspend fun deleteLlmProfile(id: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Query("DELETE FROM skills")
    suspend fun deleteAllSkills()

    @Query("DELETE FROM conversation_skills")
    suspend fun deleteAllConversationSkills()

    @Query("DELETE FROM saved_prompts")
    suspend fun deleteAllSavedPrompts()

    @Query("DELETE FROM build_events")
    suspend fun deleteAllBuildEvents()

    @Query("DELETE FROM llm_usage")
    suspend fun deleteAllLlmUsage()

    @Transaction
    suspend fun wipeUserTables() {
        deleteAllMessages()
        deleteAllProjects()
        deleteAllSkills()
        deleteAllConversationSkills()
        deleteAllSavedPrompts()
        deleteAllBuildEvents()
        deleteAllLlmUsage()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLlmUsage(row: LlmUsageEntity)

    @Query(
        """
        SELECT
            COUNT(*) AS requestCount,
            COALESCE(SUM(promptTokens), 0) AS promptTokens,
            COALESCE(SUM(completionTokens), 0) AS completionTokens,
            COALESCE(SUM(estimatedUsd), 0) AS estimatedUsd
        FROM llm_usage
        WHERE createdAt >= :sinceMs AND success = 1
        """,
    )
    fun observeUsageTotals(sinceMs: Long): Flow<UsageTotalsAggregate>

    @Query(
        """
        SELECT
            providerName,
            model,
            COUNT(*) AS requestCount,
            COALESCE(SUM(totalTokens), 0) AS totalTokens,
            COALESCE(SUM(estimatedUsd), 0) AS estimatedUsd
        FROM llm_usage
        WHERE createdAt >= :sinceMs AND success = 1
        GROUP BY providerName, model
        ORDER BY estimatedUsd DESC
        """,
    )
    fun observeUsageByModel(sinceMs: Long): Flow<List<UsageByModelAggregate>>

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') AS dayKey,
            COUNT(*) AS requestCount,
            COALESCE(SUM(totalTokens), 0) AS totalTokens,
            COALESCE(SUM(estimatedUsd), 0) AS estimatedUsd
        FROM llm_usage
        WHERE createdAt >= :sinceMs AND success = 1
        GROUP BY dayKey
        ORDER BY dayKey DESC
        """,
    )
    fun observeUsageByDay(sinceMs: Long): Flow<List<UsageByDayAggregate>>
}

@Database(
    entities = [
        ChatMessageEntity::class,
        ProjectEntity::class,
        SkillEntity::class,
        BuildEventEntity::class,
        LlmProfileEntity::class,
        LlmUsageEntity::class,
        ConversationSkillEntity::class,
        SavedPromptEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MentorDatabase : RoomDatabase() {
    abstract fun mentorDao(): MentorDao
}
