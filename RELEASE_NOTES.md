# Kitsugi v2.4.9-beta Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎉 Yenilikler ve Eklenen Özellikler
- **Performans ve Listem Aktarım İyileştirmesi**: AniList, MyAnimeList ve Simkl veri aktarımlarında binlerce kaydı tek bir veritabanı işleminde (Batch Transaction) işleyen yeni mimariye geçildi. Aktarım sırasındaki kasma, donma ve takılmalar tamamen giderildi.
- **Detay Sayfaları Tasarım Temizliği**: Detay sayfalarında banner rozetleri ile tekrarlayan `Dizi • 2010` vb. gereksiz alt başlık metinleri gizlendi.
- **İstatistik Kartı Sadeleştirmesi**: Detay ekranlarında rozetlerle tekrarlayan Kaynak, Yıl ve +18 bilgi kartları kaldırıldı; alan kullanımı optimize edildi.
- **Gelişmiş Güncelleme Yönetimi**: Ayarlar ekranına Otomatik Güncelleme kontrol anahtarı ve manuel "Şimdi Denetle" butonu eklendi.

### 🛠️ Hata Düzeltmeleri
- **Arka Plan İşlem Ayrıştırması**: Tüm liste çekme ve eşitleme coroutine'leri UI iş parçacığından arka plan `Dispatchers.IO` kanalına taşınarak arayüzün akıcılığı (60/120 FPS) korundu.
- **Sürüm ve Not Senkronizasyonu**: GitHub Release başlığı, APK sürüm numarası ve uygulama içi güncelleme notları %100 otomatik senkronize edildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎉 New Features & Enhancements
- **High-Performance List Imports**: Massive performance overhaul for AniList, MAL, and Simkl sync pipelines using single-transaction batch database operations. Completely eliminated lag and stuttering during large list imports.
- **Refined Detail Page Layout**: Cleaned up redundant metadata subtitles (e.g. `Series • 2010`) that were already displayed as hero badges.
- **Streamlined Stats Grid**: Removed redundant stat cards (Source, Year, Adult status) to maximize usable content space.
- **Advanced Update Controls**: Added automatic update check toggle and manual check button in system settings.

### 🛠️ Bug Fixes
- **UI Thread Offloading**: All data import coroutines now run strictly on `Dispatchers.IO`, ensuring smooth frame rates without UI locks.
- **Metadata Synchronization**: Guaranteed 100% version alignment between local APK build tags and remote GitHub release changelogs.
