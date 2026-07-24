# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Yeniden Yapılandırılan Resim Galerisi
- **Kategorize Edilmiş Resim Bölümleri:** Medya detay sayfasındaki "Resimler" bölümü artık görselleri düz bir liste olarak değil, her biri kendi emoji başlığıyla ayrı yatay satırlarda gösteriyor:
  - 🎨 Logo (Fanart.tv hdtvlogo / hdmovielogo)
  - 🖼 Arka Plan (Fanart.tv showbackground / TMDB backdrops)
  - 📋 Poster (Fanart.tv tvposter / TMDB posters / Jikan)
  - 🎭 Karakter (Fanart.tv characterart)
  - 🌐 Küçük Resim (Fanart.tv tvthumb)
- **Platform Kaynak Etiketleri:** Her küçük resmin üzerinde hangi platformdan (Fanart.tv / TMDB / Jikan) geldiğini gösteren renkli rozet eklendi.
- **Galeri Diyaloğunda Akıllı Sekme Seçimi:** Detay sayfasında bir resme tıklandığında galeri diyaloğu otomatik olarak o resmin kategorisini üstteki sekmelerden seçili hale getiriyor. Kullanıcı sekmeler arasında kolayca geçiş yapabilir.
- **Doğru İndeks Eşleşmesi:** Tıklanan resim, galeri diyaloğunda tam olarak karşısına açılıyor; sayfa kaymaları veya indeks hataları giderildi.

### 🎨 Fanart.tv Medya Galerisi Entegrasyonu
- **Çoklu Kaynak Galerisi:** Fanart.tv, TMDB ve Jikan'dan gelen tüm görseller tek bir galeri yapısında birleştirilip kategorilere ayrılıyor.
- **Otomatik TVDB Kimlik Çözümleme:** Anime içerikleri için TVDB ID'leri ARM API üzerinden otomatik çözümleniyor.
- **Entegrasyon Ayarları:** Ayarlar > Entegrasyonlar altında Fanart.tv sekmesi; API anahtarı girişi ve etkinleştirme seçeneğiyle birlikte sunuluyor.

### 🛡️ Sistem Genelinde NSFW Flulaştırma
- **Merkezi `KitsugiNsfwImage` Bileşeni:** Tüm el ile yazılmış `Modifier.blur` kullanımları kaldırılarak merkezi bileşene geçildi.
- **Otomatik ve Tutarlı Gizleme:** Keşfet, Profil ve tüm medya ekranlarında yetişkin içerik görselleri kullanıcı ayarına göre sistem genelinde flulaştırılıyor.

### 🎬 Gelişmiş Video Oynatıcı Kontrolleri
- **Kalite Seçimi:** Tam ekran oynatıcıya dinamik akış kalitesi seçim menüsü eklendi.
- **Çoklu Ses İzi:** Birden fazla ses izi varsa ses izi butonu görünür, tek ise otomatik gizleniyor.
- **Otomatik Kontrol Gizleme:** Oynatıcı arayüzü belirli süre sonra otomatik olarak gizleniyor.

### 📱 Modernize Edilmiş Kullanıcı Medya Listesi
- **Kaydırma ile Sekme Geçişi:** `HorizontalPager` ile Anime/Manga sekmeleri arasında parmak kaydırmayla geçiş yapılabiliyor.
- **Gizlenebilir Arama Çubuğu:** Aşağı kaydırmada arama çubuğu gizleniyor, yukarı kaydırmada geri geliyor.

### 🎙️ Ses Sanatçısı Normalizasyonu
- **Ad Soyad Formatı:** Tüm ses sanatçısı isimleri "Soyad, Ad" yerine "Ad Soyad" formatında gösteriliyor.
- **Dil Öncelikli Sıralama:** Japonca > İngilizce > Türkçe > Alfabetik sıraya göre listeleniyor.

### 📅 Birleştirilmiş Yayın Takvimi
- **Ortak Veri Altyapısı:** TMDB, AniList ve MAL'ın "Yakında" içerikleri tek bir Yayın Takvimi akışında toplandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Reconstructed Image Gallery
- **Categorized Sections:** The "Resimler" (Images) section on the media detail page now displays assets in named, emoji-prefixed rows instead of a flat list:
  - 🎨 Logo (Fanart.tv hdtvlogo / hdmovielogo)
  - 🖼 Backdrop (Fanart.tv showbackground / TMDB backdrops)
  - 📋 Poster (Fanart.tv tvposter / TMDB posters / Jikan)
  - 🎭 Character Art (Fanart.tv characterart)
  - 🌐 Thumbnail (Fanart.tv tvthumb)
- **Platform Source Badges:** Each thumbnail now displays a colored badge indicating its source (Fanart.tv / TMDB / Jikan).
- **Smart Pre-selected Filter Tab:** Tapping an image in the detail page opens the gallery dialog with the matching category tab automatically pre-selected for instant context.
- **Correct Index Matching:** The gallery opens precisely on the tapped image, with all index-mismatch and scroll-reset issues resolved.

### 🎨 Fanart.tv Media Gallery Integration
- **Multi-Source Gallery:** Assets from Fanart.tv, TMDB, and Jikan are merged and categorized into a unified gallery structure.
- **Automatic TVDB ID Resolution:** Anime entries resolve TVDB IDs automatically via the ARM API for seamless Fanart.tv lookups.
- **Settings Integration:** A dedicated Fanart.tv tab under Settings > Integrations allows entering an API key and toggling the feature.

### 🛡️ System-Wide NSFW Blurring
- **Centralized `KitsugiNsfwImage` Component:** All manual `Modifier.blur` calls replaced with a single centralized wrapper.
- **Consistent Automated Blur:** NSFW/adult content is automatically blurred across Explore, Profile, and all media screens based on user settings.

### 🎬 Advanced Video Player Controls
- **Quality Selection:** Dynamic streaming quality selector added to the full-screen player overlay.
- **Smart Audio Track Button:** The audio track button is shown only when multiple tracks are available.
- **Auto-Hide Overlay:** Player controls auto-hide after inactivity for an uninterrupted viewing experience.

### 📱 Modernized User Media List
- **Swipe Navigation:** `HorizontalPager` integration for gesture-based Anime/Manga tab switching.
- **Scroll-Aware Search Bar:** Search bar hides on downward scroll and reappears on upward scroll.

### 🎙️ Normalized Voice Actors
- **First Last Format:** Voice actor names standardized from "LastName, FirstName" to "FirstName LastName".
- **Language-Priority Sort:** Sorted by Japanese > English > Turkish > Alphabetical.

### 📅 Unified Airing Calendar
- **Consolidated Pipelines:** TMDB, AniList, and MAL upcoming/airing feeds unified into a single calendar pipeline.
