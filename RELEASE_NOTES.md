# Kitsugi v2.4.0-beta Sürüm Notları 🚀

### 🎉 Yenilikler ve Eklenen Özellikler
- **Otomatik Uygulama Güncelleme Sistemi**: GitHub Releases altyapısı ile entegre, canlı indirme yüzdesi (MB/s) gösteren, modern ve glassmorphic Jetpack Compose diyalog arayüzü entegre edildi.
- **Anime & Manga Takip Senkronizasyonu**: AniList, MyAnimeList ve Simkl hesap bağlantıları ile anlık liste ve bölüm takibi iyileştirildi.
- **Gelişmiş Medya Oynatıcı Engine**: Media3 ve MPV tabanlı çoklu motor desteği, altyazı dil önceliklendirmeleri ve otomatik gain booster geliştirildi.
- **Manga & Webtoon Okuyucu Katmanı**: Cloudstream, Mihon ve Kotatsu (1300+ kaynak) eklenti katmanı desteğiyle sorunsuz oku/izle akışı sağlandı.

### 🛠️ Hata Düzeltmeleri
- **Compose Scope Çakışmaları**: Güncelleme diyaloglarında oluşan Composable scope ve derleme tipi uyuşmazlıkları giderildi.
- **Tip Güvenliği ve Durum Yönetimi**: `UpdateUiState.Failed` durum mimarisi standart kütüphane çakışmalarını önleyecek şekilde yeniden yapılandırıldı.
- **Paket Yükleyici İzinleri**: Android 8.0+ (API 26+) sürümlerinde APK indirme sonrası güvenli `FileProvider` ve `REQUEST_INSTALL_PACKAGES` akışı doğrulandı.

### ⚡ Performans ve İyileştirmeler
- **Arka Plan İndirme Yöneticisi**: Büyük boyutlu dosya indirmelerinde bellek optimizasyonu sağlandı.
- **Koyu Tema ve Akıcı Animasyonlar**: Tüm diyaloglar ve ana ekran bileşenleri Kitsugi tasarım token'larına uygun olarak güncellendi.
