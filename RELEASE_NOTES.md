# Kitsugi v2.4.41 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🚀 Uygulama Açılış & Sıfır Gecikme (Zero-Delay Startup)
- **Sıfır Suni Gecikme**: Cloudstream ve Manga eklentilerinin yüklenmesindeki yapay gecikmeler kaldırıldı. Tüm kaynaklar uygulamanın açıldığı ilk anda `Dispatchers.IO` arka plan thread havuzunda eşzamanlı olarak yüklenir.
- **60/120 FPS Akıcı Açılış Animasyonu**: Arka plan yüklemeleri UI thread'ini etkilemediği için açılış animasyonu tamamen akıcı çalışır.

### 📊 AniHyou / MoeList Gelişmiş Profil İstatistikleri Entegrasyonu
- **Puan Dağılımı ve Skor Renklendirmesi**: Puan grafiği 1-10 arası renk skalasıyla (Kırmızı-Yeşil-Mavi gradient) görselleştirildi. `[Başlık sayısı]` ve `[Harcanan süre]` filtre çipleri eklendi.
- **Bölüm Sayısı & Cilt Dağılımı**: Anime bölüm sayıları ve manga cilt gruplamaları grafik olarak eklendi. `[Başlık sayısı]`, `[Harcanan süre]` ve `[Ortalama Puan]` filtreleri entegre edildi.
- **Detaylı Dağılım Grafikleri**: Durum Dağılımı (Türkçe durum etiketleriyle), Tür/Format Dağılımı (TV, Film, OVA, ONA vb.) ve Ülke Dağılımı (Japonya, Çin, Güney Kore) renk paletli çubuk grafiklerle zenginleştirildi.
- **Yayın Yılı & İzleme Yılı Grafikleri**: Yayın yılları ve izleme/okuma yıllarına göre başlık sayısı, süre ve puan dağılım grafikleri eklendi.
- **3x2 Özet İstatistik Izgarası**: Toplam kayıt, izlenen bölüm, izlenen gün, planlanan gün/bölüm, ortalama puan ve standart sapma verileri düzenli kart yapısına dönüştürüldü.

### 👤 Profil Ekranı Pozisyon Koruma & State İyileştirmesi
- **Kaldığın Yerden Devam Etme**: Profil ekranındaki sekmeler (Hakkında, Aktivite, İstatistikler, Favoriler, Sosyal) ve kaydırma pozisyonu (`LazyListState`) `rememberSaveable` ile koruma altına alındı.
- **Yenileme ve Detay Sayfası Dönüşü**: Detay sayfalarına girip geri dönüldüğünde veya sayfa yenilendiğinde seçili sekme ve kaydırma konumu sıfırlanmaz.

### 🔄 Güncelleme Denetleyicisi & Doğrudan APK İndirme Fix
- **Doğrudan İndirme ve Otomatik Kurulum**: Hakkında sayfasındaki "Şimdi Denetle" butonuna tıklandığında dış web sayfalarına gitmeden doğrudan uygulama içi güncelleme penceresini açan ve APK'yı indirip kuran akış düzeltildi.
- **Sürüm Kontrol Mantığı İyileştirmesi**: Kapatılan güncellemelerin manuel denetlemede yeniden görünmesini engelleyen bayrak sıfırlandı ve semantik sürüm karşılaştırması optimize edildi.

### ⚡ UI & Liste Performans Optimizasyonları
- **Tekil Shimmer Fırçası (`LocalShimmerBrush`)**: Tüm yükleme animasyonları tek bir merkezi fırçadan beslenerek GPU/CPU tüketimi azaltıldı.
- **Sabit Liste Anahtarları (Keyed Lists)**: Yayın Takvimi, Keşfet ve Ana Ekran yatay satır öğelerine benzersiz keys eklenerek liste takılmaları giderildi.
- **Coil Önbellek Güçlendirmesi**: Resim bellek önbelleği artırılarak akıcı görsel yükleme sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🚀 Zero-Delay Startup Performance
- **Instant Background Loading**: Artificial delay triggers during plugin/extension initialization were eliminated. All loading processes run immediately on background `Dispatchers.IO`.
- **High Frame-Rate Cinematic Loading**: Isolated background tasks ensure 60/120 FPS fluid loading screen animations.

### 👤 Profile Screen State & Scroll Retention
- **Seamless Navigation Backstack**: Profile tabs (About, Activity, Stats, Favorites, Social) and scroll position (`LazyListState`) are now persisted using `rememberSaveable`.
- **State Preservation**: Returning from detail pages or pulling to refresh maintains your exact active tab and scroll position.

### ⚡ System-wide UI Rendering Optimizations
- **Consolidated Shimmer Animation**: Unified `LocalShimmerBrush` eliminates redundant animation cycles across the app.
- **Keyed Lazy Components**: Airing calendar and home rows now use composite item keys to eliminate list jitter and scroll reset issues.

