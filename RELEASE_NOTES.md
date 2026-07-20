# Kitsugi v2.4.27 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 👤 Profil Sekmesi Modernizasyonu & Platform Entegrasyonu
- **Gelişmiş Favoriler Bottom Sheet (`FavoritesExpandedBottomSheet`)**: AniList, MyAnimeList (MAL) ve Simkl platformlarındaki favori kategorileri (Anime, Manga, Karakter, Ekip/Personel, Son İzlenenler) için modern ve genişletilebilir bir alttan açılır pencere (bottom sheet) eklendi.
- **"Tümünü Gör" Butonları**: Tüm profil sekmelerindeki favori kartlarının başlıklarına "Tümünü Gör (Sayı)" butonu yerleştirilerek bu pencerelerin kolayca açılması sağlandı.
- **Kütüphane İstatistikleri Entegrasyonu**: Ayarlar sekmesinde bulunan "Kütüphane İstatistikleri" ve "Favorilerim" sekmeleri tamamen kaldırılarak doğrudan profil sekmesindeki her bir platformun (AniList, MAL, Simkl) içine entegre edildi.
- **Görsel Dağılım Çubukları (`SegmentedDistributionBar`)**: İzleme ve okuma durumlarının (İzliyor, Tamamlandı, Planlandı vb.) oranlarını görsel olarak gösteren renkli, segmentli dağılım çubukları eklendi.
- **Sabit Platform Seçici (Sticky Sub-Tab Bar)**: Kaydırma yapıldığında platform seçici barın (`AniList`, `MyAnimeList`, `Simkl`) gizlenmesi engellenerek ekranın üstünde sabit kalması sağlandı.
- **Tıklanabilir Aktivite & Geçmiş Kartları**: AniList aktivite akışındaki kartlar ile Simkl izleme geçmişindeki ögeler tıklanabilir hale getirilerek doğrudan ilgili medya detay sayfalarına yönlendirme (Deep-linking) sağlandı.
- **Otomatik Hesap Bağlantı Yönlendirmesi**: Profil sekmesinde AniList, MAL veya Simkl hesabı bağlı değilken "Hesabı Bağla" butonuna tıklandığında artık ayarlar ekranı yerine doğrudan tarayıcıdaki OAuth giriş sayfası açılıyor (Listem sekmesiyle aynı davranış).
- **MAL Aktivite Filtresi Düzeltmesi**: MAL aktivite sekmesinde yalnızca gerçekten MyAnimeList kaynağından gelen kayıtlar görüntüleniyor; Simkl senkronizasyonundan gelen çapraz-platform kayıtların sızması engellendi.
- **Profil Alt Sekmesi Kalıcılığı**: Profil sekmesinde AniList, MAL veya Simkl alt sekmelerinden birine geçip herhangi bir içeriğin detay sayfasına gidip geri döndüğünde, aktif sekme AniList'e sıfırlanmak yerine son seçilen sekme korunuyor.

### 📱 Yatay Mod & Tablet Navigasyonu
- **Navigasyon Barı Buton Dağılımı**: Tablet ve geniş ekranlarda yatay (landscape) modda sol navigasyon barındaki butonlar (Keşfet, Listem, Arama, Profil, Ayarlar) ekranın üstüne yığılmak yerine ekran yüksekliğine göre otomatik ve dengeli biçimde dağılıyor.

### ⚡ Arama Sayfası İyileştirmeleri (Göreceli Arama & Fuzzy)
- **Göreceli Arama (Tümü) Tekilleştirme Düzeltmesi**: Farklı platformlardan gelen benzer isimli sonuçların (MAL, AniList, TMDB) tekilleştirme sırasında yanlışlıkla silinmesi sorunu çözüldü; sonuçlar artık adil şekilde harmanlanarak listeleniyor.
- **Akıllı Sorgu Temizleyici & Fallback Arama**: Birleşik veya noktalama işaretli sorgularda (`demon-slayer` veya `OnePiece` gibi) kelimelerin akıllıca ayrıştırılması ve boş sonuç dönüldüğünde otomatik fallback araması yapılması sağlandı.

### ⚙️ Güncelleme Ekranı Tasarım & Kaydırma İyileştirmesi
- **Genişletilmiş Görünüm**: Güncelleme penceresi artık ekranın %85'ini kaplayacak şekilde yukarıdan açılır.
- **Dinamik Kaydırma**: Değişiklik notları kutusu ekran yüksekliğine göre otomatik genişler ve kaydırma (scroll) sorunu olmadan tüm notların okunabilmesini sağlar.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 👤 Profile Dashboard Modernization & Platform Integration
- **Advanced Favorites Bottom Sheet (`FavoritesExpandedBottomSheet`)**: Introduced a modern, expandable bottom sheet using `KitsugiSheetOrDialog` for all favorite categories (Anime, Manga, Characters, Staff, and Recent History) across AniList, MAL, and Simkl.
- **"See All" Navigation**: Added a "See All (Count)" button to the header of every horizontal favorites section for instant access to the full list.
- **Consolidated Library Stats**: Moved "Library Statistics" and "My Favorites" out of the settings screen and fully integrated them directly into the profile tabs for AniList, MAL, and Simkl.
- **Segmented Distribution Bars**: Added beautiful visual color-coded distribution bars showing watch/read status ratios (Watching, Completed, Planned, etc.) on profile stats cards.
- **Sticky Platform Sub-Tab Bar**: Locked the platform selector tab bar (`AniList`, `MyAnimeList`, `Simkl`) at the top of the Profile screen so it stays fixed upon scrolling.
- **Interactive Activity & History Feeds**: Enabled clickability on AniList activity cards and Simkl recent history items to navigate directly to their respective media detail pages.
- **Direct Account Connection Redirect**: On the Profile tab, tapping "Connect Account" when a service (AniList, MAL, Simkl) is not linked now opens the browser OAuth page directly instead of navigating to Settings — matching the behavior from the My List tab.
- **MAL Activity Feed Fix**: The MAL activity tab now exclusively shows entries whose source is `myanimelist`, preventing cross-platform Simkl-synced entries (which may carry a MAL ID) from appearing there.
- **Profile Sub-Tab Persistence**: When navigating from AniList, MAL, or Simkl sub-tabs into a media detail page and back, the active sub-tab is now preserved instead of resetting to AniList.

### 📱 Landscape Mode & Tablet Navigation
- **Navigation Rail Button Distribution**: On tablets and wide screens in landscape mode, the left-side navigation rail buttons (Explore, My List, Search, Profile, Settings) now spread evenly and dynamically across the full screen height instead of stacking at the top.

### ⚡ Search Enhancements (Fuzzy & Relative Blending)
- **Deduplication Fix in Relative Search**: Fixed a critical issue where TMDB and AniList results were erroneously scrubbed by the title deduplication logic when MAL results matched. They are now harmoniously blended side-by-side.
- **Smart Query Cleaning & Fallback**: Automatically cleans concatenated or improperly formatted queries (e.g. `OnePiece` -> `One Piece`, `demon-slayer` -> `demon slayer`) and retries the search as a fallback if no direct matches are found.

### ⚙️ Update Dialog Design & Scrolling Enhancements
- **Extended Layout**: The update bottom sheet now fills up to 85% of screen height.
- **Dynamic Scrolling**: The release notes container scales dynamically with weight, resolving scrolling issues on smaller screens/fonts and ensuring all changes can be read properly.
