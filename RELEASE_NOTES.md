# Kitsugi v2.4.24 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📁 Özel Resim İndirme Konumu & Klasör Seçimi (SAF)
- **Kullanıcı Tanımlı Depolama Klasörü**: Uygulama içinden indirilen poster, galeri ve duvar kağıdı resimlerinin kaydedileceği hedef klasörü doğrudan Android Storage Access Framework (SAF) ile seçebilme imkanı eklendi.
- **Sistem & Veri Ayarları Entegrasyonu**: "Ayarlar -> Sistem & Veri Ayarları" menüsüne **"İndirmeler"** sekmesi eklendi. Buradan özel klasör seçilebilir ve istenildiğinde varsayılan konuma (`Downloads/Kitsugi`) sıfırlanabilir.
- **Kalıcı İzin Yönetimi & SAF Desteği**: Seçilen klasör için kalıcı URI okuma/yazma izni otomatik alınır; modern Android cihazlarda depolama izin kısıtlamalarına takılmadan doğrudan seçilen dizine resim kaydı yapılır.

### ⚡ Arama ve Profil İyileştirmeleri
- **Göreceli Arama & Çoklu Platform Harmanlama**: MAL, AniList ve TMDB arama sonuçlarının esnek harmanlanması ve yazım hatalarına duyarlı arama altyapısı optimize edildi.
- **Profil Kartları Etkileşimi (Deep-Linking)**: Medya kartları ile profil sekmelerindeki favori/geçmiş kartlarının detay sayfalarına sorunsuz geçişi sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📁 Custom Image Download Directory (Storage Access Framework)
- **User-Defined Download Path**: Added full support for selecting custom image download directories using Android's Storage Access Framework (SAF).
- **System & Data Settings Integration**: Added a dedicated **"Downloads"** tab under System & Data Settings to pick or reset custom download locations.
- **Persistent Storage Authorization**: Automatically handles persistent URI permissions for chosen folders, bypassing runtime permission hurdles on modern Android versions.

### ⚡ Search & Profile Enhancements
- **Multi-Platform Search Blending**: Optimized relative fuzzy search blending across MAL, AniList, and TMDB sources.
