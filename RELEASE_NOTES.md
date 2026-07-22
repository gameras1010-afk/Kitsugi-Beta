# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔍 Keşfet Ekranı Kategorileri & Görünüm Senkronizasyonu
- **Açılır-Kapanır Kategoriler**: Keşfet ekranında ayrı olan Anime ve Manga kategori satırları tek bir açılır-kapanır **"Kategoriler"** kartı altında birleştirildi. Manga kategorileri, Anime kategorilerinin altına konumlandırılarak görsel düzen sadeleştirildi.
- **Scroll Senkronizasyonu**: Grid ve Liste görünümleri arasında geçiş yapıldığında kullanıcının kaydırma pozisyonu (scroll) kaybolmadan birebir senkronize edilerek geçiş yapılması sağlandı.

### 👤 Kullanıcı Profili Navigasyonu & Sayfa Düzeni Güncellemesi
- **Gelişmiş Pager Altyapısı**: Kullanıcı profili ekranı, premium detay sayfası standartlarına uyumlu hale getirilerek ana sekmeler için kaydırılabilir `HorizontalPager` ve dinamik sayfa yüksekliği ölçümü (`onGloballyPositioned` ile height enterpolasyonu) ile baştan aşağı yenilendi.
- **Snap Destekli LazyRow Filtreleri**: İstatistik alt sekmeleri ve sosyal/favori filtre çipleri, kaydırma bittiğinde en yakın öğeye yumuşakça yaslanan (`rememberSnapFlingBehavior` ve `SnapPosition.Start`) modern `LazyRow` bileşenlerine dönüştürüldü.
- **Performans & Akıcı Arayüz**: Pager sayfalarında dikey listelerin (`LazyColumn` içinde `LazyColumn` / pager çakışmaları) yol açtığı takılmalar ve kaydırma kilitlenmeleri, döngü mantığı (`forEach`) ile optimize edilerek tamamen giderildi.

### 📝 Detay Sayfası Açıklama (Synopsis) Alanı Kısaltma & Buton Desteği
- **Otomatik Kısaltma Mantığı:** Detay sayfalarındaki açıklama metinleri (hem yerel hem de API sonuçları için) satır atlama sayısı (`>= 4`) veya karakter uzunluğuna (`> 200`) bağlı olarak otomatik olarak kısaltılacak şekilde güncellendi.
- **Akıcı Geçiş Animasyonu:** "Daha fazla" ve "Daha az" butonlarına basıldığında metin alanı boyutu `animateContentSize()` ile yumuşak geçişli bir şekilde değişecek şekilde optimize edildi.

### 🛡️ +18 Blur Yüklenme Performansı
- **Öneriler & İlişkiler Sekmesi İyileştirmesi:** Önerilen ve ilişkili yapımlar sekmelerinde +18 içerikli kartlar yüklenirken oluşan anlık görsel sızıntıları engellemek amacıyla Coil resim yükleyicisinin geçiş (crossfade) animasyonu blurlanmış resimler için devre dışı bırakıldı. Böylece blur efekti resim yüklenir yüklenmez anında görünür hale getirildi.

### 🎨 Modern Simge Tabanlı Bilgi Satırları
- **Metadata Standardizasyonu:** Detay sayfalarındaki eski tip chip tabanlı yerleşimler kaldırılarak, modern ve simge tabanlı dinamik bilgi satırlarına (Format, Bölüm/Bölümler, Yayın Tarihi vb.) geçiş yapıldı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔍 Explore Screen Categories & View Synchronization
- **Collapsible Categories**: Merged separate Anime and Manga category rows on the Explore Screen into a single collapsible **"Kategoriler"** container. Manga categories are nested directly below Anime categories to reduce screen clutter.
- **Scroll Position Synchronization**: Implemented precise scroll state synchronization when toggling between list and grid views in the Full Screen Media Grid. This prevents layout resetting and preserves the user's scroll index and offset.

### 👤 User Profile Navigation & Layout Modernization
- **Horizontal Pager Integration**: The user profile screen has been modernized with scrollable tab views (`HorizontalPager`) and dynamic height tracking (`onGloballyPositioned`) to ensure visual alignment with the premium detail page architecture.
- **Snap-Driven LazyRow Filters**: Sub-filters and stats categories are now built with a snap-fling behavior (`rememberSnapFlingBehavior` and `SnapPosition.Start`) within dynamic `LazyRow` components for a premium feel.
- **Nested Scroll Optimization**: Resolved nested scroll and paging stutters by migrating inner pager lists to standard Column iteration blocks.

### 📝 Detail Page Description (Synopsis) Truncation & Toggle
- **Dynamic Truncation:** Descriptions on media detail pages (both local entries and API results) are now automatically truncated based on character length (`> 200`) and newline count (`>= 4`).
- **Smooth Height Transitions:** Integrated `animateContentSize()` to provide a buttery-smooth animation when toggling between "Daha fazla" (Read more) and "Daha az" (Read less).

### 🛡️ +18 Adult Content Blur Fix
- **Recommendations & Relations Tabs:** Fixed a minor visual race condition where +18 adult content cover images briefly flashed without the blur filter during initial load. Coil's crossfade transition is now disabled for blurred media, applying the blur modifier instantly.

### 🎨 Icon-Based Metadata Layout
- **Visual Standardization:** Replaced rigid chip layouts with premium, icon-supported horizontal metadata rows showing format, episode counts, and airing schedules.
