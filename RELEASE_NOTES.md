# Kitsugi v2.4.42 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎨 Standart Detay Sayfası Navigasyon & Paylaşım Barı
- **Merkezi Navigasyon Barı**: Karakter, Seslendirici/Staff, Stüdyo ve API detay sayfalarındaki eski ve dağınık navigasyon butonları tek tipleştirildi.
- **Standart Paylaşım (Share) Butonu**: Tüm detay sayfalarının sağ üst köşesine yarı saydam dairesel butonlar ile "Paylaş" seçeneği eklendi. `ShareUtils` üzerinden AniList, MAL ve Simkl platformlarına uygun bağlantı paylaşımı sağlandı.
- **Favori Entegrasyonu Korundu**: AniList oturumu açık olan kullanıcılar için favorilere ekleme/çıkarma kalp butonu standart üst bar düzeniyle uyumlu hale getirildi.

### 🐛 Profil Ekranı Çökme Çözümü (Favorites & Social Crash Fix)
- **LazyVerticalGrid Çakışması Giderildi**: Profil sekmesindeki Favoriler ve Sosyal sekmelerine tıklandığında iç içe dikey kaydırma (`IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints`) hatasından kaynaklanan çökme tamamen çözüldü.
- **Güvenli Grid Düzeni**: 3'lü kapak ve kullanıcı kartı düzeni arka planda dinamik `items(chunked(3))` mimarisine geçirilerek görsel kalite bozulmadan %100 stabil çalışma sağlandı.

### 🚀 Uygulama Açılış & Sıfır Gecikme (Zero-Delay Startup)
- **Sıfır Suni Gecikme**: Cloudstream ve Manga eklentilerinin yüklenmesindeki yapay gecikmeler kaldırıldı. Tüm kaynaklar uygulamanın açıldığı ilk anda `Dispatchers.IO` arka plan thread havuzunda eşzamanlı olarak yüklenir.
- **60/120 FPS Akıcı Açılış Animasyonu**: Arka plan yüklemeleri UI thread'ini etkilemediği için açılış animasyonu tamamen akıcı çalışır.

### 📊 AniHyou / MoeList Gelişmiş Profil İstatistikleri Entegrasyonu
- **Puan Dağılımı ve Skor Renklendirmesi**: Puan grafiği 1-10 arası renk skalasıyla görselleştirildi.
- **3x2 Özet İstatistik Izgarası**: Toplam kayıt, izlenen bölüm, izlenen gün, planlanan gün/bölüm, ortalama puan ve standart sapma verileri kart yapısına dönüştürüldü.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎨 Unified Detail Navigation & Share Action Bar
- **Standardized Hero Bar**: Unified "Back" and "Share" action bars across all detail screens (Character, Staff, Studio, ApiResult).
- **Persistent Share Action**: Top action bar includes cross-platform deep-linking powered by `ShareUtils` for AniList, MAL, and Simkl.
- **Preserved Favorite Support**: Heart toggle for AniList authenticated sessions is seamlessly integrated into the unified translucent action bar.

### 🐛 Profile Screen Favorites & Social Crash Resolution
- **Eliminated Scroll Constraint Crash**: Fixed `IllegalStateException` caused by nested scrollable grids within the Profile tab's outer `LazyColumn`.
- **Chunked Row Layout**: Preserved 3-column visual layout while using safe `chunked(3)` items composition.

