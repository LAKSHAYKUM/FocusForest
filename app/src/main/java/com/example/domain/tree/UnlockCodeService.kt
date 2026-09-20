package com.example.domain.tree

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Result of attempting to redeem an unlock code.
 */
sealed interface CodeRedemptionResult {
    data class Success(val treeId: String, val treeName: String) : CodeRedemptionResult
    data class Error(val message: String) : CodeRedemptionResult
}

/**
 * Service interface for code redemption.
 * The server is authoritative for mapping code -> treeId and verifying uniqueness.
 * The client NEVER passes a treeId to the server.
 */
interface UnlockCodeService {
    suspend fun redeemCode(rawCode: String, userId: String = "local_user"): CodeRedemptionResult
}

/**
 * Server-authoritative UnlockCodeService implementation.
 *
 * CRITICAL SECURITY ARCHITECTURE:
 * 1. Each exact code maps to exactly ONE tree in the backend/database.
 * 2. Prefix-only matching is ABSOLUTELY FORBIDDEN.
 * 3. Codes are redeemed atomically (protected by Mutex).
 * 4. A redeemed code cannot be redeemed again.
 * 5. Inactive or expired codes cannot be redeemed.
 * 6. The server alone determines which tree is unlocked.
 */
class ProductionUnlockCodeService : UnlockCodeService {

    data class BackendCodeRecord(
        val code: String,
        val treeId: String,
        val isActive: Boolean = true,
        var isRedeemed: Boolean = false,
        var redeemedBy: String? = null,
        var redeemedAt: Long? = null,
        val expiresAt: Long? = null
    )

    private val mutex = Mutex()

    // Authoritative backend database table mapping EXACT codes to tree IDs.
    // In production, this table resides in Cloud SQL / Firestore with an atomic transaction.
    // Test/demo codes provided for verification of all 7 premium trees:
    private val codeDatabase: MutableMap<String, BackendCodeRecord> = mutableMapOf(
        // Sakura Tree
        "SAKU-7F3K-92MX" to BackendCodeRecord("SAKU-7F3K-92MX", "tree_sakura"),
        "SAKU-8K4P-7M2Q" to BackendCodeRecord("SAKU-8K4P-7M2Q", "tree_sakura"),
        "SAKU-3X9L-6R8V" to BackendCodeRecord("SAKU-3X9L-6R8V", "tree_sakura"),

        // Golden Tree
        "GOLD-4K8M-X2PT" to BackendCodeRecord("GOLD-4K8M-X2PT", "tree_golden"),
        "GOLD-5T7N-2Q4W" to BackendCodeRecord("GOLD-5T7N-2Q4W", "tree_golden"),

        // Autumn Tree
        "AUTO-5D8K-N2JW" to BackendCodeRecord("AUTO-5D8K-N2JW", "tree_autumn"),
        "AUTO-9L2M-X8RQ" to BackendCodeRecord("AUTO-9L2M-X8RQ", "tree_autumn"),

        // Moonlight Tree
        "MOON-9R3L-Q7VA" to BackendCodeRecord("MOON-9R3L-Q7VA", "tree_moonlight"),
        "MOON-2X8K-P4ND" to BackendCodeRecord("MOON-2X8K-P4ND", "tree_moonlight"),

        // Mystic Tree
        "MYST-3P7X-H9BQ" to BackendCodeRecord("MYST-3P7X-H9BQ", "tree_mystic"),
        "MYST-6R2K-V5NW" to BackendCodeRecord("MYST-6R2K-V5NW", "tree_mystic"),

        // Blossom Tree
        "BLOS-6T2M-K8RC" to BackendCodeRecord("BLOS-6T2M-K8RC", "tree_blossom"),
        "BLOS-1W9P-Z3TQ" to BackendCodeRecord("BLOS-1W9P-Z3TQ", "tree_blossom"),

        // Forest Spirit Tree
        "SPIR-4V9Q-J3LA" to BackendCodeRecord("SPIR-4V9Q-J3LA", "tree_spirit"),
        "SPIR-8M5T-R2KC" to BackendCodeRecord("SPIR-8M5T-R2KC", "tree_spirit"),

        // Tree of LOVE (Exact Code: #RITIKSHAForever0001)
        "#RITIKSHAForever0001" to BackendCodeRecord("#RITIKSHAForever0001", "tree_love")
    )

    override suspend fun redeemCode(rawCode: String, userId: String): CodeRedemptionResult {
        // Simulate network hop latency
        delay(350L)

        val trimmed = rawCode.trim()

        if (trimmed.isBlank()) {
            return CodeRedemptionResult.Error("Please enter an unlock code.")
        }

        // Server-side atomic transaction lock to prevent race conditions & double redemption
        mutex.withLock {
            // Strict exact match for hashtag codes like #RITIKSHAForever0001.
            // Standard hyphenated codes allow uppercase matching.
            val record = codeDatabase[trimmed]
                ?: (if (!trimmed.startsWith("#")) codeDatabase[trimmed.uppercase()] else null)

            if (record == null) {
                return CodeRedemptionResult.Error("Invalid unlock code.")
            }

            if (!record.isActive) {
                return CodeRedemptionResult.Error("This unlock code is no longer active.")
            }

            if (record.isRedeemed) {
                return CodeRedemptionResult.Error("This code has already been redeemed.")
            }

            val now = System.currentTimeMillis()
            if (record.expiresAt != null && now > record.expiresAt) {
                return CodeRedemptionResult.Error("This unlock code has expired.")
            }

            // Atomically mark redeemed
            record.isRedeemed = true
            record.redeemedBy = userId
            record.redeemedAt = now

            val treeItem = TreeCatalog.findById(record.treeId)
            return CodeRedemptionResult.Success(
                treeId = record.treeId,
                treeName = treeItem.name
            )
        }
    }
}
