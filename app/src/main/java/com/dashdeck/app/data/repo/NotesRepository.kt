package com.dashdeck.app.data.repo

import android.content.Context
import android.content.Intent
import com.dashdeck.app.data.db.ChecklistItem
import com.dashdeck.app.data.db.NotesDao
import com.dashdeck.app.data.db.PinnedNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

data class NoteModel(
    val id: Long,
    val title: String,
    val body: String,
    val checklist: List<ChecklistItem>,
    val isPinned: Boolean,
    val sourcePackage: String?,
    val updatedAtEpochMs: Long,
)

data class ParsedKeepNote(
    val title: String,
    val body: String,
    val checklist: List<ChecklistItem>,
)

object KeepShareParser {
    private val uncheckedRegex = Regex("""^\s*(?:[-*•]\s*)?(?:\[\s?]|☐|□)\s+(.+?)\s*$""")
    private val checkedRegex = Regex("""^\s*(?:[-*•]\s*)?(?:\[[xX]]|☑|✓|✔)\s+(.+?)\s*$""")
    private val bulletRegex = Regex("""^\s*[-*•]\s+(.+?)\s*$""")

    fun parse(
        subject: String?,
        sharedText: String,
        idGenerator: (Int) -> String = { idx -> "item_${idx}_${UUID.randomUUID().toString().take(8)}" },
    ): ParsedKeepNote {
        val normalized = sharedText.replace("\r\n", "\n").replace("\r", "\n")
        val rawLines = normalized.lines().map { it.trimEnd() }
        val nonBlankLines = rawLines.filter { it.isNotBlank() }

        val checklist = mutableListOf<ChecklistItem>()
        val bodyLines = mutableListOf<String>()

        var explicitTitle = subject?.trim()?.takeIf { it.isNotBlank() }
        var itemIndex = 0

        val hasExplicitCheckboxes = nonBlankLines.any {
            uncheckedRegex.matches(it) || checkedRegex.matches(it)
        }

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (bodyLines.isNotEmpty() && bodyLines.last().isNotEmpty()) {
                    bodyLines += ""
                }
                continue
            }

            val uncheckedMatch = uncheckedRegex.matchEntire(trimmed)
            if (uncheckedMatch != null) {
                checklist += ChecklistItem(
                    id = idGenerator(itemIndex++),
                    text = uncheckedMatch.groupValues[1].trim(),
                    isChecked = false,
                )
                continue
            }

            val checkedMatch = checkedRegex.matchEntire(trimmed)
            if (checkedMatch != null) {
                checklist += ChecklistItem(
                    id = idGenerator(itemIndex++),
                    text = checkedMatch.groupValues[1].trim(),
                    isChecked = true,
                )
                continue
            }

            if (hasExplicitCheckboxes) {
                val bulletMatch = bulletRegex.matchEntire(trimmed)
                if (bulletMatch != null) {
                    checklist += ChecklistItem(
                        id = idGenerator(itemIndex++),
                        text = bulletMatch.groupValues[1].trim(),
                        isChecked = false,
                    )
                    continue
                }
            }

            if (explicitTitle == null && bodyLines.isEmpty() && checklist.isEmpty()) {
                explicitTitle = trimmed.take(80)
            } else {
                bodyLines += trimmed
            }
        }

        val finalTitle = explicitTitle ?: if (checklist.isNotEmpty()) "Checklist" else "Keep Note"
        val finalBody = bodyLines.joinToString("\n").trim()

        return ParsedKeepNote(
            title = finalTitle,
            body = finalBody,
            checklist = checklist,
        )
    }
}

class NotesRepository(
    private val notesDao: NotesDao,
    private val json: Json,
    private val onNotesChanged: () -> Unit = {},
) {
    fun observeAllNotes(): Flow<List<NoteModel>> =
        notesDao.observeAllNotes().map { list -> list.map { it.toModel() } }

    fun observePinnedNotes(): Flow<List<NoteModel>> =
        notesDao.observePinnedNotes().map { list -> list.map { it.toModel() } }

    suspend fun getPinnedNotesSnapshot(): List<NoteModel> =
        notesDao.getPinnedNotesSnapshot().map { it.toModel() }

    suspend fun importSharedNote(
        subject: String?,
        text: String,
        sourcePackage: String? = GOOGLE_KEEP_PACKAGE,
    ): Long {
        val parsed = KeepShareParser.parse(subject = subject, sharedText = text)
        val id = notesDao.upsertNote(
            PinnedNoteEntity(
                title = parsed.title,
                body = parsed.body,
                checklistJson = json.encodeToString(parsed.checklist),
                isPinned = true,
                sourcePackage = sourcePackage,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        onNotesChanged()
        return id
    }

    suspend fun saveNote(
        id: Long = 0L,
        title: String,
        body: String,
        checklist: List<ChecklistItem>,
        isPinned: Boolean = true,
        sourcePackage: String? = null,
    ): Long {
        val savedId = notesDao.upsertNote(
            PinnedNoteEntity(
                id = id,
                title = title.trim().ifBlank { "Untitled Note" },
                body = body.trim(),
                checklistJson = json.encodeToString(checklist),
                isPinned = isPinned,
                sourcePackage = sourcePackage,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        onNotesChanged()
        return savedId
    }

    suspend fun toggleChecklistItem(noteId: Long, itemId: String) {
        val existing = notesDao.getNoteById(noteId) ?: return
        val items = decodeChecklist(existing.checklistJson)
        val updated = items.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        notesDao.upsertNote(
            existing.copy(
                checklistJson = json.encodeToString(updated),
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        onNotesChanged()
    }

    suspend fun setPinned(noteId: Long, isPinned: Boolean) {
        notesDao.setPinned(noteId, isPinned, System.currentTimeMillis())
        onNotesChanged()
    }

    suspend fun deleteNote(noteId: Long) {
        notesDao.deleteNoteById(noteId)
        onNotesChanged()
    }

    fun openInGoogleKeep(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(GOOGLE_KEEP_PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: return false
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    private fun PinnedNoteEntity.toModel(): NoteModel = NoteModel(
        id = id,
        title = title,
        body = body,
        checklist = decodeChecklist(checklistJson),
        isPinned = isPinned,
        sourcePackage = sourcePackage,
        updatedAtEpochMs = updatedAtEpochMs,
    )

    private fun decodeChecklist(raw: String): List<ChecklistItem> =
        runCatching { json.decodeFromString<List<ChecklistItem>>(raw) }.getOrDefault(emptyList())

    companion object {
        const val GOOGLE_KEEP_PACKAGE = "com.google.android.keep"
    }
}
