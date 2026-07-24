# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📅 Yayın Takvimi — Platform Başına Ayrı Takvim
- **TMDB / AniList / MAL için ayrı yayın takvimleri:** Her platform artık bağımsız bir ViewModel'e sahip. Platform değiştirince birinin verisi diğerini bozmuyor.
- **Race condition düzeltildi:** `init { loadSchedule() }` (AniList verisi) ile `LaunchedEffect` (TMDB verisi) aynı anda çalışarak birbirinin verisini ezip atıyordu. Yeni Job yönetimi ile önceki istek otomatik iptal ediliyor.

### 🖼️ "Yakında Yayında" Şeridi — Görsel ve Veri Düzeltmeleri
- **TMDB posterleri artık yükleniyor:** `discover/tv?first_air_date.gte=today` (duyurulmuş ama posteri olmayan içerikler) yerine `tv/on_the_air` (bu hafta gerçekten yayında, posterli içerikler) ve `movie/upcoming` kullanılmaya başlandı.
- **Null poster fallback eklendi:** Poster URL'si olmayan içerikler için başlık baş harfleri ile görsel placeholder gösteriliyor.
- **Veri filtreleme iyileştirildi:** Postersiz öğeler şeride dahil edilmiyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📅 Airing Calendar — Per-Platform Separate Calendars
- **Independent calendars for TMDB / AniList / MAL:** Each platform now has its own ViewModel instance with a unique key. Switching platforms no longer corrupts each other's calendar data.
- **Race condition fixed:** `init { loadSchedule() }` (AniList data) and `LaunchedEffect` (TMDB data) were running concurrently, with whichever finished last overwriting the other's data. New Job management cancels previous in-flight requests automatically.

### 🖼️ "Airing Soon" Strip — Image & Data Fixes
- **TMDB posters now load correctly:** Replaced `discover/tv?first_air_date.gte=today` (newly announced shows often without posters) with `tv/on_the_air` (currently airing shows with poster images) and `movie/upcoming`.
- **Null poster fallback added:** Items without a poster URL now display title initials as a visual placeholder.
- **Data filtering improved:** Items without poster images are excluded from the strip.
