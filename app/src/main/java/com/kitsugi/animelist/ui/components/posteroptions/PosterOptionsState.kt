package com.kitsugi.animelist.ui.components.posteroptions

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * V2-A07 – PosterOptionsState
 *
 * Poster üzerinde uzun basma ile açılan seçenek menüsünün durumu.
 * NuvioTV posteroptions/State.kt referans alındı.
 */

enum class PosterOption {
    ADD_TO_LIST,
    SET_STATUS,
    ADD_TO_COLLECTION,
    SHARE,
    OPEN_IN_BROWSER,
    EDIT_PROGRESS,
    REMOVE_FROM_LIST
}

@Stable
class PosterOptionsState {
    var isVisible by mutableStateOf(false)
        private set

    var mediaId by mutableStateOf<Int?>(null)
        private set

    var mediaTitle by mutableStateOf("")
        private set

    var mediaCoverUrl by mutableStateOf<String?>(null)
        private set

    var availableOptions by mutableStateOf<List<PosterOption>>(emptyList())
        private set

    fun show(
        id: Int,
        title: String,
        coverUrl: String?,
        options: List<PosterOption> = PosterOption.entries
    ) {
        mediaId = id
        mediaTitle = title
        mediaCoverUrl = coverUrl
        availableOptions = options
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
        mediaId = null
        mediaTitle = ""
        mediaCoverUrl = null
        availableOptions = emptyList()
    }
}
