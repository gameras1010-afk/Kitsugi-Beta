# Kitsugi v2.4.25 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 👤 Profil Sekmesi Modernizasyonu & Platform Entegrasyonu
- **Gelişmiş Favoriler Bottom Sheet (`FavoritesExpandedBottomSheet`)**: AniList, MyAnimeList (MAL) ve Simkl platformlarındaki favori kategorileri (Anime, Manga, Karakter, Ekip/Personel, Son İzlenenler) için modern ve genişletilebilir bir alttan açılır pencere (bottom sheet) eklendi.
- **"Tümünü Gör" Butonları**: Tüm profil sekmelerindeki favori kartlarının başlıklarına "Tümünü Gör (Sayı)" butonu yerleştirilerek bu pencerelerin kolayca açılması sağlandı.
- **Kütüphane İstatistikleri Entegrasyonu**: Ayarlar sekmesinde bulunan "Kütüphane İstatistikleri" ve "Favorilerim" sekmeleri tamamen kaldırılarak doğrudan profil sekmesindeki her bir platformun (AniList, MAL, Simkl) içine entegre edildi.
- **Görsel Dağılım Çubukları (`SegmentedDistributionBar`)**: İzleme ve okuma durumlarının (İzliyor, Tamamlandı, Planlandı vb.) oranlarını görsel olarak gösteren renkli, segmentli dağılım çubukları eklendi.
- **Sabit Platform Seçici (Sticky Sub-Tab Bar)**: Kaydırma yapıldığında platform seçici barın (`AniList`, `MyAnimeList`, `Simkl`) gizlenmesi engellenerek ekranın üstünde sabit kalması sağlandı.
- **Tıklanabilir Aktivite & Geçmiş Kartları**: AniList aktivite akışındaki kartlar ile Simkl izleme geçmişindeki ögeler tıklanabilir hale getirilerek doğrudan ilgili medya detay sayfalarına yönlendirme (Deep-linking) sağlandı.

### ⚡ Arama Sayfası İyileştirmeleri (Göreceli Arama & Fuzzy)
- **Göreceli Arama (Tümü) Tekilleştirme Düzeltmesi**: Farklı platformlardan gelen benzer isimli sonuçların (MAL, AniList, TMDB) tekilleştirme sırasında yanlışlıkla silinmesi sorunu çözüldü; sonuçlar artık adil şekilde harmanlanarak listeleniyor.
- **Akıllı Sorgu Temizleyici & Fallback Arama**: Birleşik veya noktalama işaretli sorgularda (`demon-slayer` veya `OnePiece` gibi) kelimelerin akıllıca ayrıştırılması ve boş sonuç dönüldüğünde otomatik fallback araması yapılması sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 👤 Profile Dashboard Modernization & Platform Integration
- **Advanced Favorites Bottom Sheet (`FavoritesExpandedBottomSheet`)**: Introduced a modern, expandable bottom sheet using `KitsugiSheetOrDialog` for all favorite categories (Anime, Manga, Characters, Staff, and Recent History) across AniList, MAL, and Simkl.
- **"See All" Navigation**: Added a "See All (Count)" button to the header of every horizontal favorites section for instant access to the full list.
- **Consolidated Library Stats**: Moved "Library Statistics" and "My Favorites" out of the settings screen and fully integrated them directly into the profile tabs for AniList, MAL, and Simkl.
- **Segmented Distribution Bars**: Added beautiful visual color-coded distribution bars showing watch/read status ratios (Watching, Completed, Planned, etc.) on profile stats cards.
- **Sticky Platform Sub-Tab Bar**: Locked the platform selector tab bar (`AniList`, `MyAnimeList`, `Simkl`) at the top of the Profile screen so it stays fixed upon scrolling.
- **Interactive Activity & History Feeds**: Enabled clickability on AniList activity cards and Simkl recent history items to navigate directly to their respective media detail pages.

### ⚡ Search Enhancements (Fuzzy & Relative Blending)
- **Deduplication Fix in Relative Search**: Fixed a critical issue where TMDB and AniList results were erroneously scrubbed by the title deduplication logic when MAL results matched. They are now harmoniously blended side-by-side.
- **Smart Query Cleaning & Fallback**: Automatically cleans concatenated or improperly formatted queries (e.g. `OnePiece` -> `One Piece`, `demon-slayer` -> `demon slayer`) and retries the search as a fallback if no direct matches are found.
