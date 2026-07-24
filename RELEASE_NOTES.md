# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔍 Keşfet Ekranı — Görünüm ve Düzen İyileştirmeleri
- **Metadata Tekrarı Giderildi:** Mobil (dikey ve yatay) kart tasarımlarında, halihazırda alt başlıkta yer alan tip ve yıl bilgileri (örneğin "ANIME • 2024") meta satırından temizlenerek gereksiz görsel kalabalık kaldırıldı.
- **TV Arayüzü Uyumluluğu:** Alt başlığı gösterilmeyen Android TV kart tasarımlarında tip ve yıl bilgilerinin meta satırında görünmeye devam etmesi sağlandı.

### ⚙️ Ön Yükleme Pipeline Senkronizasyonu
- **Startup Caching Hizalaması:** Uygulama açılışındaki önbellekleme (prefetch) mekanizması, çalışma zamanındaki TMDB ve Airing Schedule akışlarıyla tam uyumlu hale getirildi. 
- Bu sayede soğuk başlangıçlarda "En Yüksek Puanlı Animeler" ve "Yakında Yayında" şeritlerinin boş kalması engellendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔍 Explore Screen — Visual & Layout Cleanup
- **Removed Metadata Redundancy:** On mobile layouts (portrait and landscape cards), type and year details (e.g. "ANIME • 2024") are no longer duplicated in the metadata line, keeping the interface clean and concise.
- **Android TV Preserved:** On large-screen TV layouts where subtitles are hidden, type and year information remains visible in the metadata block.

### ⚙️ Prefetch Pipeline Alignment
- **Startup Cache Synchronization:** The background prefetching pipeline was aligned with runtime TMDB and Airing Schedule data fetching.
- This ensures sections like "Top Rated Anime" and "Airing Soon" populate immediately upon initial application launch.
