# Kitsugi v2.4.19 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve İyileştirmeler
- **Profil Dashboard Sadeleştirmesi**: Profil ekranındaki "Yerel" sekmesi tamamen kaldırıldı; profil artık sadece platform bazlı istatistiklere ve favorilere odaklanıyor (AniList / MAL / Simkl).
- **MAL Profili Zenginleştirildi**: MyAnimeList profil sayfasına favori Personeller (Seiyuu/Staff) bölümü eklendi — Jikan API'den `people` favorileri çekiliyor ve Staff detay sayfasına tıklanabilir.
- **Simkl Profili Zenginleştirildi**: Simkl profil sayfasına "Son İzlenenler" bölümü eklendi — `/sync/history` API'si üzerinden son 20 öğe poster kartlarıyla listeleniyor.
- **AniList Favorileri Düzeltildi**: AniList favori anime/mangalara tıklanınca artık doğru AniList ID (100M offset) kullanılarak detay sayfası doğru şekilde açılıyor.
- **Profil Üst Çubuğu Davranışı**: Profil sekmesindeki platform seçici (AniList / MAL / Simkl) artık Keşfet ekranındaki gibi aşağı kaydırınca gizleniyor, yukarı kaydırınca tekrar görünüyor.
- **Beyaz Alan Giderildi**: Profil ekranının üstünde oluşan gereksiz boş alan (çift Scaffold + statusBarsPadding sorunu) tamamen giderildi.

### 🛠️ Hata Düzeltmeleri
- AniList favori medya ID'si artık 100_000_000 offset'li olarak iletiliyor — yanlış Jikan fallback yerine doğrudan AniList API sorgusu yapılıyor.
- MAL Jikan `favorites` parse bloğundaki `favStaff` scope sorunu giderildi.
- Simkl geçmişi için sessiz hata yönetimi eklendi (API başarısız olursa profil yüklenmeye devam eder).

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Improvements
- **Profile Dashboard Streamlining**: "Local" tab fully removed from the profile screen; profile now focuses exclusively on platform-based stats and favorites (AniList / MAL / Simkl).
- **MAL Profile Enriched**: Added favorite Staff/People section to MyAnimeList profile — fetched via Jikan `people` favorites array, tappable with Staff detail navigation.
- **Simkl Profile Enriched**: Added "Recently Watched" section to Simkl profile — powered by `/sync/history` API, displaying poster cards for the last 20 watched items.
- **AniList Favorites Fixed**: Clicking AniList favorite anime/manga now correctly uses the 100M-offset AniList ID to open the detail page instead of incorrectly falling back to Jikan.
- **Scroll-Aware Top Bar**: Profile platform selector (AniList / MAL / Simkl) now hides on scroll-down and reveals on scroll-up, matching Explore screen behavior.
- **Top Spacing Fixed**: Removed excessive top whitespace caused by double Scaffold + statusBarsPadding in the profile layout.

### 🛠️ Bug Fixes
- AniList favorite media now passed with 100_000_000 offset so detail screen queries AniList API directly instead of Jikan fallback.
- Fixed `favStaff` variable scope issue in MAL Jikan favorites parse block.
- Added silent error handling for Simkl history fetch so profile loads even if history API fails.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
