# Kitsugi v2.4.31 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🌐 Küresel Paylaşım & Derin Bağlantı (Deep Links / App Links) Entegrasyonu
- **Medya, Profil ve Karakter Paylaşımı**: Medya detay sayfalarının yapışkan başlıklarında (sticky floating header) ve hero bölümlerinde, profil banner'larında (AniList, MAL, Simkl) ile Karakter ve Ekip detay sayfalarında doğrudan web bağlantısı paylaşma (**Paylaş** butonu) eklendi.
- **Sistem Paylaşım Menüsü (Share Sheet)**: `ShareUtils` merkezi altyapısı ile oluşturulan AniList, MyAnimeList, TMDB ve Simkl bağlantıları tek tıkla Android sistem paylaşım menüsüne aktarılır.
- **Otomatik Uygulama İçi Yönlendirme (App Links)**: AniList, MyAnimeList, TMDB ve Simkl web bağlantılarına (örn. tarayıcıdan veya mesajlaşma uygulamalarından) tıklandığında Kitsugi doğrudan ilgili medya, profil veya detay sayfasını açar.

### 🔞 Yetişkin İçerik (+18) Gizlilik Bulanıklaştırması (Adult Content Blur)
- Keşfet, Listem ve Profil ekranlarındaki medya kartları, hero kesimleri ve sıralama listelerinde yetişkin içerikler kullanıcı tercihlerine göre otomatik olarak bulanıklaştırılır.

### 🌐 Harici Çeviri Servisi Ayarı (External Translation Support)
- Ayarlar menüsüne tercih edilen harici çeviri servisinin (DeepL, Google Translate vb.) seçilebileceği yapı eklendi. Biyografi ve açıklama çevirileri kullanıcı tercihine göre açılır.

### 📱 Listem (My List) Ekranı & Arayüz İyileştirmeleri
- **Dinamik Kaydırmaya Duyarlı Buton (Scroll-aware FAB)**: Kategori seçimi için kaydırma yönüne göre gizlenen/gösterilen hareketli buton entegre edildi.
- **Gelişmiş İlerleme Göstergeleri**: Medya kartlarında izleme/okuma durumlarını görsel olarak takip etmeyi kolaylaştıran ilerleme çubukları eklendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🌐 Global Sharing & Deep Link (App Links) Integration
- **Media, Profile & Character Sharing**: Added **Share** action buttons across Media Detail pages (sticky header & hero sections), Profile banners (AniList, MAL, Simkl), as well as Character and Staff detail screens.
- **System Share Sheet**: Centralized `ShareUtils` formats standard URLs for AniList, MyAnimeList, TMDB, and Simkl, triggering the native Android share sheet with one tap.
- **App Links Navigation**: Clicking external URLs for AniList, MAL, TMDB, or Simkl now automatically opens the corresponding media, profile, character, or staff screen inside Kitsugi.

### 🔞 Adult Content (+18) Privacy Blur
- Media cards, hero sections, and bottom sheets across Explore, My List, and Profile screens now apply privacy blur to adult-tagged media according to user preferences.

### 🌐 External Translation Service Selection
- Added configurable preferred translator options (DeepL, Google Translate, etc.) in Settings, allowing one-tap translation of biographies and descriptions using your preferred engine.

### 📱 My List Screen & UX Enhancements
- **Scroll-Aware Floating Action Button**: Smooth category selection FAB that automatically hides on scroll down and reappears on scroll up.
- **Visual Progress Indicators**: Added progress bars to media list entries for instant progress tracking.
