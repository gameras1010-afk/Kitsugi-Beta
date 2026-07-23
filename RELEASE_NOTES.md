# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### ⏳ TMDB Yakında Yayında & Sayaç Entegrasyonu
- **TMDB Sekmesine Canlı Geri Sayım**: TMDB sekmesindeki "Yakında Yayında" bölümü, artık MAL ve AniList sekmelerinde olduğu gibi canlı geri sayım sayaçlarıyla çalışıyor.
- **Tüm Medya Türleri İçin Sayaç Desteği**: Sadece anime değil, tüm dizi ve filmler için de vizyona/yayına kalan süre gün, saat ve dakika cinsinden anlık gösteriliyor.
- **Akıllı Yayın Durumu**: Bölüm numarası olmayan genel dizi ve filmler yayınlandığı an sayaç otomatik olarak "Yayınlandı" durumuna geçiyor.
- **Yatay Kart Paritesi**: Dikey kartlar yerine, premium yatay kaydırılabilir `AiringSoonHorizontalCard` tasarımına geçilerek görsel bütünlük sağlandı.
- **Kategori Düzenlemesi**: "Yakında Yayında" butonu, genel bir kapsam kazandığı için "Dizi ve Film" kategorisi altına taşındı.
- **+18 İçerik Blurlama**: TMDB tablosundaki +18 içeriklerin afiş görselleri, hassas içerik blurlama ayarı aktif olduğunda artık tüm arayüzde doğru bir şekilde blurlanıyor.

### 📱 Benim Listem Sayfası — Başlık ve Düzen Değiştirici
- **Pürüzsüz Daralan Başlık (Collapsing Header)**: Liste sayfasında aşağı doğru kaydırırken küt kesilmek yerine yumuşak ve akıcı bir şekilde daralan/genişleyen modern bir başlık yapısı entegre edildi.
- **Hızlı Düzen Değiştirme Butonu**: Başlığın sağ üst köşesine eklenen döngüsel buton sayesinde (Kompakt, Rahat, Büyük, 2 Sütun Izgara) görünümleri arasında tek tıkla geçiş yapabilme kolaylığı sağlandı.
- **Kalıcı Arayüz Seçimleri**: Düzen tercihleri DataStore üzerinde saklanarak uygulama kapatılıp açılsa bile korunuyor.
- **Ayarlar Temizliği**: Genel ayarlar sekmesindeki karmaşık ve mükerrer liste düzeni yapılandırmaları kaldırılarak arayüz sadeleştirildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### ⏳ TMDB Airing Soon & Countdown Integration
- **Live Countdowns on TMDB**: The "Airing Soon" section in the TMDB tab now features live countdown timers, matching the premium MAL/AniList layout.
- **Support for All Media Types**: Real-time remaining time (days, hours, minutes) is now displayed for all upcoming TV shows, movies, and anime.
- **Smart Release States**: For movies and general series without episode numbers, the countdown smoothly transitions to "Released" once the air time passes.
- **Horizontal Card Parity**: Replaced vertical listings with premium horizontal scrolling `AiringSoonHorizontalCard`s for absolute visual parity.
- **Category Cleanup**: Moved the "Airing Soon" category chip to the general "TV & Movies" subcategory.
- **18+ Content Blurring**: Poster images for 18+ content from TMDB are now correctly blurred across all layouts when the sensitive content blur setting is active.

### 📱 My List Screen — Header & Layout Switcher
- **Smooth Collapsing Header**: Implemented a fluid, scroll-aware collapsing header animation for the My List screen to replace the abrupt snapping behavior.
- **Dynamic Layout Switcher Button**: Added a fast cycle button (Compact, Comfortable, Large, 2-Column Grid) to the header, allowing quick layout toggles.
- **Persistent Preferences**: User layout preferences are saved to DataStore, ensuring they persist across app restarts.
- **Settings De-cluttering**: Removed legacy list layout settings from the global Preferences Dialog to keep the settings menu clean.
