# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎨 Premium Fanart.tv Medya Galerisi Entegrasyonu
- **Gelişmiş Görsel Kaynağı:** Medya detay sayfalarını zenginleştirmek için **Fanart.tv** entegrasyonu tamamlandı. Anime, dizi ve filmler için yüksek kaliteli logolar, arka planlar (backdrops), afişler ve küçük resimler doğrudan Fanart.tv API'sinden çekiliyor.
- **Çoklu Kaynak Desteği:** Görsel galerisi artık Jikan (MyAnimeList), TMDB ve Fanart.tv kaynaklarından gelen tüm görselleri bir arada yönetiyor ve her görselin hangi platformdan geldiğini gösteren özel renkli doğrulama rozetleri barındırıyor.
- **Kategori Filtreleme Sekmeleri:** Galeri diyaloğunda görseller kategorilerine göre (Logo, Backdrop, Poster vb.) sekmelerle filtrelenebiliyor.

### 🎬 Gelişmiş Galeri Önizleme Arayüzü
- **En-Boy Oranına Duyarlı Satır:** Medya detay genel bakış sayfasında (Overview), görsellerin kendi boyut oranlarını koruyan şık bir yatay önizleme satırı (`DetailGalleryCard`) eklendi.
- **Akıllı Kimlik Çözümleme:** Anime içerikleri için Fanart.tv üzerinde TVDB kimlikleri (IDs) ARM API'si kullanılarak arka planda otomatik olarak çözümleniyor.

### ⚙️ Fanart.tv Entegrasyon Ayarları
- **Özel Ayarlar Sekmesi:** Entegrasyonlar ayarlarına dördüncü sekme olarak "Fanart.tv" eklendi. Kullanıcılar Fanart.tv API anahtarlarını girip entegrasyonu kolayca aktif veya pasif hale getirebiliyor.
- **Hata ve Kararlılık Düzeltmeleri:** JVM imza çakışması çözüldü, ayarlar ekranı parametre geçişleri düzeltildi ve projenin sorunsuz derlenmesi sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎨 Premium Fanart.tv Media Gallery Integration
- **Premium Asset Provider:** Integrated **Fanart.tv** as a premium source for high-quality logos, backdrops, posters, and thumbnails in media detail pages.
- **Multi-Source Image Gallery:** The image gallery system now consolidates assets from Jikan (MyAnimeList), TMDB, and Fanart.tv, featuring colored source-verification badges for each provider.
- **Category Filtering Tabs:** Implemented dynamic category tabs (Logo, Backdrop, Poster, etc.) within the gallery dialog for easy asset filtering.

### 🎬 Sleek Gallery Preview UI
- **Aspect-Ratio-Aware Preview:** Added a custom horizontal preview row (`DetailGalleryCard`) on the media detail overview tab, matching the specific aspect ratio of each asset.
- **Automatic ID Resolution:** Anime media entries leverage ARM API to resolve TVDB IDs behind the scenes for seamless Fanart.tv API communication.

### ⚙️ Fanart.tv Integration Settings
- **Dedicated Settings Tab:** Introduced a "Fanart.tv" settings tab under Integrations, allowing users to input their Fanart.tv API Key and toggle the feature.
- **Stability & Compiler Fixes:** Resolved JVM declaration clashes and settings UI parameter mapping issues, ensuring a clean and stable build.
