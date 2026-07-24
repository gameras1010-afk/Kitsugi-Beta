# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎨 Premium Fanart.tv Medya Galerisi Entegrasyonu
- **Gelişmiş Görsel Kaynağı:** Medya detay sayfalarını zenginleştirmek için **Fanart.tv** entegrasyonu tamamlandı. Anime, dizi ve filmler için yüksek kaliteli logolar, arka planlar (backdrops), afişler ve küçük resimler doğrudan Fanart.tv API'sinden çekiliyor.
- **Çoklu Kaynak Desteği:** Görsel galerisi artık Jikan (MyAnimeList), TMDB ve Fanart.tv kaynaklarından gelen tüm görselleri bir arada yönetiyor ve her görselin hangi platformdan geldiğini gösteren özel renkli doğrulama rozetleri barındırıyor.
- **Kategori Filtreleme Sekmeleri:** Galeri diyaloğunda görseller kategorilerine göre (Logo, Backdrop, Poster vb.) sekmelerle filtrelenebiliyor.
- **Akıllı Kimlik Çözümleme:** Anime içerikleri için Fanart.tv üzerinde TVDB kimlikleri (IDs) ARM API'si kullanılarak arka planda otomatik olarak çözümleniyor.
- **Özel Ayarlar Sekmesi:** Entegrasyonlar ayarlarına dördüncü sekme olarak "Fanart.tv" eklendi. Kullanıcılar Fanart.tv API anahtarlarını girip entegrasyonu kolayca aktif veya pasif hale getirebiliyor.

### 🛡️ Sistem Genelinde NSFW Flulaştırma (NSFW Blur)
- **Merkezi NSFW Görsel Bileşeni:** Tüm el ile yazılmış `Modifier.blur` kullanımları kaldırılarak yerine yeni merkezi `KitsugiNsfwImage` bileşeni entegre edildi.
- **Otomatik ve Güvenli Filtreleme:** Keşfet, Profil ve diğer tüm ekranlarda NSFW/Yetişkin içeriklerin görselleri ve yüklenme durumları, kullanıcının "Yetişkin İçerikleri Flulaştır" ayarına göre sistem genelinde otomatik ve tutarlı bir şekilde gizleniyor.

### 🎬 Gelişmiş Video Oynatıcı Kontrolleri
- **Çözünürlük/Kalite Seçimi:** Tam ekran video oynatıcısına dinamik akış kalitesi (Quality Selection) menüsü eklendi.
- **Çoklu Ses İzi Desteği:** Birden fazla ses izi barındıran videolar için ses izi seçim butonu eklendi, tek ses izi olan videolarda ise bu buton otomatik olarak gizlenerek arayüz temizlendi.
- **Akıllı Arayüz Gizleme:** Oynatıcı kontrollerinin kullanıcı deneyimini engellememesi için otomatik gizleme mekanizması ve hareket kontrolleri iyileştirildi.

### 📱 Modernize Edilmiş Kullanıcı Medya Listesi
- **Kaydırma Hareketleri (Swipe Tabs):** Anime ve Manga sekmeleri arasında daha akıcı geçiş yapabilmek için `HorizontalPager` entegrasyonu sağlandı.
- **Gizlenebilir Arama Çubuğu:** Liste aşağı kaydırıldığında ekran alanını genişletmek amacıyla arama çubuğu otomatik olarak gizleniyor, yukarı kaydırıldığında ise tekrar görünür hale geliyor.

### 🎙️ Ses Sanatçısı Normalizasyonu ve Akıllı Sıralama
- **Ad Soyad Formatı:** Tüm ses sanatçıları ve ekip isimleri "Soyad, Ad" yerine "Ad Soyad" formatına otomatik olarak dönüştürülüyor.
- **Öncelikli Dil Sıralaması:** Ses sanatçıları artık tercih diline göre (Japonca, İngilizce, Türkçe ve ardından alfabetik) sıralanarak en uygun seslendirme kadrosuna kolayca ulaşılması sağlanıyor.

### 📅 Birleştirilmiş Keşfet & Yayın Takvimi
- **Ortak Veri Akışı:** TMDB, AniList ve MAL platformlarındaki "Yakında Yayınlanacak" içerikler ve geri sayımlar tek bir ortak Yayın Takvimi (Airing Calendar) altyapısında birleştirildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎨 Premium Fanart.tv Media Gallery Integration
- **Premium Asset Provider:** Integrated **Fanart.tv** as a premium source for high-quality logos, backdrops, posters, and thumbnails in media detail pages.
- **Multi-Source Image Gallery:** The image gallery system now consolidates assets from Jikan (MyAnimeList), TMDB, and Fanart.tv, featuring colored source-verification badges for each provider.
- **Category Filtering Tabs:** Implemented dynamic category tabs (Logo, Backdrop, Poster, etc.) within the gallery dialog for easy asset filtering.
- **Automatic ID Resolution:** Anime media entries leverage ARM API to resolve TVDB IDs behind the scenes for seamless Fanart.tv API communication.
- **Dedicated Settings Tab:** Introduced a "Fanart.tv" settings tab under Integrations, allowing users to input their Fanart.tv API Key and toggle the feature.

### 🛡️ System-Wide NSFW Blurring
- **Centralized NSFW Image Wrapper:** Migrated all manual `Modifier.blur` implementations across screens to a centralized `KitsugiNsfwImage` component.
- **Consistent Privacy Protection:** Ensures consistent automated blurring of NSFW/adult content and loading/placeholder states across the Explore and Profile screens based on user settings.

### 🎬 Advanced Video Player Controls
- **Dynamic Quality Selection:** Added a dedicated quality selector within the player overlay during playback.
- **Smart Audio Track Selector:** Dynamically renders the audio track selector only when multiple audio tracks are available, removing UI clutter.
- **Overlay Auto-Hide:** Refined player overlay auto-hide logic and gestures for an uninterrupted viewing experience.

### 📱 Modernized User Media List
- **Swipe-Based Navigation:** Integrated `HorizontalPager` for smooth gesture-based tab switching between Anime and Manga.
- **Scroll-Aware Search Bar:** The search bar automatically hides when scrolling down to maximize screen real estate and reveals on scroll up.

### 🎙️ Normalized & Sorted Voice Actors
- **First Last Format:** Standardized name presentations from "LastName, FirstName" to "FirstName LastName" using a global utility.
- **Language-Preference Sort:** Stabilized voice actor listings by sorting them based on language preferences (Japanese, English, Turkish, then alphabetical).

### 📅 Unified Explore & Airing Calendar
- **Consolidated Pipelines:** Harmonized TMDB, AniList, and MAL upcoming/airing media feeds into a unified countdown pipeline and Airing Calendar presentation.
