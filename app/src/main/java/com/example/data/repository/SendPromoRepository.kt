package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.PromoDraft
import com.example.data.model.PromoGroup
import com.example.data.model.SentCampaign
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "SendPromoRepo"

data class ImportContactsResult(
    val totalImported: Int,
    val newEmailsCount: Int,
    val groupsCreatedCount: Int,
    val groupsUpdatedCount: Int,
    val details: List<String>
)

class SendPromoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // Observe Groups sorted by newest update/creation
    fun observeGroups(userId: String): Flow<List<PromoGroup>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("groups")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PromoGroup::class.java)
            }
    }

    // Seed default sample groups if user has none
    suspend fun seedDefaultGroupsIfEmpty(userId: String) {
        if (userId.isBlank()) return
        try {
            val existing = db.collection("users").document(userId).collection("groups").limit(1).get().await()
            if (existing.isEmpty) {
                Log.d(TAG, "Seeding curated DJ & Label promo groups for user: $userId")
                val sampleGroups = listOf(
                    PromoGroup(
                        name = "Labels",
                        emails = listOf(
                            "demo@spinninrecords.com",
                            "submissions@defected.com",
                            "demos@toolroomrecords.com",
                            "promo@armadamusic.com",
                            "demo@drumcode.se",
                            "ar@anjunadeep.com",
                            "demo@afterlife.it"
                        ),
                        isProtected = true
                    ),
                    PromoGroup(
                        name = "Tech House Top 50",
                        emails = listOf(
                            "james.hype@djpromopool.club",
                            "fisher@catchandrelease.audio",
                            "michael.bibi@solidgrooves.uk",
                            "cloonee@hellbentrecords.com",
                            "solardo@solamusic.com",
                            "chris.lake@blackbookrecords.com"
                        )
                    ),
                    PromoGroup(
                        name = "Melodic Techno Labels & A&R",
                        emails = listOf(
                            "promo@afterlife.recordings",
                            "demo@drumcode.se",
                            "ar@anjunadeep.com",
                            "inbox@innervisions.berlin",
                            "promo@diynamic.com"
                        )
                    ),
                    PromoGroup(
                        name = "Festival & Club Residents",
                        emails = listOf(
                            "resident@spaceibiza.es",
                            "headliner@ushuaia.club",
                            "promos@fabriclondon.com",
                            "sound@amnesia.es",
                            "booking@hïibiza.com"
                        )
                    ),
                    PromoGroup(
                        name = "Radio Tastemakers & Podcasts",
                        emails = listOf(
                            "bbc.radio1@danceanthems.co.uk",
                            "submissions@defectedradio.com",
                            "music@toolroomradio.com",
                            "playlist@armindance.com"
                        )
                    )
                )

                for (grp in sampleGroups) {
                    db.collection("users").document(userId).collection("groups").add(
                        grp.copy(createdAt = null, updatedAt = null)
                    ).await()
                }
            } else {
                ensureLabelsGroupExists(userId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking/seeding groups", e)
        }
    }

    suspend fun ensureLabelsGroupExists(userId: String) {
        if (userId.isBlank()) return
        try {
            val groupsSnapshot = db.collection("users").document(userId).collection("groups").get().await()
            val hasLabels = groupsSnapshot.documents.any { doc ->
                val name = doc.getString("name") ?: ""
                val isProt = doc.getBoolean("isProtected") ?: false
                isProt || name.equals("Labels", ignoreCase = true)
            }
            if (!hasLabels) {
                Log.d(TAG, "Creating dedicated Labels group for user: $userId")
                db.collection("users").document(userId).collection("groups").add(
                    PromoGroup(
                        name = "Labels",
                        emails = listOf(
                            "demo@spinninrecords.com",
                            "submissions@defected.com",
                            "demos@toolroomrecords.com",
                            "promo@armadamusic.com",
                            "demo@drumcode.se",
                            "ar@anjunadeep.com",
                            "demo@afterlife.it"
                        ),
                        isProtected = true,
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error ensuring Labels group exists", e)
        }
    }

    suspend fun saveGroup(userId: String, group: PromoGroup): String {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(group.name.isNotBlank()) { "Group name cannot be blank" }
        val collection = db.collection("users").document(userId).collection("groups")
        val isLabels = group.isProtected || group.name.equals("Labels", ignoreCase = true)
        val toSave = if (isLabels) group.copy(isProtected = true) else group
        return if (toSave.id.isBlank()) {
            val docRef = collection.add(toSave.copy(createdAt = null, updatedAt = null)).await()
            docRef.id
        } else {
            val docRef = collection.document(toSave.id)
            docRef.set(toSave.copy(updatedAt = null)).await()
            toSave.id
        }
    }

    suspend fun deleteGroup(userId: String, groupId: String) {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("groups").document(groupId)
        val snap = docRef.get().await()
        val isProt = snap.getBoolean("isProtected") ?: false
        val name = snap.getString("name") ?: ""
        if (isProt || name.equals("Labels", ignoreCase = true)) {
            Log.w(TAG, "Refusing to delete protected Labels group: $groupId")
            return
        }
        docRef.delete().await()
    }

    suspend fun importContacts(
        userId: String,
        contacts: List<com.example.data.util.ParsedContact>,
        targetGroupId: String? = null,
        targetGroupName: String? = null
    ): ImportContactsResult {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        if (contacts.isEmpty()) {
            return ImportContactsResult(0, 0, 0, 0, emptyList())
        }

        val groupsCollection = db.collection("users").document(userId).collection("groups")
        val currentGroupsSnapshot = groupsCollection.get().await()
        val currentGroups = currentGroupsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(PromoGroup::class.java)?.copy(id = doc.id)
        }

        var groupsCreated = 0
        var groupsUpdated = 0
        var newEmailsAdded = 0
        val summaries = mutableListOf<String>()

        // Case 1: Import into a specific existing group by ID
        if (!targetGroupId.isNullOrBlank()) {
            val targetGroup = currentGroups.find { it.id == targetGroupId }
            if (targetGroup != null) {
                val existingEmailSet = targetGroup.emails.map { it.trim().lowercase() }.toSet()
                val incomingEmails = contacts.map { it.email.trim().lowercase() }.filter { it.isNotBlank() }
                val newEmails = incomingEmails.filterNot { existingEmailSet.contains(it) }.distinct()
                val mergedEmails = (targetGroup.emails + newEmails).distinct()

                val isLabels = targetGroup.isProtected || targetGroup.name.equals("Labels", ignoreCase = true)
                groupsCollection.document(targetGroup.id).set(
                    targetGroup.copy(
                        emails = mergedEmails,
                        isProtected = isLabels,
                        updatedAt = null
                    )
                ).await()

                groupsUpdated++
                newEmailsAdded += newEmails.size
                summaries.add("Added ${newEmails.size} new contacts to '${targetGroup.name}' (Total: ${mergedEmails.size})")
            }
        }
        // Case 2: Import into a named group (either existing or new)
        else if (!targetGroupName.isNullOrBlank()) {
            val cleanName = targetGroupName.trim()
            val existingGroup = currentGroups.find { it.name.equals(cleanName, ignoreCase = true) }
            val isLabels = cleanName.equals("Labels", ignoreCase = true)

            if (existingGroup != null) {
                val existingEmailSet = existingGroup.emails.map { it.trim().lowercase() }.toSet()
                val incomingEmails = contacts.map { it.email.trim().lowercase() }.filter { it.isNotBlank() }
                val newEmails = incomingEmails.filterNot { existingEmailSet.contains(it) }.distinct()
                val mergedEmails = (existingGroup.emails + newEmails).distinct()

                groupsCollection.document(existingGroup.id).set(
                    existingGroup.copy(
                        emails = mergedEmails,
                        isProtected = existingGroup.isProtected || isLabels,
                        updatedAt = null
                    )
                ).await()

                groupsUpdated++
                newEmailsAdded += newEmails.size
                summaries.add("Added ${newEmails.size} contacts to existing group '$cleanName' (Total: ${mergedEmails.size})")
            } else {
                val uniqueEmails = contacts.map { it.email.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
                groupsCollection.add(
                    PromoGroup(
                        name = cleanName,
                        emails = uniqueEmails,
                        isProtected = isLabels,
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()

                groupsCreated++
                newEmailsAdded += uniqueEmails.size
                summaries.add("Created new group '$cleanName' with ${uniqueEmails.size} contacts")
            }
        }
        // Case 3: Group by CSV group column (e.g., "Labels", "Resident DJs", "Tech House", etc.)
        else {
            val contactsByGroup = contacts.groupBy { contact ->
                contact.group.trim().ifBlank { "Imported Contacts" }
            }

            for ((grpName, grpContacts) in contactsByGroup) {
                val existingGroup = currentGroups.find { it.name.equals(grpName, ignoreCase = true) }
                val isLabels = grpName.equals("Labels", ignoreCase = true)

                if (existingGroup != null) {
                    val existingEmailSet = existingGroup.emails.map { it.trim().lowercase() }.toSet()
                    val incomingEmails = grpContacts.map { it.email.trim().lowercase() }.filter { it.isNotBlank() }
                    val newEmails = incomingEmails.filterNot { existingEmailSet.contains(it) }.distinct()
                    val mergedEmails = (existingGroup.emails + newEmails).distinct()

                    groupsCollection.document(existingGroup.id).set(
                        existingGroup.copy(
                            emails = mergedEmails,
                            isProtected = existingGroup.isProtected || isLabels,
                            updatedAt = null
                        )
                    ).await()

                    groupsUpdated++
                    newEmailsAdded += newEmails.size
                    summaries.add("Added ${newEmails.size} contacts to '$grpName' (Total: ${mergedEmails.size})")
                } else {
                    val uniqueEmails = grpContacts.map { it.email.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
                    groupsCollection.add(
                        PromoGroup(
                            name = grpName,
                            emails = uniqueEmails,
                            isProtected = isLabels,
                            createdAt = null,
                            updatedAt = null
                        )
                    ).await()

                    groupsCreated++
                    newEmailsAdded += uniqueEmails.size
                    summaries.add("Created new group '$grpName' with ${uniqueEmails.size} contacts")
                }
            }
        }

        return ImportContactsResult(
            totalImported = contacts.size,
            newEmailsCount = newEmailsAdded,
            groupsCreatedCount = groupsCreated,
            groupsUpdatedCount = groupsUpdated,
            details = summaries
        )
    }

    // Observe Drafts sorted by newest
    fun observeDrafts(userId: String): Flow<List<PromoDraft>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("drafts")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PromoDraft::class.java)
            }
    }

    suspend fun saveDraft(userId: String, draft: PromoDraft): String {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val collection = db.collection("users").document(userId).collection("drafts")
        return if (draft.id.isBlank()) {
            val docRef = collection.add(draft.copy(createdAt = null, updatedAt = null)).await()
            docRef.id
        } else {
            val docRef = collection.document(draft.id)
            docRef.set(draft.copy(updatedAt = null)).await()
            draft.id
        }
    }

    suspend fun deleteDraft(userId: String, draftId: String) {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(draftId.isNotBlank()) { "Draft ID must not be blank" }
        db.collection("users").document(userId).collection("drafts").document(draftId).delete().await()
    }

    // Observe Sent Campaigns sorted by newest
    fun observeCampaigns(userId: String): Flow<List<SentCampaign>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId).collection("campaigns")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SentCampaign::class.java)
            }
    }

    suspend fun recordCampaign(userId: String, campaign: SentCampaign): String {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("campaigns")
            .add(campaign.copy(sentAt = null)).await()
        return docRef.id
    }
}
