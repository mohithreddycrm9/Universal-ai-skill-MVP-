package com.skillmcp.mentor.data

import android.content.Context
import androidx.room.Room
import com.skillmcp.mentor.BuildConfig
import com.skillmcp.mentor.backup.BackupRepository
import com.skillmcp.mentor.backup.PayloadEncryptor
import com.skillmcp.mentor.data.db.MentorDatabase
import com.skillmcp.mentor.data.db.MentorMigrations
import com.skillmcp.mentor.mentor.BuildSuggestionEngine
import com.skillmcp.mentor.data.LlmSecureStore
import com.skillmcp.mentor.llm.LlmProfileRepository
import com.skillmcp.mentor.llm.LlmStreaming
import com.skillmcp.mentor.llm.MultiLlmClient
import com.skillmcp.mentor.extensions.ExtensionOrchestrator
import com.skillmcp.mentor.mentor.MentorRepository
import com.skillmcp.mentor.plugins.PluginRunner
import com.skillmcp.mentor.skills.BundledSkillInstaller
import com.skillmcp.mentor.skills.GitHubSkillImporter
import com.skillmcp.mentor.sync.SyncCoordinator
import com.skillmcp.mentor.analytics.UsageAnalytics
import com.skillmcp.mentor.brief.MorningBriefCollector
import com.skillmcp.mentor.voice.VoiceMentor

class AppContainer(context: Context) {
    val appContext = context.applicationContext

    val llmSecureStore = LlmSecureStore(appContext)
    val userPreferences = UserPreferences(appContext, llmSecureStore)
    val shareTextHolder = ShareTextHolder()
    val launchIntentHolder = LaunchIntentHolder()

    private val database: MentorDatabase =
        Room.databaseBuilder(appContext, MentorDatabase::class.java, "mentor.db")
            .addMigrations(*MentorMigrations.ALL)
            .apply {
                if (BuildConfig.DEBUG) {
                    fallbackToDestructiveMigrationOnDowngrade()
                }
            }
            .build()

    val encryptor = PayloadEncryptor(appContext)
    val multiLlmClient = MultiLlmClient()
    val llmStreaming = LlmStreaming()
    val llmProfileRepository =
        LlmProfileRepository(database.mentorDao(), llmSecureStore, userPreferences)
    val buildSuggestionEngine = BuildSuggestionEngine()
    val skillImporter = GitHubSkillImporter()
    val bundledSkillInstaller = BundledSkillInstaller(appContext)
    val pluginRunner = PluginRunner()
    val extensionOrchestrator = ExtensionOrchestrator(pluginRunner)
    val voiceMentor = VoiceMentor(appContext)
    val syncCoordinator = SyncCoordinator(appContext, database.mentorDao(), encryptor)

    val mentorRepository =
        MentorRepository(
            dao = database.mentorDao(),
            multiLlmClient = multiLlmClient,
            llmStreaming = llmStreaming,
            llmProfileRepository = llmProfileRepository,
            userPreferences = userPreferences,
            skillImporter = skillImporter,
            bundledSkillInstaller = bundledSkillInstaller,
            extensionOrchestrator = extensionOrchestrator,
            syncCoordinator = syncCoordinator,
        )

    val backupRepository =
        BackupRepository(
            dao = database.mentorDao(),
            encryptor = encryptor,
            userPreferences = userPreferences,
            secureStore = llmSecureStore,
        )

    val morningBriefCollector = MorningBriefCollector(appContext)
    val chatPdfExporter = ChatPdfExporter(appContext, database.mentorDao())

    val localDataWiper =
        LocalDataWiper(
            dao = database.mentorDao(),
            secureStore = llmSecureStore,
            userPreferences = userPreferences,
            llmProfileRepository = llmProfileRepository,
        )

    val chatExporter = ChatExporter(database.mentorDao())
    val usageAnalytics = UsageAnalytics(appContext)
    val internalLaunchToken = InternalLaunchToken(userPreferences)
}
