package eu.kanade.tachiyomi.source

/**
 * Mihon/Tachiyomi kaynakları `setupPreferenceScreen(screen: PreferenceScreen)` içinde
 * GERÇEK `androidx.preference.PreferenceScreen` API'sini kullanır
 * (EditTextPreference, ListPreference, SwitchPreferenceCompat vb. eklerler).
 *
 * Bu yüzden burada boş bir stub yerine, Mihon'daki gibi androidx tipine
 * `typealias` veriyoruz. Aksi halde bir kaynağın setupPreferenceScreen'i
 * çağrıldığında ClassCastException/NoSuchMethodError ile çöker.
 */
typealias PreferenceScreen = androidx.preference.PreferenceScreen
