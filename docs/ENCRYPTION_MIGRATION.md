# Encrypted storage migration plan

`security-crypto` 1.1.0 deprecates `EncryptedSharedPreferences`. Mentor will move API keys and backup tokens to **DataStore + Tink** with an Android Keystore master key.

## Steps

1. Add `androidx.datastore` + Google Tink Android Keystore integration.
2. On first launch after upgrade, read all keys from `LlmSecureStore` / `EncryptedSharedPreferences`.
3. Re-encrypt with Tink `Aead` and persist in DataStore.
4. Set preference flag `encrypted_storage_migrated`.
5. Delete legacy encrypted preference files after successful verification.

Until migration ships, secrets remain in EncryptedSharedPreferences (current behavior).
