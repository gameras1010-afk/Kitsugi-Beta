# Kitsugi v2.4.95 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Standart Galeri Butonu — Karakter, Ekip & Stüdyo Sayfaları
- **Yeni Galeri Butonu:** Karakter, Ekip Üyesi ve Stüdyo detay sayfalarının başlık alanına "Galeri" butonu (🖼️) eklendi.
- **Paylaş butonunun hemen yanında:** Galeri butonu Paylaş ve Favori butonlarıyla aynı hizada, tutarlı konumda görünüyor.
- **Koşullu Görünürlük:** Buton yalnızca o sayfa için geçerli bir resim mevcut olduğunda gösteriliyor; resim yoksa düzen bozulmuyor.
- **Tek Dokunuşla Galeri:** Butona tıklayınca `KitsugiImageGalleryDialog` açılıyor ve resim tam ekranda incelenebiliyor.
- **Yatay & Dikey Uyum:** Hem portre hem de yatay ekran düzeninde (tablet / TV) düzgün çalışıyor.
- **Kayan Başlık Desteği:** Aşağı kaydırmada beliren sticky başlık barına da galeri butonu eklendi.

### 🎨 Fanart.tv Medya Galerisi Entegrasyonu
- **Çoklu Kaynak Galerisi:** Fanart.tv, TMDB ve Jikan'dan gelen tüm görseller tek bir galeri yapısında birleştirilip kategorilere ayrılıyor.
- **Kategorize Edilmiş Bölümler:** Görseller türüne göre ayrı satırlarda gösteriliyor:
  - 🎨 Logo — 🖼 Arka Plan — 📋 Poster — 🎭 Karakter Sanatı — 🌐 Küçük Resim
- **Platform Kaynak Etiketleri:** Her görsel üzerinde hangi platformdan geldiğini gösteren renkli rozet.
- **Akıllı Galeri Filtresi:** Bir görsele tıklandığında galeri diyaloğu otomatik olarak o kategoriye geçiyor.
- **Fanart.tv Ayarları:** Ayarlar → Entegrasyonlar altından API anahtarı girilebilir, özellik açılıp kapatılabilir.

### 🛡️ Sistem Genelinde NSFW Flulaştırma
- **Merkezi `KitsugiNsfwImage` Bileşeni:** Tüm el ile yazılmış `Modifier.blur` kullanımları merkezi bileşene taşındı.
- **Tutarlı Otomatik Gizleme:** Keşfet, Profil ve tüm medya ekranlarında yetişkin içerikler kullanıcı tercihine göre sistem genelinde flulaştırılıyor.

### 🎬 Gelişmiş Video Oynatıcı Kontrolleri
- **Kalite Seçimi:** Tam ekran oynatıcıya dinamik akış kalitesi seçim menüsü eklendi.
- **Akıllı Ses İzi Butonu:** Birden fazla ses izi varsa buton görünür, tek ise otomatik gizleniyor.
- **Otomatik Kontrol Gizleme:** Oynatıcı arayüzü belirli süre işlemsiz kalırsa otomatik olarak gizleniyor.

### 🎙️ Ses Sanatçısı Normalizasyonu
- **Ad Soyad Formatı:** Tüm ses sanatçısı isimleri "Soyad, Ad" yerine "Ad Soyad" formatında gösteriliyor.
- **Dil Öncelikli Sıralama:** Japonca → İngilizce → Türkçe → Alfabetik sıraya göre listeleniyor.

### 📱 Modernize Edilmiş Kullanıcı Medya Listesi
- **Kaydırma ile Sekme Geçişi:** `HorizontalPager` ile Anime/Manga sekmeleri arasında parmak kaydırmayla geçiş yapılabiliyor.
- **Gizlenebilir Arama Çubuğu:** Aşağı kaydırmada arama çubuğu gizleniyor, yukarı kaydırmada geri geliyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Standardized Gallery Button — Character, Staff & Studio Pages
- **New Gallery Button:** A gallery button (🖼️) has been added to the hero action bar of Character, Staff, and Studio detail pages.
- **Alongside Share & Favorite:** The button is consistently placed next to the Share and Favorite icons in all layouts.
- **Conditional Visibility:** The button only appears when a valid image is available for that entity — no empty space when there's nothing to show.
- **One-Tap Gallery:** Tapping the button opens `KitsugiImageGalleryDialog` for full-screen image browsing.
- **Portrait & Landscape Support:** Works correctly in both portrait and landscape (tablet/TV) orientations.
- **Floating Header Support:** The gallery button is also included in the sticky collapse header that appears on scroll.

### 🎨 Fanart.tv Media Gallery Integration
- **Multi-Source Gallery:** Assets from Fanart.tv, TMDB, and Jikan are merged and categorized into a unified gallery structure.
- **Categorized Sections:** Images are displayed in labeled horizontal rows by type:
  - 🎨 Logo — 🖼 Backdrop — 📋 Poster — 🎭 Character Art — 🌐 Thumbnail
- **Platform Source Badges:** Each thumbnail displays a colored badge indicating its origin (Fanart.tv / TMDB / Jikan).
- **Smart Pre-selected Filter Tab:** Tapping an image opens the gallery dialog with the matching category tab automatically pre-selected.
- **Settings Integration:** Fanart.tv API key and toggle available under Settings → Integrations.

### 🛡️ System-Wide NSFW Blurring
- **Centralized `KitsugiNsfwImage` Component:** All manual `Modifier.blur` calls replaced with a single centralized wrapper.
- **Consistent Automated Blur:** NSFW/adult content is automatically blurred across Explore, Profile, and all media screens based on user settings.

### 🎬 Advanced Video Player Controls
- **Quality Selection:** Dynamic streaming quality selector added to the full-screen player overlay.
- **Smart Audio Track Button:** Audio track button is shown only when multiple tracks are available; hidden otherwise.
- **Auto-Hide Overlay:** Player controls auto-hide after inactivity for an uninterrupted viewing experience.

### 🎙️ Normalized Voice Actors
- **First Last Format:** Voice actor names standardized from "LastName, FirstName" to "FirstName LastName".
- **Language-Priority Sort:** Sorted by Japanese → English → Turkish → Alphabetical.

### 📱 Modernized User Media List
- **Swipe Navigation:** `HorizontalPager` integration for gesture-based Anime/Manga tab switching.
- **Scroll-Aware Search Bar:** Search bar hides on downward scroll and reappears on upward scroll.
