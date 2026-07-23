# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### 🎨 AniHyou Tasarım Dilinde Sosyal Kartlar
- **Yeni PostItem Tasarım Paritesi**: Tartışma Konuları (`TopicCard`), Aktiviteler (`ActivityCard`) ve İncelemeler (`KitsugiReviewCard`) kartları, AniHyou uygulamasının premium `PostItem` düzenine göre tamamen yeniden tasarlandı.
- **Ortalanmış İtalik Metin Gösterimi**: Kart gövdelerinde artık kafa karıştırıcı markdown sembollerinden arındırılmış, ortalanmış, italik ve yarı kalın (semi-bold) özet metinleri yer alıyor.
- **Standart Boyutlar**: Sosyal sekmesindeki tüm kartların boyutları yatay LazyRow listelerinde hizalamayı bozmayacak şekilde `280.dp` genişlik ve `144.dp` yükseklik olarak eşitlendi.
- **Alt Bilgi ve İstatistik Satırı**: Kartların alt kısmında sol tarafta istatistikler (beğeni, yorum, puan ve görüntülenme sayıları) yer alırken, sağ tarafta tıklanabilir premium yuvarlak profil resmi ve kullanıcı adı bulunuyor.
- **Sadeleştirilmiş Arayüz**: Çeviri ve kopyalama gibi ikincil fonksiyon butonları detay sayfalarına taşınarak ana sosyal kart listelerinin sade ve şık kalması sağlandı.

### 🖼️ Biyografi ve Sosyal Detaylar İçin Görsel Galeri Paritesi
- **Etkileşimli Görsel Galerisi**: Gerek kullanıcı biyografilerindeki (AniList & Simkl) resimler, gerekse de detay sayfalarında açılan yorum/aktivite akışlarındaki tüm görseller ve hareketli GIF'ler tıklandığında premium, tam ekran görsel galerisi ile açılıyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 🎨 AniHyou-Style Social Cards Refactoring
- **PostItem Layout Parity**: Redesigned all social feed components—Forum Topics (`TopicCard`), Activities (`ActivityCard`), and Reviews (`KitsugiReviewCard`)—to match the premium AniHyou `PostItem` design language.
- **Centered Italic Typography**: Features clean, centered, italicized, and semi-bold body text stripped of raw markdown symbols for a clean feed appearance.
- **Uniform Card Dimensions**: Bounded card dimensions to `width(280.dp)` and `height(144.dp)` to ensure perfect horizontal alignment inside lazy list layouts.
- **Bottom Status & Stats Row**: Positioned ratings, likes, comments, and views counters on the bottom-left, and interactive, clickable author details (avatar + username) on the bottom-right.
- **Decluttered Card Interface**: Moved secondary actions (such as translate and copy buttons) into the detailed sheet overlays to keep the main lists minimal.

### 🖼️ Comprehensive Media Gallery & Gesture Support
- **Tap-to-Gallery Integration**: Fully verified that tapping any image or animated GIF in Simkl/AniList bios or post detail sheet contents successfully launches the interactive fullscreen gallery.
