# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔄 Otomatik Arka Plan Liste Senkronizasyonu
- **Sessiz Arka Plan Güncellemesi**: Uygulama her açıldığında bağlı olan platformlardaki (AniList, MyAnimeList ve Simkl) kütüphane listeleriniz arka planda otomatik olarak yenilenir. Artık listeleri güncellemek için manuel "İçe Aktar" butonuna basmanıza gerek kalmaz.
- **24 Saatlik Zaman Sınırı (Throttle)**: API sunucularını yormamak ve veri tasarrufu sağlamak adına otomatik yenileme işlemi son başarılı güncellemenin üzerinden 24 saat geçtikten sonra tetiklenir.
- **Thread Optimizasyonu**: Otomatik senkronizasyon işlemi tamamen arka plan (IO) thread'i üzerinde sessizce yürütülür, uygulama arayüzünde herhangi bir donma veya gecikmeye yol açmaz.

### 🎙️ Karakter Seslendirmenleri Alt Sayfası
- **Çoklu Dil Seslendirmen Desteği**: Karakterler sekmesindeki her karakter kartına yeni bir ses simgesi eklendi. Bu simgeye tıklandığında, karakterin farklı dillerdeki (Japonca, İngilizce, Türkçe vb.) tüm seslendirmenlerini listeleyen şık bir alt sayfa açılır.
- **Kolay Navigasyon**: Alt sayfadan herhangi bir seslendirmen seçildiğinde, doğrudan o seslendirmenin (Ekip) detay sayfasına sorunsuz geçiş yapılır.

### 🏷️ Platform Kaynak Rozetleri
- **Görsel Ayrım**: Arama, keşfet ve sıralama listelerindeki poster görsellerinin sol alt köşesine şık platform rozetleri (AL: AniList, MAL: MyAnimeList, SK: Simkl, TMDB: The Movie Database) eklendi. Böylece hangi içeriğin hangi kaynaktan geldiği anında anlaşılır.

### 🏷️ Keşfet ve Arama Arayüzünde Kütüphane Durumu Entegrasyonu
- **Liste Durum Rozetleri ve İlerleme Barları**: Keşfet ekranı, arama sonuçları ve sıralama alt sayfalarındaki tüm medya kartları artık kütüphanenizdeki durumunuzu (İzleniyor, Tamamlandı, Planlandı vb.) ve izleme ilerlemenizi (örn. 12/24 bölüm) dinamik olarak gösterir.
- **O(1) Performans Optimizasyonu**: Kütüphane durum sorgulamaları arka plan eşlemesi ile O(1) karmaşıklığa indirilerek, grid kaydırma ve hızlı gezinme esnasında arayüz akıcılığından ödün verilmez.

### 📋 Seçilebilir Detay Metinleri
- **Kopyalama ve Arama Desteği**: Detay sayfalarındaki açıklama (synopsis) metinleri, bilgi alanları ve karakter/ekip hakkındaki biyografiler artık kopyalanabilir hale getirildi. Seçilebilir metin (SelectionContainer) desteği sayesinde dilediğiniz kısmı kolayca kopyalayabilir veya çevirebilirsiniz.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔄 Automated Background List Synchronization
- **Silent Background Sync**: Your library lists from connected platforms (AniList, MyAnimeList, and Simkl) are now automatically refreshed in the background upon application startup. Manual import is no longer required to keep your lists up to date.
- **24-Hour Synchronization Throttle**: To maintain API efficiency and optimize data usage, the automated refresh is throttled to run at most once every 24 hours since the last successful sync.
- **IO Thread Offloading**: The entire sync routine executes silently on the background IO thread, preserving fluid UI performance and preventing any main thread blocking.

### 🏷️ Explore & Search Library Status Integration
- **Status Badges & Progress Bars**: Media cards in the Explore screen, search results, and ranking bottom sheets now dynamically reflect your library watch status (Watching, Completed, Planned, etc.) and granular episode progress (e.g., 12/24 episodes).
- **O(1) Performance Optimization**: Library status checks are optimized to run in O(1) time using background-mapped indexing, ensuring buttery-smooth scrolling and layout transition responsiveness during navigation.

### 🎙️ Character Voice Actors Bottom Sheet
- **Multi-Language Voice Cast**: Added a voice icon next to each character in the Characters tab. Tapping this icon opens a premium bottom sheet displaying all voice actors for that character across different languages (Japanese, English, etc.).
- **Seamless Navigation**: Selecting any voice actor inside the bottom sheet immediately navigates you to their respective Staff detail page.

### 🏷️ Platform Source Badges
- **Visual Branding**: Integrated premium source badges (AL: AniList, MAL: MyAnimeList, SK: Simkl, TMDB: TMDB) on the bottom-left corner of media posters across search results, rankings, and explore grids.

### 📋 Selectable Detail Content
- **Text Selection & Copying**: Media summaries (synopsis), metadata panels, and character/staff biographies are now fully selectable. You can long-press to copy or translate text anywhere on the detail screens.
