# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 👤 Kullanıcı Listesi Sayfası — Büyük Güncelleme
- **Anime/Manga sekmeleri artık kaydırılabilir:** Parmakla sağa/sola kaydırarak Anime ve Manga listeleri arasında geçiş yapılabiliyor (HorizontalPager).
- **Arama barı akıllı gizleme:** Listeyi yukarı kaydırınca arama kutusu kaybolur, aşağı doğru kaydırınca geri belirir — böylece içerik alanı maksimum kullanılır.

### 🔄 Karakter & Ekip Detay Sayfaları — Yenile Modernizasyonu
- **Pull-to-Refresh eklendi:** Karakter ve ekip detay sayfalarında aşağı çekerek yenileme (PullToRefreshBox) artık aktif. Eski aksiyon barındaki manuel yenile butonları kaldırıldı.
- **Çok kaynaklı veri birleştirme:** Karakter ve ekip verileri önce Jikan/MAL'dan, eksik alanlar Shikimori ve AniList'ten sıralı olarak tamamlanıyor.
- **Zorunlu yenileme:** Önbellek atlanarak tüm kaynaklar yeniden sorgulanabiliyor.

### 🗂️ MyList Sekme Navigasyonu — Race Condition Düzeltmesi
- **Tab senkronizasyonu iyileştirildi:** Pager'ın `settledPage` değeri kullanılarak geçiş animasyonu sırasında oluşan tab indeksi yarış koşulu giderildi.

### 🌐 Explore & TMDB Entegrasyonu
- **Platform farkındalıklı etiketler:** TMDB içerikleri artık "Anime/Manga" yerine doğru şekilde "Trending Shows/Movies" olarak etiketleniyor.
- **Kaynak izolasyonu güçlendirildi:** Platform değiştirirken eski veri yeni verinin üzerine yazılmıyor.

### 👤 Profil Sayfası İyileştirmeleri
- **Medya bulanıklığı tutarlı hale getirildi:** Yetişkin içerik bulanıklığı tüm favori ve yükleme durumlarında doğru çalışıyor.
- **Bottom Navigation senkronizasyonu:** Sayfanın en üstüne dönünce navigasyon barı otomatik yeniden görünüyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 👤 User Media List — Major Update
- **Swipeable Anime/Manga tabs:** Swipe left/right to switch between Anime and Manga lists via HorizontalPager.
- **Smart search bar visibility:** Search bar hides on scroll up and reappears on scroll down, maximizing content area.

### 🔄 Character & Staff Detail Pages — Refresh Modernization
- **Pull-to-Refresh added:** Swipe down to refresh on character and staff detail pages (PullToRefreshBox). Legacy manual refresh buttons removed from action bar.
- **Multi-source data aggregation:** Character and staff data is fetched sequentially from Jikan/MAL, then Shikimori and AniList fill in missing fields.
- **Force refresh:** Cache can be bypassed to re-query all sources fresh.

### 🗂️ MyList Tab Navigation — Race Condition Fix
- **Tab sync improved:** Using `settledPage` instead of `currentPage` eliminates the tab index race condition during pager transition animations.

### 🌐 Explore & TMDB Integration
- **Platform-aware labels:** TMDB content is now correctly labeled as "Trending Shows/Movies" instead of "Anime/Manga".
- **Source isolation hardened:** Switching platforms no longer allows stale data to overwrite fresh results.

### 👤 Profile Screen Improvements
- **Consistent adult media blur:** The blur setting is correctly applied across all favorite thumbnails and loading states.
- **Bottom Navigation sync:** Navigation bar automatically reappears when scrolled back to the top of profile pages.
