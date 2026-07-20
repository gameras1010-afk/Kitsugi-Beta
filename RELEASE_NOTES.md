# Kitsugi v2.4.0-beta Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎉 Yenilikler ve Eklenen Özellikler
- **Otomatik Uygulama Güncelleme Sistemi**: GitHub Releases altyapısı ile entegre, canlı indirme yüzdesi (MB/s) gösteren, modern ve glassmorphic Jetpack Compose diyalog arayüzü entegre edildi.
- **Anime & Manga Takip Senkronizasyonu**: AniList, MyAnimeList ve Simkl hesap bağlantıları ile anlık liste ve bölüm takibi iyileştirildi.
- **Gelişmiş Medya Oynatıcı Engine**: Media3 ve MPV tabanlı çoklu motor desteği, altyazı dil önceliklendirmeleri ve otomatik gain booster geliştirildi.
- **Manga & Webtoon Okuyucu Katmanı**: Cloudstream, Mihon ve Kotatsu (1300+ kaynak) eklenti katmanı desteğiyle sorunsuz oku/izle akışı sağlandı.

### 🛠️ Hata Düzeltmeleri
- **Compose Scope Çakışmaları**: Güncelleme diyaloglarında oluşan Composable scope ve derleme tipi uyuşmazlıkları giderildi.
- **Tip Güvenliği ve Durum Yönetimi**: `UpdateUiState.Failed` durum mimarisi standart kütüphane çakışmalarını önleyecek şekilde yeniden yapılandırıldı.
- **Paket Yükleyici İzinleri**: Android 8.0+ (API 26+) sürümlerinde APK indirme sonrası güvenli `FileProvider` ve `REQUEST_INSTALL_PACKAGES` akışı doğrulandı.

### ⚡ Performans ve İyileştirmeler
- **Arka Plan İndirme Yöneticisi**: Büyük boyutlu dosya indirmelerinde bellek optimizasyonu sağlandı.
- **Koyu Tema ve Akıcı Animasyonlar**: Tüm diyaloglar ve ana ekran bileşenleri Kitsugi tasarım token'larına uygun olarak güncellendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎉 New Features & Enhancements
- **Automated In-App Update System**: Fully integrated with GitHub Releases API, featuring a modern glassmorphic Jetpack Compose update dialog with real-time download progress tracking (MB/s).
- **Anime & Manga Sync Engine**: Improved OAuth list sync for AniList, MyAnimeList, and Simkl accounts with real-time episode progress tracking.
- **Advanced Dual-Engine Video Player**: Hardware-accelerated playback with Media3 + MPV engines, prioritized subtitle language selection, and auto gain audio boosting.
- **Unified Manga & Webtoon Reader**: Integrated support for Cloudstream, Mihon/Tachiyomi, and Kotatsu (1300+ sources) extensions.

### 🛠️ Bug Fixes
- **Compose Scope Resolution**: Resolved compiler scope errors and layout parameter syntax mismatches across update dialog composables.
- **Type-Safe State Machine**: Refactored `UpdateUiState.Failed` architecture to avoid standard library naming collisions.
- **Package Installer Permissions**: Verified Android 8.0+ (API 26+) secure `FileProvider` URI generation and `REQUEST_INSTALL_PACKAGES` flow.

### ⚡ Performance & Polish
- **Background Downloader**: Memory optimization during high-speed APK asset downloading.
- **Dark Mode & Fluid Animations**: Polished all dialog surfaces and screen components using Kitsugi design tokens.
