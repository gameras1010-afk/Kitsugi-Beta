# Kitsugi Release Notes - v2.4.116-beta

Bu sürümde, video indirme altyapısındaki kritik çalışma zamanı hataları giderilmiş, üçüncü taraf entegrasyonlar genişletilmiş ve platformlar arası senkronizasyon kararlılığı artırılmıştır.

### 🛠️ FFmpeg İndirme Çökme Hatası Giderildi
- **Eksik Bağımlılık Çözümü:** Video indirme/dönüştürme işlemlerinde FFmpeg çalıştırılırken meydana gelen ve uygulamanın çökmesine yol açan `NoClassDefFoundError: Failed resolution of: Lcom/arthenica/smartexception/java/Exceptions;` hatası giderildi.
- **Transitive Linking Desteği:** `smart-exception-java` ve `smart-exception-common` kütüphaneleri runtime sınıf yoluna (classpath) ve paketleme yapılandırmasına açıkça dahil edildi.
- **R8 / ProGuard Optimizasyonu:** R8 kod daraltıcısının FFmpeg bağımlılıklarını silmesini engelleyen keep kuralları güncellendi ve doğrulandı.

### 🌐 Gelişmiş Cloudstream Eklenti Depoları (Extensions)
- **Doğrulanmış Türkçe & Küresel Depolar:** `CloudstreamExtensionsTab` içerisine tek tıkla kurulabilir 8 adet güncel ve çalışan Türkçe/Community eklenti deposu (aktif linkler) entegre edildi.
- **Eklenti Lisans ve Anti-Leech Korumaları:** Türkçe eklentilerdeki sürüm kilitlerini ve RequestBlocker hız sınırlarını otomatik olarak aşan `applyHelperPatches` mantığı iyileştirildi.

### 📺 NuvioTV Entegrasyon ve Eşitleme Çalışmaları
- **NuvioIdResolver Altyapısı:** MyAnimeList (MAL), AniList ve TMDB medya kimlikleri arasında çapraz eşleştirme sağlayan kimlik çözücü tamamlandı.
- **Birleşik Keşfet Desteği (Discover):** TV arayüzünde MAL, AniList ve TMDB verilerini tek bir ekranda birleştiren `DiscoverRepository` ve `MediaDiscoverScreen` entegrasyonu tamamlandı.
- **PathClassLoader Dinamik Yükleme:** Harici TV eklentilerinin dinamik olarak güvenle yüklenmesi için `ExternalExtensionLoader` yapısı PathClassLoader ile yeniden tasarlandı.

### 🔄 Multi-Platform Eşitleme ve Hız Sınırlandırıcı (Throttling)
- **Simkl Entegrasyonu:** AniList ve MyAnimeList entegrasyonlarının yanına Simkl de eklenerek 3 yönlü senkronizasyon sağlandı.
- **API 429 Koruması:** Yoğun senkronizasyon isteklerinde API sunucularından 429 (Too Many Requests) hatası almayı önlemek için coroutine tabanlı akıllı gecikme (throttling) mekanizması devreye alındı.
