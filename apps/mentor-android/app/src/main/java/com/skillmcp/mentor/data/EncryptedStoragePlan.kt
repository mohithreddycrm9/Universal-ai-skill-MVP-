package com.skillmcp.mentor.data

/**
 * One-time migration path from EncryptedSharedPreferences to DataStore + Tink (Android Keystore).
 *
 * Steps (not yet automated — see docs/ENCRYPTION_MIGRATION.md):
 * 1. Read secrets from [LlmSecureStore] and legacy encrypted prefs.
 * 2. Write into DataStore with Tink AEAD using a Keystore-backed key.
 * 3. Set [MentorPrefs.encryptedStorageMigrated] and remove legacy files.
 */
object EncryptedStoragePlan {
    const val DOCUMENTATION_PATH = "docs/ENCRYPTION_MIGRATION.md"
}
