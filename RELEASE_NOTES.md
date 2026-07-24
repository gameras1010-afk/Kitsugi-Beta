# Kitsugi v2.4.96 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Standartlaştırılmış Medya Galerisi — Karakter, Ekip & Stüdyo
- **Tam GalleryItem Migrasyonu:** Karakter, Ekip ve Stüdyo detay sayfaları artık tamamen modern `GalleryItem` tabanlı galeri sistemini kullanıyor.
- **Platform Kaynak Rozetleri:** Açık galeri görüntüsünün sol alt köşesinde hangi platformdan geldiği **renkli rozet** ile kesin olarak belirtiliyor:
  - 🟣 **Fanart.tv** — Mor rozet
  - 🟢 **TMDB** — Yeşil rozet
  - 🔵 **Jikan (MyAnimeList)** — Mavi rozet
  - 🔷 **AniList** — AniList mavisi rozet
  - 🔴 **Simkl** — Kırmızı rozet
- **Kategorili Filtre Sekmeleri:** Galeride birden fazla kategori varsa üst kısımda filtre sekmeleri belirir: Logo · Arka Plan · Poster · Karakter · Küçük Resim.
- **Koşullu Galeri Butonu:** Galeri butonu yalnızca o sayfa için gerçek resim verisi mevcut olduğunda görünür.
- **Yatay & Dikey Uyum:** Tüm detay sayfalarında hem portre hem de yatay ekran düzeninde tam uyum.

### 🎨 Fanart.tv & Çoklu Kaynak Galeri Entegrasyonu
- **Birleşik Galeri:** Fanart.tv, TMDB ve Jikan'dan gelen tüm görseller tek bir galeri yapısında toplanıp kategorilere ayrılıyor.
- **Akıllı Kategori Geçişi:** Bir görsele tıklandığında galeri diyaloğu otomatik olarak o kategoriye açılıyor.
- **Küçük Resim Şeridi:** Galeri altındaki thumbnail şeridi seçili kategoriye göre filtreleniyor ve seçili resme otomatik kaydırıyor.
- **Fanart.tv Ayarları:** Ayarlar → Entegrasyonlar altından API anahtarı girilebilir, özellik açılıp kapatılabilir.

### ⚡ APK Derleme Hızlandırması
- **`clean` Kaldırıldı:** Her derlemede sıfırdan başlama zorunluluğu kaldırıldı; Gradle yalnızca değişen dosyaları yeniden derliyor.
- **Paralel Build:** FOSS ve GMS varyantları artık aynı anda, paralel olarak derleniyor.
- **Configuration Cache:** Gradle proje yapısını önbelleğe alıyor — tekrarlayan derlemelerde yapılandırma süresi neredeyse sıfır.
- **Akıllı Fallback:** Hızlı derleme başarısız olursa sistem otomatik olarak güvenli `clean build`'e geçiyor.
- **Kotlin Incremental Düzeltmesi:** `useClasspathSnapshot=true` ile deprecated flag temizlendi, ABI snapshot tabanlı derleme aktif.

### 🛡️ Sistem Genelinde NSFW Flulaştırma
- **Merkezi `KitsugiNsfwImage` Bileşeni:** Tüm el ile yazılmış blur kullanımları merkezi bileşene taşındı.
- **Tutarlı Otomatik Gizleme:** Keşfet, Profil ve tüm medya ekranlarında yetişkin içerikler kullanıcı tercihine göre sistem genelinde flulaştırılıyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Standardized Media Gallery — Character, Staff & Studio
- **Full GalleryItem Migration:** Character, Staff, and Studio detail pages now fully use the modern `GalleryItem`-based gallery architecture.
- **Platform Source Badges:** Every open gallery image displays a **colored badge** in the bottom-left corner showing exactly which platform it came from:
  - 🟣 **Fanart.tv** — Purple badge
  - 🟢 **TMDB** — Green badge
  - 🔵 **Jikan (MyAnimeList)** — Blue badge
  - 🔷 **AniList** — AniList blue badge
  - 🔴 **Simkl** — Red badge
- **Category Filter Tabs:** When a gallery contains multiple categories, scrollable filter tabs appear at the top: Logo · Backdrop · Poster · Character · Thumbnail.
- **Conditional Gallery Button:** Gallery button is only shown when real image data is available for that entity.
- **Portrait & Landscape Support:** Full layout compatibility across all detail pages in both orientations.

### 🎨 Fanart.tv & Multi-Source Gallery Integration
- **Unified Gallery:** Assets from Fanart.tv, TMDB, and Jikan are merged and categorized into a single unified gallery structure.
- **Smart Category Pre-selection:** Tapping an image opens the gallery dialog with the correct category tab automatically pre-selected.
- **Synchronized Thumbnail Strip:** The bottom thumbnail strip filters by selected category and auto-scrolls to the current image.
- **Settings Integration:** Fanart.tv API key and toggle available under Settings → Integrations.

### ⚡ APK Build Speed Improvements
- **`clean` Removed:** No more full rebuild from scratch every time — Gradle only recompiles changed files.
- **Parallel Builds:** FOSS and GMS variants now compile simultaneously using all available CPU threads.
- **Configuration Cache:** Gradle caches project configuration — repeated builds skip re-evaluation entirely.
- **Smart Fallback:** If the fast build fails, the system automatically falls back to a safe clean build.
- **Kotlin Incremental Fix:** Deprecated `useClasspathSnapshot=false` flag removed; ABI snapshot-based incremental compilation enabled.

### 🛡️ System-Wide NSFW Blurring
- **Centralized `KitsugiNsfwImage` Component:** All manual `Modifier.blur` calls replaced with a single centralized wrapper.
- **Consistent Automated Blur:** Adult content is automatically blurred across Explore, Profile, and all media screens based on user settings.
