package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "managed_addons")
data class ManagedAddonEntity(
    @PrimaryKey
    val manifestUrl: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0,
    /**
     * JSON array of ID prefixes declared by the manifest (e.g. ["tt", "kitsu:"]).
     * Null means the addon wasn't parsed with prefix support (legacy rows).
     * Mirrors Kitsugi TV's Addon.idPrefixes.
     */
    val idPrefixes: String? = null,
    /**
     * Comma-separated list of resource types supported for streams (e.g. "series,movie").
     * Null means no type filtering is applied (accept all).
     */
    val streamTypes: String? = null,
    /**
     * Comma-separated list of resource types supported for subtitles (e.g. "series,movie").
     * Null means no subtitle resource is supported or not a subtitle addon.
     */
    val subtitleTypes: String? = null
)
