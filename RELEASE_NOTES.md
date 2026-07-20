# Kitsugi v2.4.29 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔧 Listem — Türkçe Metadata ve Otomatik Çeviri Düzeltmeleri
- **TMDB Türkçe Synopsis**: Listem'deki herhangi bir içeriğe tıklandığında açılan detay sayfasında artık TMDB'den doğru Türkçe açıklama çekiliyor. Daha önce `tmdbId` ve `realMalId` parametreleri TMDB servisine iletilmiyordu.
- **Simkl Kayıtları için TMDB Desteği**: Simkl üzerinden senkronize edilen Film, Dizi ve Anime içerikleri artık `tmdbId` değerini doğru şekilde TMDB'ye iletiyor; Türkçe başlık, tür ve açıklama sorunsuz geliyor.
- **AniList İçerikleri için Doğru MAL ID Çözümlemesi**: AniList kaynaklı içeriklerde 100 milyon+ offset'li `stableId` artık gerçek MAL ID olarak ARM/TMDB'ye iletilmiyor; yalnızca geçerli (< 100M) ID'ler Jikan ve TMDB sorguları için kullanılıyor.
- **Otomatik Çeviri Düzeltmesi**: TMDB'den synopsis geldikten sonra `Otomatik Çeviri` ayarı açıksa Google Translate döngüsü artık doğru şekilde tetikleniyor. Daha önce TMDB tamamlandığında çeviri hiç başlatılmıyordu.
- **Yarış Durumu Giderildi**: `fetchDetail` ve `fetchSynopsis` artık sıralı çalışıyor; TMDB'den Türkçe açıklama geldikten sonra `fetchSynopsis` bunu görüyor ve gereksiz İngilizce çeviri tetiklemiyor.

### 👤 Profil Sekmesi Modernizasyonu & Platform Entegrasyonu
- **Gelişmiş Favoriler Bottom Sheet**: AniList, MAL ve Simkl platformlarındaki favori kategorileri için modern ve genişletilebilir bir alttan açılır pencere eklendi.
- **"Tümünü Gör" Butonları**: Tüm profil sekmelerindeki favori kartlarının başlıklarına "Tümünü Gör (Sayı)" butonu eklendi.
- **Kütüphane İstatistikleri Entegrasyonu**: İstatistikler ve favoriler doğrudan profil sekmelerinin içine entegre edildi.
- **Görsel Dağılım Çubukları**: İzleme/okuma durumlarının oranlarını gösteren renkli segmentli çubuklar eklendi.
- **Sabit Platform Seçici (Sticky Sub-Tab Bar)**: Kaydırma yapıldığında platform seçici bar ekranın üstünde sabit kalıyor.
- **Tıklanabilir Aktivite & Geçmiş Kartları**: AniList aktivite kartları ve Simkl geçmiş ögeleri tıklanabilir hale getirildi.
- **Otomatik Hesap Bağlantı Yönlendirmesi**: Hesap bağlı değilken "Hesabı Bağla" butonuna tıklandığında doğrudan OAuth giriş sayfası açılıyor.
- **MAL Aktivite Filtresi Düzeltmesi**: MAL aktivite sekmesinde yalnızca MyAnimeList kaynaklı kayıtlar görüntüleniyor.
- **Profil Alt Sekmesi Kalıcılığı**: Detay sayfasından geri dönüldüğünde aktif platform sekmesi korunuyor.

### 📱 Yatay Mod & Tablet Navigasyonu
- **Navigasyon Barı Buton Dağılımı**: Tablet ve geniş ekranlarda navigasyon barı butonları ekran yüksekliğine göre dengeli biçimde dağılıyor.

### ⚡ Arama Sayfası İyileştirmeleri
- **Tekilleştirme Düzeltmesi**: Farklı platformlardan gelen benzer isimli sonuçların yanlışlıkla silinmesi sorunu çözüldü.
- **Akıllı Sorgu Temizleyici & Fallback Arama**: Birleşik veya noktalama işaretli sorgularda akıllı ayrıştırma ve otomatik fallback araması eklendi.

### ⚙️ Güncelleme Ekranı Tasarım & Kaydırma İyileştirmesi
- **Genişletilmiş Görünüm**: Güncelleme penceresi ekranın %85'ini kaplayacak şekilde açılır.
- **Yayınlanma Zamanı Gösterimi**: Güncellemelerin GitHub'da yayınlanma zamanı clock ikonuyla birlikte Türkçe formatında gösterilir.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔧 My List — Turkish Metadata & Auto-Translate Fixes
- **TMDB Turkish Synopsis**: Detail pages opened from the My List tab now correctly fetch Turkish descriptions from TMDB. Previously, `tmdbId` and `realMalId` parameters were not being passed to the TMDB service.
- **Simkl Entries TMDB Support**: Movies, TV shows and anime synced via Simkl now correctly forward `tmdbId` to TMDB, returning proper Turkish titles, genres and descriptions.
- **Correct MAL ID Resolution for AniList**: AniList entries with 100M+ offset `stableId` values are no longer forwarded as MAL IDs to ARM/TMDB. Only valid (< 100M) IDs are used for Jikan and TMDB lookups.
- **Auto-Translate Fix**: When a synopsis arrives from TMDB, the Google Translate loop now correctly fires if the "Auto Translate" setting is enabled. Previously, the translate step was never triggered after TMDB completion.
- **Race Condition Eliminated**: `fetchDetail` and `fetchSynopsis` now run sequentially. `fetchSynopsis` sees the TMDB Turkish synopsis and skips unnecessary English translation attempts.

### 👤 Profile Dashboard Modernization & Platform Integration
- **Advanced Favorites Bottom Sheet**: Expandable bottom sheet for all favorite categories (Anime, Manga, Characters, Staff, History) across AniList, MAL, and Simkl.
- **"See All" Navigation**: "See All (Count)" button added to every favorites section header.
- **Consolidated Library Stats**: Library Statistics and Favorites integrated directly into profile platform tabs.
- **Segmented Distribution Bars**: Visual color-coded distribution bars showing watch/read status ratios.
- **Sticky Platform Sub-Tab Bar**: Platform selector tab bar stays fixed at the top of the Profile screen when scrolling.
- **Interactive Activity & History Feeds**: AniList activity cards and Simkl history items are now clickable and navigate to media detail pages.
- **Direct Account Connection Redirect**: "Connect Account" now opens the browser OAuth page directly.
- **MAL Activity Feed Fix**: MAL activity tab now exclusively shows `myanimelist` sourced entries.
- **Profile Sub-Tab Persistence**: Active sub-tab (AniList, MAL, Simkl) is preserved when navigating to a detail page and back.

### 📱 Landscape Mode & Tablet Navigation
- **Navigation Rail Button Distribution**: Navigation rail buttons now spread evenly across the full screen height in landscape/tablet mode.

### ⚡ Search Enhancements
- **Deduplication Fix**: Fixed erroneous removal of TMDB/AniList results during title deduplication.
- **Smart Query Cleaning & Fallback**: Automatically cleans malformed queries and retries as a fallback if no results are found.

### ⚙️ Update Dialog Design & Scrolling Enhancements
- **Extended Layout**: The update bottom sheet now fills up to 85% of screen height.
- **Publication Date Display**: Displays the exact GitHub release publication date and time with a clock icon in localized format.
