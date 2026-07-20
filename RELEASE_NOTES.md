# Kitsugi v2.4.13 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve İyileştirmeler
- **Keşfet Sayfası Kasma Sorunu Çözüldü**: Keşfet sayfası, TV ve mobil cihazlar için ayrı ayrı yazılmış iki farklı kaydırma motoru kullanıyordu. Eski `Column(verticalScroll)` motoru çerçeve düşüşlerine (frame drop) neden oluyordu. Tüm mimari tek ve yüksek performanslı `LazyColumn` ile birleştirildi.
- **Gereksiz Yeniden Çizmeler Engellendi**: Filtre hesaplamaları (`filteredTopAnime`, `filteredAiringAnime` vb.) her çizimde yeniden yapılmak yerine `remember {}` ile önbelleğe alındı. Bu sayede kaydırma sırasında gereksiz recomposition'lar ortadan kalktı.
- **Liste Algılama Optimize Edildi**: Listelere eklenen öğelerin zaten listede olup olmadığını kontrol eden `isAlreadyInList` fonksiyonu `remember {}` ile stabilize edildi.
- **LazyRow Performansı Arttırıldı**: Yatay medya listelerinde mobil cihazlar için de `LazyRow` kullanılmaya başlandı. Sanal listeleme sayesinde yalnızca ekranda görünen kartlar işleniyor.
- **Sticky Header Optimize Edildi**: `isHeroGone` durumu artık `derivedStateOf` ile `LazyListState`'e bağlı — doğrudan `scrollState.value` gözlemlemek yerine snapshot tabanlı reaktif durum izleme kullanılıyor.

### 🛠️ Hata Düzeltmeleri
- `LazyRow` içinde öğe anahtarı (key) tanımında kullanılan geçersiz `result.id` alanı, doğru `result.malId ?: result.tmdbId ?: 0` ifadesiyle düzeltildi.
- `derivedStateOf` delegate operatörü için eksik `getValue` ve `derivedStateOf` importları eklendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Improvements
- **Explore Page Lag Fixed**: The Explore page used two separate scroll engines for TV and mobile devices. The old `Column(verticalScroll)` engine caused frame drops. The entire architecture has been unified into a single high-performance `LazyColumn`.
- **Unnecessary Recompositions Eliminated**: Filter calculations (`filteredTopAnime`, `filteredAiringAnime`, etc.) are now cached with `remember {}` instead of being recomputed on every render. This eliminates unnecessary recompositions during scroll.
- **List Detection Optimized**: The `isAlreadyInList` lambda used to check if items are already in the user's list is now stabilized with `remember {}`.
- **LazyRow Performance Improved**: Horizontal media sections now use `LazyRow` on mobile as well. Only cards visible on screen are composed at any given time.
- **Sticky Header Optimized**: The `isHeroGone` state is now bound to `LazyListState` via `derivedStateOf` — using snapshot-based reactive state observation instead of directly observing `scrollState.value`.

### 🛠️ Bug Fixes
- Fixed an invalid `result.id` key reference in `LazyRow` item key lambda. Now correctly uses `result.malId ?: result.tmdbId ?: 0`.
- Added missing `getValue` and `derivedStateOf` imports required for the `by` delegate pattern in ExploreScreen.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
