# Kitsugi v2.4.17 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve İyileştirmeler
- **Profil Dashboard Sadeleştirmesi**: Profil ekranlarındaki (Yerel, AniList, MAL, Simkl) mükerrer \"Listem\" sekmeleri kaldırıldı. Profil sekmesi artık tamamen istatistiklere ve favorilere odaklanıyor.
- **MAL Profili Zenginleştirildi**: MyAnimeList profil sayfasına artık favori Personeller (Seiyuu/Staff) bölümü eklendi — Jikan API üzerinden `people` favorileri çekiliyor ve tıklanabilir Staff detay sayfasına yönlendiriyor.
- **Simkl Profili Zenginleştirildi**: Simkl profil sayfasına \"Son İzlenenler\" bölümü eklendi — `/sync/history` API endpoint'i aracılığıyla son izleme geçmişi kapaklı poster kartlarla listeleniyor.
- **Daha Temiz Navigasyon**: AniList profilinde \"Profil\" ve \"Aktivite\" sekmeleri korunarak kütüphane navigasyonunun tekrarlanması önlendi.
- **Favorilere Tıklanabilirlik**: Profil sekmesindeki favori animeler, mangalar, karakterler ve staff öğelerine tıklanarak doğrudan detay sayfalarına gidilmesi korundu.

### 🛠️ Hata Düzeltmeleri
- MAL Jikan `favorites` parse bloğunda `favStaff` scope sorunu giderildi.
- Simkl geçmişi için sessiz hata yönetimi eklendi (API başarısız olursa profil yüklenmeye devam eder).

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Improvements
- **Profile Dashboard Streamlining**: Removed redundant "My List" sub-tabs from all profile views (Local, AniList, MAL, Simkl). Profile tab now focuses exclusively on stats and favorites.
- **MAL Profile Enriched**: Added favorite Staff/People section to the MyAnimeList profile — fetched from Jikan `/users/{name}/favorites` `people` array, tappable with Staff detail navigation.
- **Simkl Profile Enriched**: Added "Recently Watched" section to the Simkl profile — powered by `/sync/history` API, displaying poster cards for the last 20 watched items.
- **Cleaner Navigation Flow**: Preserved "Profile" and "Activity" tabs for AniList, preventing duplicate library navigation.
- **Clickable Favorites**: Kept full support for clicking favorite anime, manga, characters, and staff items to navigate to details.

### 🛠️ Bug Fixes
- Fixed `favStaff` variable scope issue in MAL Jikan favorites parse block.
- Added silent error handling for Simkl history fetch so profile loads even if history API fails.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
