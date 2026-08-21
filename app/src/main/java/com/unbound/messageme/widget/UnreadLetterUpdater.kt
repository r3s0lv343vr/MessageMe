package com.unbound.messageme.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnreadLetterUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun refresh() {
        runCatching { UnreadLetterViews.push(context) }
    }
}
