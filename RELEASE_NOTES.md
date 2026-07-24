# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — v2.4.86

### 🔍 Keşfet Ekranı — Metadata ve Görünüm İyileştirmeleri
- **Metadata Tekrarı Giderildi**: Mobil kartlarda tip/yıl bilgisi (ör. "ANIME • 2024") artık sadece altyazı satırında gösteriliyor; meta satırında aynı bilgi ikinci kez çıkmıyor.
- **Yıldız Puanı Formatı**: Tüm medya kartlarında puan artık `★ 8.1` formatında gösteriliyor, kaynak platforma bakılmaksızın (MAL, AniList, TMDB).
- **TV Ekranları Korundu**: Büyük ekran (Android TV) görünümünde tip/yıl bilgisi meta satırında görünmeye devam ediyor.

### 📺 TMDB Keşfet Kategorileri
- **"Trend Animeler"**: Popülerlik sırasına göre TMDB'den Japonca animasyon filmi ve dizisi (genre=16, lang=ja).
- **"Popüler Animeler"**: Oy sayısına göre TMDB Japanca anime içerikleri.
- **"En Yüksek Puanlı Animeler"**: Oy ortalamasına göre TMDB'den en beğenilen Japonca animeler (min. 200 oy).
- **Prefetch Senkronizasyonu**: Uygulama açılışındaki ön yükleme (prefetch), çalışma zamanı verisiyle tam olarak eşleştirildi; "En Yüksek Puanlı Animeler" ve "Yakında Yayında" bölümleri artık soğuk başlatmada da dolu geliyor.

### 🏷️ Kaynak Etiketleme
- Tüm medya kartlarında `toFriendlySourceLabel()` fonksiyonu üzerinden kaynak adı gösteriliyor (ör. "MyAnimeList", "AniList", "TMDB").

---

## 🇬🇧 ENGLISH RELEASE NOTES — v2.4.86

### 🔍 Explore Screen — Metadata & Display Improvements
- **No More Redundant Labels**: On mobile cards, type/year info (e.g. "ANIME • 2024") is now shown only in the subtitle row — no longer duplicated in the meta line below.
- **Star Score Format**: Scores across all media cards now display as `★ 8.1`, regardless of platform (MAL, AniList, TMDB).
- **TV Layout Preserved**: On large-screen / Android TV layouts, the type and year remain visible in the meta line as before.

### 📺 TMDB Discovery Categories
- **"Trend Animeler" (Trending Anime)**: Japanese animation (genre=16, lang=ja) sorted by popularity from TMDB.
- **"Popüler Animeler" (Popular Anime)**: TMDB Japanese anime sorted by vote count.
- **"En Yüksek Puanlı Animeler" (Top Rated Anime)**: Highest-rated TMDB Japanese anime (min. 200 votes).
- **Prefetch Sync Fix**: Startup prefetch now fully mirrors the runtime data pipeline — "Top Rated Anime" and "Airing Soon" sections populate correctly on cold launch.

### 🏷️ Source Labeling
- All media cards now display friendly source labels via `toFriendlySourceLabel()` (e.g. "MyAnimeList", "AniList", "TMDB").
