# Kitsugi v2.4.98 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Birleşik Medya Galerisi Standardizasyonu
- **Merkezi `GalleryItem` Mimarisi:** Karakter, Ekip (Staff), Stüdyo ve Medya detay sayfalarının tamamı artık tek bir merkezi `GalleryItem` mimarisi kullanıyor. Tüm eski veri formatları ve dize listeleri temizlendi.
- **Tek ve Güçlü `KitsugiImageGalleryDialog`:** Karakter/Ekip/Stüdyo ve Medya sayfaları için ayrı ayrı yazılmış olan tüm galeri diyaloğu kodları birleştirilerek tek bir güçlü, performanslı ve tutarlı galeri diyaloğuna dönüştürüldü.
- **Gelişmiş Kategori Filtreleme & Arama:** Görseller otomatik olarak Logo, Poster, Arka Plan, Karakter ve Küçük Resim gibi kategorilere ayrılarak üstte filtre sekmeleri ile sunuluyor.
- **Akıllı Başlangıç Kategorisi:** Detay sayfasında herhangi bir resim kategorisine veya kartına tıklandığında, galeri diyaloğu otomatik olarak ilgili kategoriyi seçerek ve doğru görseli odaklayarak açılır.
- **Yatay & Dikey Tam Uyumluluk:** Tüm ekran boyutlarında, portre (dikey) ve landscape (yatay) modlarda kusursuz galeri yerleşimi ve dokunmatik kontroller sağlandı.
- **Kararlılık ve Performans İyileştirmeleri:** VM düzeyinde eski yinelenen görsel çekme mantıkları kaldırıldı, rate-limit aşım sorunları giderildi ve galeri tetikleyicilerindeki durumsal tutarsızlıklar tamamen çözüldü.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Unified Media Gallery Standardization
- **Centralized `GalleryItem` Architecture:** Character, Staff, Studio, and Media detail pages now all leverage a single centralized `GalleryItem` model. Legacy string-based and fragmented image lists have been completely purged.
- **Consolidated `KitsugiImageGalleryDialog`:** Merged multiple bifurcated media gallery dialog implementations into a single, high-performance, and unified dialog window.
- **Advanced Category Filtering:** Images are automatically grouped into categories (Logo, Poster, Backdrop, Character, Thumbnail) with clear tabs shown at the top of the gallery dialog.
- **Smart Category Auto-Focus:** Clicking on any poster, card, or specific category button in the detail views launches the gallery dialog pre-selected to that category and focused on the selected image.
- **Perfect Landscape & Portrait Scaling:** Fixed previous orientation regressions, ensuring a fluid and fully responsive viewing experience across all device form factors and rotations.
- **Performance & State Refinement:** Cleaned up duplicate image retrieval processes at the ViewModel level, resolved API rate-limiting edge cases, and polished all touch-target and trigger behaviors.
