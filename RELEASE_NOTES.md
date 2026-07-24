# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔞 Sistem Genelinde NSFW Bulanıklaştırma Standardizasyonu
- **Merkezi bulanıklaştırma bileşeni:** Tüm ekranlarda artık tek bir `KitsugiNsfwImage` bileşeni kullanılıyor. Ayarı bir kez yapıyorsun, her yerde geçerli.
- **Tam kapsam:** Hero banner, detail sayfaları, Relations, Recommendations, Explore, Profil, Aktivite sayfası, Sinematik yükleme ekranı — hepsi artık aynı sistemi kullanıyor.
- **Yükleme durumları da bulanık:** Görsel yüklenirken gösterilen arka plan/placeholder da içerik yetişkine uygun değilse bulanıklaştırılıyor.

### 🎬 Player İyileştirmeleri
- **Otomatik kontrol gizleme:** Oynatıcı overlay'i bir süre dokunulmadığında otomatik olarak gizleniyor; tam ekran deneyimi sağlanıyor.
- **Video kalite seçimi:** Oynatıcıda mevcut akış kaliteleri arasında geçiş yapılabiliyor.
- **Ses kanalı butonu iyileştirildi:** Birden fazla ses kanalı yoksa buton görünmüyor — gereksiz arayüz kirliliği giderildi.

### 🗣️ Seslendirme Sanatçısı & Ekip Verisi İyileştirmeleri
- **İsim formatı normalleştirildi:** "SoyAdı, İsim" formatı otomatik olarak "İsim SoyAdı" biçimine dönüştürülüyor.
- **Dil etiketleri tutarlı hale getirildi:** Karmaşık bölgesel meta veriler doğru şekilde eşleniyor.
- **Sıralama sabitlendi:** Seslendirme sanatçıları dil tercihine göre (Japonca > İngilizce > Türkçe > alfabetik) sıralanıyor.

### 🌐 Explore — Takvim & Platform Birleştirme
- **Yayın takvimi birleştirildi:** TMDB, AniList ve MAL "Yakında Yayında" verileri tek bir pipeline üzerinden akıyor.
- **Platform farkındalıklı etiketler:** TMDB içerikleri "Trending Shows/Movies" olarak doğru etiketleniyor.
- **Kaynak izolasyonu:** Platform değiştirirken eski veri yeni verinin üzerine yazılmıyor.

### 🔄 Karakter & Ekip Detay Sayfaları
- **Pull-to-Refresh:** Aşağı çekerek yenileme aktif. Eski yenile butonları kaldırıldı.
- **Çok kaynaklı veri birleştirme:** Jikan/MAL, Shikimori ve AniList sıralı olarak sorgulanıyor; eksik alanlar otomatik dolduruluyor.
- **Zorunlu yenileme:** Önbellek atlanarak tüm kaynaklar yeniden sorgulanabiliyor.

### 👤 Kullanıcı Listesi & Profil Sayfaları
- **Kaydırılabilir Anime/Manga sekmeleri:** Parmakla sola/sağa kaydırarak liste sekmeleri arasında geçiş.
- **Akıllı arama barı:** Yukarı kaydırınca gizlenir, aşağı kaydırınca geri çıkar.
- **Resim galerisi:** Aktivite ve medya detay sayfalarındaki galeriler düzgün açılıp gezinilebiliyor.

### 🗂️ MyList — Sekme Senkronizasyonu
- **Race condition düzeltildi:** Sekme geçişlerindeki animasyon sırasında oluşan indeks tutarsızlığı giderildi.

### 🛠️ Diğer Düzeltmeler & Teknik İyileştirmeler
- **Yayın takvimi TMDB verileri:** Poster görselleri doğru yükleniyor, geri sayım ve meta veriler tüm platformlarda tutarlı.
- **Cloudflare bypass & ağ katmanı:** HTTP istek altyapısı güçlendirildi.
- **Genel performans:** Gereksiz recomposition azaltıldı, daha akıcı animasyonlar.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔞 System-Wide NSFW Blur Standardization
- **Centralized blur component:** All screens now use a single `KitsugiNsfwImage` wrapper. Set it once, works everywhere.
- **Full coverage:** Hero banners, detail pages, Relations, Recommendations, Explore, Profile, Activity sheet, Cinematic loading screen — all unified.
- **Loading states also blurred:** Background/placeholder shown while image loads is also blurred for adult content.

### 🎬 Player Improvements
- **Auto-hide overlay:** Player controls automatically hide after a period of inactivity for a true fullscreen experience.
- **Video quality selection:** Switch between available stream qualities directly within the player.
- **Conditional audio track button:** Audio track button is hidden when only one track is available, reducing UI clutter.

### 🗣️ Voice Actor & Staff Data Improvements
- **Name format normalized:** "LastName, FirstName" format is automatically converted to "FirstName LastName".
- **Language label consistency:** Complex regional metadata is correctly mapped.
- **Sort order stabilized:** Voice actors sorted by language preference (Japanese > English > Turkish > alphabetical).

### 🌐 Explore — Calendar & Platform Unification
- **Unified airing calendar:** TMDB, AniList, and MAL "Airing Soon" data flows through a single pipeline.
- **Platform-aware labels:** TMDB content correctly labeled as "Trending Shows/Movies".
- **Source isolation hardened:** Switching platforms no longer allows stale data to overwrite fresh results.

### 🔄 Character & Staff Detail Pages
- **Pull-to-Refresh:** Swipe down to refresh. Legacy manual refresh buttons removed.
- **Multi-source data aggregation:** Jikan/MAL, Shikimori, and AniList queried sequentially; missing fields auto-filled.
- **Force refresh:** Cache can be bypassed to re-query all sources fresh.

### 👤 User List & Profile Pages
- **Swipeable Anime/Manga tabs:** Swipe left/right to switch between list tabs (HorizontalPager).
- **Smart search bar:** Hides on scroll up, reappears on scroll down.
- **Image gallery:** Galleries in activity and media detail pages open and navigate correctly.

### 🗂️ MyList — Tab Sync Fix
- **Race condition fixed:** Tab index inconsistency during pager transition animation is resolved.

### 🛠️ Other Fixes & Technical Improvements
- **Airing calendar TMDB data:** Poster images load correctly, countdown and metadata consistent across all platforms.
- **Cloudflare bypass & network layer:** HTTP request infrastructure hardened.
- **General performance:** Reduced unnecessary recompositions, smoother animations throughout.
