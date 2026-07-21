# Kitsugi v2.4.46 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Kullanıcı Profili Medya ve Galeri Yöneticisi (Profile Media & Gallery Management)
- **Profil Fotoğrafı & Banner Galerisi**: Kendi profiliniz ve tüm diğer kullanıcıların profil sayfalarındaki avatar (profil resmi) ve banner (kapak resmi) görsellerine dokunulduğunda tam ekran **Kitsugi Galeri Diyalogu** (`KitsugiImageGalleryDialog`) açılması sağlandı.
- **İndir & Paylaş Entegrasyonu**: Profil görsellerini tam ekranda yüksek çözünürlükte inceleyebilir, cihaz galerisine indirebilir ve paylaşma butonu ile diğer uygulamalara gönderebilirsiniz.
- **Kişisel & Harici Profil Paritesi**: AniList, MyAnimeList ve Simkl hesap bağlantılı profil sayfaları ile harici kullanıcı profil sayfalarının tamamı visual ve fonksiyonel olarak tektipleştirildi.

### 📚 Harici Kullanıcı Listeleri & Filtreleme (`Listem` Paritesi)
- **Izgara / Liste Görünümü & Canlı Arama**: Başkalarının anime ve manga kütüphanelerini diler izgara, diler liste modunda görüntüleyebilir, dinamik `KitsugiSearchField` ile kullanıcı listesinde anlık arama yapabilirsiniz.
- **Durum Çipleri & +18 Koruması**: İzliyor, Okuyor, Tamamlandı, Planlıyor, Durduruldu ve Bırakıldı çipleriyle filtreleme yapabilir; +18 içerik bulanıklaştırmasını (`blurAdultMedia`) aktif olarak deneyimleyebilirsiniz.

### 📱 Profil Ekranı Navigasyon ve Durum Koruma (Navigation State Preservation)
- **Yenilenmeyen Sayfa Konumu**: Profil sekmesindeki detay sayfalarına veya alt kısımlara girip geri dönüldüğünde sayfanın başa atması ve verilerin gereksiz yere tekrar yüklenmesi engellendi. En son kalınan kaydırma konumu korunur.

### 🎨 Detay Sayfası Tasarım ve Kaydırılabilir Eylem Barı Standartlaştırması
- **"Listem" Tasarım Paritesi**: Tüm detay sayfalarında (MAL, Simkl, AniList, Karakter, Stüdyo, Seslendirme/Personel) Geri, Paylaş ve Favori (Kalp) butonları üst barda dairesel yarı-saydam buton stiliyle tektipleştirildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Profile Media & Image Gallery Management
- **Avatar & Banner Gallery Dialog**: Tap on any user's profile photo or banner (including your own AniList/MAL/Simkl profile) to view high-resolution images via `KitsugiImageGalleryDialog`.
- **Download & Share Actions**: Integrated `KitsugiImageDownloadHelper` for single-tap image downloads directly to local media storage and system-level content sharing.
- **Full UX Parity**: Standardized personal and external user profile headers, media list navigation, and interaction flows.

### 📚 External User Media Lists (`MyList` Parity)
- **Grid & List Layout Switching**: Browse other users' anime and manga collections in Grid or List view mode with live text search and status filters (*Watching/Reading*, *Completed*, *Planning*, *Paused*, *Dropped*).
- **Adult Blur Support**: Seamless support for `blurAdultMedia` privacy mode across external lists.

### 📱 Profile Screen Navigation Stability
- **Scroll State Preservation**: Prevented unnecessary data resets and scroll reset when navigating back from detailed sub-views in the Profile tab.

