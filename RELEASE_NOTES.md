# Kitsugi v2.4.15 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve İyileştirmeler
- **Profil Dashboard Tam Entegrasyonu**: AniList, MyAnimeList ve Simkl profil sekmeleri artık eksiksiz ve interaktif veri ile çalışıyor.
- **AniList & MAL Favorilere Tıklanabilirlik**: Profil sekmesindeki favori animeler, mangalar, karakterler ve staff öğelerine artık tıklanabiliyor; doğrudan ilgili detay sayfasına yönlendirme yapılıyor.
- **MyAnimeList Manga İstatistikleri**: MAL profilinde artık hem anime hem manga istatistikleri (tamamlanan, planlanan, ortalama skor vb.) ayrı ayrı doğru şekilde gösteriliyor.
- **Simkl Profil İstatistikleri**: Simkl sekmesi artık toplam anime/dizi/film sayıları, izleme durumu dağılımı ve ortalama skor bilgilerini gerçek API verisinden çekiyor; boş profil sorunu çözüldü.

### 🛠️ Hata Düzeltmeleri
- Profil sekmesine eklenen favori navigasyon callback'leri sayesinde `onFavoriteMediaClick`, `onFavoriteCharacterClick`, `onFavoriteStaffClick` bağlantıları tamamlandı.
- `FavoritesHorizontalSection` bileşenine tıklama desteği (`onItemClick` lambda) eklenerek daha önce tıklanamayan favori kartlar düzeltildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Improvements
- **Full Profile Dashboard Integration**: AniList, MyAnimeList, and Simkl profile tabs now display complete, interactive data.
- **Clickable Favorites (AniList & MAL)**: Favorite anime, manga, characters, and staff items in the profile are now tappable, navigating directly to their respective detail screens.
- **MyAnimeList Manga Statistics**: MAL profile now correctly displays both anime and manga statistics (completed, planned, mean score, etc.) in separate sections.
- **Simkl Profile Statistics**: The Simkl tab now fetches real stats (total anime/shows/movies, status distribution, average score) directly from the API — empty profile issue resolved.

### 🛠️ Bug Fixes
- Wired `onFavoriteMediaClick`, `onFavoriteCharacterClick`, and `onFavoriteStaffClick` navigation callbacks through the full composable chain.
- Added `onItemClick` lambda to `FavoritesHorizontalSection`, enabling previously non-clickable favorite cards.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)


