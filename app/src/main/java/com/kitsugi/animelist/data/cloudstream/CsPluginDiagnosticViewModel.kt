package com.kitsugi.animelist.data.cloudstream

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * In-app CS plugin tanı ekranı için ViewModel.
 * CsPluginDiagnosticRunner state'ini toplar ve UI'a sunar.
 */
class CsPluginDiagnosticViewModel : ViewModel() {

    var onlyInstalled by mutableStateOf(true)

    val progress   get() = CsPluginDiagnosticRunner.progress
    val results    get() = CsPluginDiagnosticRunner.results
    val isRunning  get() = CsPluginDiagnosticRunner.isRunning
    val reportPath get() = CsPluginDiagnosticRunner.reportPath

    fun startDiagnostic(context: Context) {
        viewModelScope.launch {
            CsPluginDiagnosticRunner.startDiagnostic(context.applicationContext, onlyInstalled = onlyInstalled)
        }
    }

    fun cancelDiagnostic() {
        CsPluginDiagnosticRunner.cancel()
    }

    fun clearResults() {
        CsPluginDiagnosticRunner.clearResults()
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel temizlenince tanıyı durdur
        CsPluginDiagnosticRunner.cancel()
    }
}
