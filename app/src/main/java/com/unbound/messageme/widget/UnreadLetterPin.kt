package com.unbound.messageme.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import com.unbound.messageme.R

object UnreadLetterPin {
    fun isPlaced(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, UnreadLetterWidgetReceiver::class.java)
        return manager.getAppWidgetIds(provider).isNotEmpty()
    }

    /** Asks the launcher to place the unread-letter widget on the home screen. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, UnreadLetterWidgetReceiver::class.java)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(context, R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
            return false
        }
        val accepted = manager.requestPinAppWidget(provider, null, null)
        if (!accepted) {
            Toast.makeText(context, R.string.widget_pin_failed, Toast.LENGTH_LONG).show()
        }
        return accepted
    }
}
