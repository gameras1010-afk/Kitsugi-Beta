# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### 📅 TMDB Yayın Takvimi Entegrasyonu ve Platform Senkronizasyonu
- **TMDB Sekmesi ve Genel İçerik Desteği**: TMDB sekmesindeki "Yakında Yayında" (Airing Soon) ve Yayın Takvimi bölümleri artık sadece animeleri değil, TMDB üzerindeki tüm popüler/yaklaşan genel dizi ve filmleri (her şeyi) gösterecek şekilde güncellendi.
- **Platform Akıllı Yönlendirme**: Yayın Takvimi, hangi sekmeden açıldığını tespit ederek dinamik yönlendirme gerçekleştirir:
  - **TMDB Sekmesinde**: Tüm vizyondaki dizi/filmleri gösterir ve detayları TMDB üzerinden açar.
  - **AniList Sekmesinde**: Animeleri gösterir ve AniList detay sayfasını açar.
  - **MAL Sekmesinde**: Animeleri gösterir ve tıklandığında doğrudan MyAnimeList (MAL) detay sayfasını açar.
- **Performans ve Kararlılık**: TMDB API keşif istekleri optimize edilerek sayfa yüklenme hızları artırıldı ve takvim sayfasındaki veri tutarsızlıkları tamamen giderildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 📅 TMDB Airing Calendar Integration & Platform Sync
- **TMDB Tab & General Content Support**: Updated the "Yakında Yayında" (Airing Soon) and Airing Calendar sections under the TMDB tab to display all general TV shows and movies on the air, not just anime.
- **Smart Platform Routing**: The Airing Calendar dynamically adapts based on the active explore tab:
  - **TMDB Tab**: Shows all general movies/TV shows and opens details via TMDB.
  - **AniList Tab**: Shows airing anime and opens details via AniList.
  - **MAL Tab**: Shows airing anime and routes details directly to MyAnimeList (MAL).
  - **Performance & Stability**: Optimized TMDB API discovery requests to improve page load times and eliminated data inconsistencies in the calendar stream.
