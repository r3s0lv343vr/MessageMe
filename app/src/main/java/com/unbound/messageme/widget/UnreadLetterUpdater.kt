package com.unbound.messageme.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnreadLetterUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun refresh() {
        runCatching { UnreadLetterWidget().updateAll(context) }
    }
}
