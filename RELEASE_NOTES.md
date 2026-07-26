# Kitsugi v2.4.105 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔗 Birleşik Çapraz-Servis Senkronizasyonu (KRİTİK)

- **Tek Kayıtla 3 Servise Ekleme:** Arama ekranında bir anime/dizi/filme "+" butonuna basıldığında artık AniList, MyAnimeList ve Simkl'a aynı anda eklenip senkronize edilir. Eski sistemde yalnızca tek servise kayıt oluşturuluyordu.

- **ARM API ile Otomatik ID Çözümlemesi:** Ekleme sırasında ARM (Anime Relations Map) API'si üzerinden `malId`, `aniListId` ve `tmdbId` otomatik olarak çözümlenir; tüm servisler için doğru ID'ler tek bir kayıtta birleştirilir.

- **Simkl Lookup Entegrasyonu:** Simkl hesabı bağlıysa, `/search/lookup` API'si aracılığıyla MAL ID veya TMDB ID'den `simklId` otomatik çözümlenir. Artık Simkl ID'sini manuel girmeye gerek yok.

- **"Ghosting" Hatası Giderildi:** Önceki sürümde bir anime arama ekranında "eklenmiş" görünüyor ancak yalnızca tek servise kaydediliyordu. Bu tutarsızlık tamamen ortadan kaldırıldı.

- **Unified Senkronizasyon Mantığı (`ExternalListSyncManager`):** Servis seçimi artık `entry.source` alanına değil, her servis için bağımsız olarak token varlığına + ilgili platform ID'sine göre yapılır. Bir `MediaEntry` içinde birden fazla platform ID'si varsa hepsi eş zamanlı güncellenir.

- **Çapraz-Servis Silme:** Kayıt silindiğinde de `ExternalListSyncManager` artık tüm bağlı servislerde silme işlemini eş zamanlı gerçekleştirir.

- **Akıllı Yineleme Kontrolü:** Ekleme sırasında tekrar engeli artık tüm kaynak/platform kombinasyonlarına karşı çalışır; yalnızca tek kaynağa göre değil.

### 🎬 Sezonluk Akış İyileştirmeleri

- **Doğru Sezon Araması:** 2. sezon ve sonrasındaki bölümleri akış eklentilerinde ararken önce \"2. Sezon\", \"Season 2\", \"S2\" gibi sezon-özelleştirilmiş sorgular deneniyor.

- **Akıllı Sezon Eşleştirme (`CsTitleMatcher`):** Arama sonuçları sezon bilgisine göre +0.35 ödül / -0.60 ceza alıyor. Sezon içermeyen başlıklar, istenilen sezon > 1 ise -0.50 ceza alıyor.

- **`targetSeason` Yayılımı:** Sezon bilgisi UI katmanından akış motoruna ve eklenti çağrılarına doğru şekilde iletiliyor; çok sezonlu seriler için 1. sezon varsayılanı devre dışı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔗 Unified Cross-Service Synchronization (CRITICAL)

- **Add to All 3 Services with One Tap:** Pressing "+" on a search result now simultaneously registers and synchronizes the entry across AniList, MyAnimeList, and Simkl. The old system only created a record for a single service.

- **Automatic ID Resolution via ARM API:** During the add flow, MAL, AniList, and TMDB IDs are resolved automatically through the ARM (Anime Relations Map) API and consolidated into a single `MediaEntry`.

- **Simkl Lookup Integration:** When a Simkl account is connected, the `simklId` is automatically resolved from MAL ID or TMDB ID via Simkl's `/search/lookup` API. No manual Simkl ID entry required.

- **"Ghosting" Bug Fixed:** In previous builds, an anime could appear "added" in the UI while only being saved to one service. This inconsistency has been fully eliminated.

- **Unified Sync Logic (`ExternalListSyncManager`):** Service selection is now evaluated independently per service based on token presence and the availability of a matching platform ID — not by `entry.source`. A `MediaEntry` holding multiple platform IDs will sync to all of them simultaneously.

- **Cross-Service Deletion:** Deleting an entry now triggers removal across all connected services concurrently via `ExternalListSyncManager`.

- **Smart Duplicate Guard:** The duplicate check now evaluates against all source/platform combinations, not just the single matched source.

### 🎬 Seasonal Stream Improvements

- **Correct Season Search:** Season 2+ queries prepend season-specific variants ("2. Sezon", "Season 2", "S2") so plugins locate the right season instead of defaulting to Season 1.

- **Smart Season Matching (`CsTitleMatcher`):** Results are scored with +0.35 for matching the target season, -0.60 for wrong season, and -0.50 for implicit Season 1 when Season > 1 is requested.

- **`targetSeason` Propagation:** Season metadata is correctly propagated from the UI layer through the streaming engine to plugin calls, eliminating the default Season 1 fallback for multi-season series.
