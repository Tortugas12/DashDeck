package com.dashdeck.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.repo.NotesRepository
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.takeIf {
            it.action == Intent.ACTION_SEND && it.type?.startsWith("text/") == true
        }?.getStringExtra(Intent.EXTRA_TEXT)?.trim()

        if (sharedText.isNullOrBlank()) {
            Toast.makeText(this, "No text found to pin to DashDeck.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val referrerPackage = referrer?.host ?: NotesRepository.GOOGLE_KEEP_PACKAGE
        val container = (application as DashDeckApplication).container

        lifecycleScope.launch {
            container.notesRepository.importSharedNote(
                subject = subject,
                text = sharedText,
                sourcePackage = referrerPackage,
            )
            Toast.makeText(
                this@ShareReceiverActivity,
                "Pinned note to DashDeck",
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }
}
