# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### 🖼️ Biyografi ve Yorumlar İçin Görsel Galeri Desteği
- **Etkileşimli Görsel Galerisi**: Kullanıcı biyografilerindeki (AniList & Simkl) veya detaylı yorumlardaki (`KitsugiHtmlWebView` içinde render edilen) resimlere tıklandığında artık tam ekran, premium bir görsel galeri açılıyor.
- **Kaydırma ve Yakınlaştırma (Pinch-to-Zoom)**: Resimler arasında kaydırma yapabilir, çift tıklayarak veya iki parmağınızla yakınlaştırıp resmi sürükleyebilirsiniz.
- **İndirme ve Paylaşma**: Galeri üzerindeki butonlar ile resimleri doğrudan galerinize indirebilir veya arkadaşlarınızla paylaşabilirsiniz.

### 🌐 Simkl Profil Biyografisi Tasarım Paritesi
- **Birebir Görünüm**: Simkl profil sekmesindeki "Hakkında" kısmı, AniList sekmesiyle tamamen aynı premium kart tasarımına, çeviri (translate) ve panoya kopyalama butonlarına kavuşturuldu.
- **Zengin Metin Desteği**: Düz metin yerine HTML/Markdown tabanlı web view mimarisine geçilerek spoiler ve resim render desteği Simkl kullanıcıları için de aktif edildi.

### 🧭 Standartlaştırılmış Profil Navigasyonu
- **Tek Noktadan Profil Erişimi**: Forum konuları, incelemeler, yorumlar ve tüm sosyal akışlardaki profil resimleri ile kullanıcı adları tıklanabilir hale getirilerek `onUserProfileClick` callback'i üzerinden sorunsuz ve kaynak-duyarlı (source-aware) profil geçişi sağlandı.

### 📊 MDBList Puan Kartları Detay Sayfalarında
- **Özel Puan Kartları**: TMDB, MAL ve AniList detay sayfalarında MDBList puan durumunu (IMDb, Metacritic, Rotten Tomatoes, TMDb puanları) gösteren özel kartlar entegre edildi.
- **Çapraz ID Eşleme Düzeltildi**: Yerel veritabanı ID'leri yerine doğru kaynak ID'leri kullanılarak puanların her koşulda doğru yüklenmesi garanti altına alındı.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 🖼️ Interactive Image Gallery for Bios & Reviews
- **Rich Media Gallery**: Tapping any image inside user biographies (AniList & Simkl) or review detail cards now launches a premium full-screen image viewer.
- **Gestures & Zooming**: Supports smooth pinch-to-zoom, double-tap zoom, dragging, and swiping between multiple images in a responsive carousel.
- **Save & Share**: Integrated direct download options to save images to local storage and share them via external apps.

### 🌐 Simkl Profile Biography Parity
- **Visual Design Parity**: Refactored the Simkl profile "About" card to perfectly match the AniList tab, integrating translate and copy button overlays.
- **Rich Text Rendering**: Migrated Simkl bio rendering from static text to the custom HTML WebView wrapper, enabling spoiler reveals and interactive image galleries.

### 🧭 Standardized Profile Navigation
- **Unified Social Clicks**: Propagated the `onUserProfileClick` navigation callback across all social feeds, forum topics, review cards, and replies. Clicking any avatar or username now correctly resolves their profile.

### 📊 MDBList Integration on Detail Pages
- **Rating Cards**: MDBList rating cards (IMDb, Metacritic, Rotten Tomatoes, TMDb) are now successfully displayed on TMDB, MAL, and AniList media details.
- **Precise ID Mapping**: Solved rating fetch issues by resolving exact external ID mappings rather than local primary keys.
