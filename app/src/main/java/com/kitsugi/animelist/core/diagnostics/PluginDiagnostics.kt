package com.kitsugi.animelist.core.diagnostics

import android.util.Log

private const val TAG = "PluginDiagnostics"

/**
 * Collects diagnostic steps during a plugin or addon test run.
 * Each step is a status line.
 */
data class PluginDiagnostics(
    val steps: MutableList<String> = mutableListOf()
) {
    fun addStep(step: String) {
        steps.add(step)
        Log.d(TAG, step)
    }
}
