# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📝 Detay Sayfası Açıklama (Synopsis) Alanı Kısaltma & Buton Desteği
- **Otomatik Kısaltma Mantığı:** Detay sayfalarındaki açıklama metinleri (hem yerel hem de API sonuçları için) satır atlama sayısı (`>= 4`) veya karakter uzunluğuna (`> 200`) bağlı olarak otomatik olarak kısaltılacak şekilde güncellendi.
- **Akıcı Geçiş Animasyonu:** "Daha fazla" ve "Daha az" butonlarına basıldığında metin alanı boyutu `animateContentSize()` ile yumuşak geçişli bir şekilde değişecek şekilde optimize edildi.

### 🛡️ +18 Blur Yüklenme Performansı
- **Öneriler & İlişkiler Sekmesi İyileştirmesi:** Önerilen ve ilişkili yapımlar sekmelerinde +18 içerikli kartlar yüklenirken oluşan anlık görsel sızıntıları engellemek amacıyla Coil resim yükleyicisinin geçiş (crossfade) animasyonu blurlanmış resimler için devre dışı bırakıldı. Böylece blur efekti resim yüklenir yüklenmez anında görünür hale getirildi.

### 🎨 Modern Simge Tabanlı Bilgi Satırları
- **Metadata Standardizasyonu:** Detay sayfalarındaki eski tip chip tabanlı yerleşimler kaldırılarak, modern ve simge tabanlı dinamik bilgi satırlarına (Format, Bölüm/Bölümler, Yayın Tarihi vb.) geçiş yapıldı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📝 Detail Page Description (Synopsis) Truncation & Toggle
- **Dynamic Truncation:** Descriptions on media detail pages (both local entries and API results) are now automatically truncated based on character length (`> 200`) and newline count (`>= 4`).
- **Smooth Height Transitions:** Integrated `animateContentSize()` to provide a buttery-smooth animation when toggling between "Daha fazla" (Read more) and "Daha az" (Read less).

### 🛡️ +18 Adult Content Blur Fix
- **Recommendations & Relations Tabs:** Fixed a minor visual race condition where +18 adult content cover images briefly flashed without the blur filter during initial load. Coil's crossfade transition is now disabled for blurred media, applying the blur modifier instantly.

### 🎨 Icon-Based Metadata Layout
- **Visual Standardization:** Replaced rigid chip layouts with premium, icon-supported horizontal metadata rows showing format, episode counts, and airing schedules.
