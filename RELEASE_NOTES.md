# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Son Güncelleme

### 💬 AniList Sosyal Sekme ve Parite Düzeltmeleri
- **Sosyal İçerik Yükleme Sorunları Giderildi**: Detay sayfalarında AniList kaynaklı incelemeler (reviews), tartışma konuları (forum topics) ve aktivite akışlarının (activities) yüklenmeme sorunu giderildi.
- **Akıllı Kimlik Çözümleme**: Arayüzde veya `KitsugiMediaSocialClient` içerisinde kullanılan MAL ID'lerinin (MyAnimeList kimliklerinin), ilişkiler istemcisi (`relationsClient`) aracılığıyla otomatik olarak gerçek AniList ID'lerine dönüştürülmesi sağlandı. Bu sayede API isteklerinin geçersiz veya boş yanıt dönmesi tamamen engellendi.

### 🎙️ Seslendirmen Detay Sayfası — AniHyou Pariteği
- **Karakter Listesi Yeni Görünümü**: Seslendirmen detay sayfasındaki "Karakterler" sekmesi artık AniHyou'nun tasarımına birebir uygun, kompakt tek satır kart formatını kullanıyor. Her satırda karakter görseli, karakter adı ve altında `"Yapım Adı • Rol"` (örn. `Detective Conan • Ana Karakter`) bilgisi yer alıyor.
- **TMDB Oyuncu Rol Desteği**: TMDB kaynaklı kişi detay sayfalarında artık oyuncunun oynadığı karakterler listeleniyor. `combined_credits` API'dan gelen cast verileri popülerliğe göre sıralanarak en fazla 30 karakter/rol gösterilir.
- **Tam Genişlik Sekme Çubukları**: Portrait ve landscape modlarındaki sekme çubuklarındaki (Hakkında / Karakterler / Yapımlar) her sekme butonu artık `weight(1f)` ile ekran genişliğini eşit bölerek tam dolduruyor; sağdaki boş alan tamamen ortadan kalktı.

### ❤️ Standartlaştırılmış Favori Sistemi ve AniList Senkronizasyonu
- **Detay Sayfalarında Favori Butonu**: `MediaEntryDetailPage` ve `ApiResultDetailPage` ekranları, NuvioTV standartlarına uygun şekilde üst aksiyon çubuğunda premium bir kalp butonu ile güncellendi.
- **Güvenli Senkronizasyon (Toggle-Flip Düzeltmesi)**: `AniListSyncManager` ve `ExternalListSyncManager` modülleri "önce kontrol et, sonra değiştir" (check-then-toggle) mimarisiyle yeniden tasarlandı. AniList üzerindeki mevcut favori durumu sorgulanarak senkronizasyon sırasındaki durum tersine dönme hataları tamamen giderildi.

### 🐛 Bildirim Ekranı Kritik Hata Düzeltmesi
- **"Coroutine scope left the composition" Hatası Giderildi**: AniList Bildirimler ekranında "Tümü" sekmesi açıkken liste kaydırıldığında bildirimler yok oluyordu ve "Bildirimler yüklenemedi: The coroutine scope left the composition" hatası çıkıyordu. Sorunun kök nedeni, veri yükleme fonksiyonlarının (`loadAniList`, `loadMal`, `loadTmdbSimkl`) doğrudan composable scope içinde `suspend fun` olarak tanımlanmasıydı — scroll/recompose sırasında composable yeniden oluştuğunda bu coroutine'ler iptal ediliyordu.
- **ViewModel Mimarisine Geçiş (AniHyou Paterni)**: Yeni `KitsugiNotificationsViewModel` (`AndroidViewModel`) oluşturuldu. Tüm veri yükleme `viewModelScope` içine taşındı — bu scope scroll veya recompose'dan etkilenmez, Activity ömrüne bağlı olarak yaşar.
- **StateFlow Tabanlı UI**: Ekran artık `collectAsState()` ile reaktif olarak ViewModel state'ini okur; veri kaybolmaz, sonsuz scroll güvenli çalışır, filtre değişimi atomik olarak sıfırlanır.

### ⏱️ Yayın Geri Sayımı Metin Düzeltmesi
- **"X gün sonra yayınlanacak" Formatı**: Keşfet ekranındaki geri sayım metinleri (gün, saat, hafta, ay) artık `"X sonra yayınlanacak"` formatıyla gösteriliyor.

### 🧹 Ayarlar Temizliği — Navigasyon İyileştirmeleri
- **Yatay Modda Bildirim Butonu Kaldırıldı**: NavigationRail'de zaten alt çubukta bulunan bildirim butonu ile çakışmaması için yan navigasyon barındaki tekrarlı bildirim girişi kaldırıldı.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 💬 AniList Social Tab Parity Fixes
- **Resolved Social Content Loading Issues**: Fixed an issue where reviews, discussion topics, and activity feeds from AniList sources would fail to load on detail pages.
- **Smart ID Resolution**: Integrated automated resolution of MyAnimeList (MAL) IDs to AniList IDs inside the client network orchestration layer. This ensures that queries for forum topics, activities, and reviews receive valid AniList identifiers instead of failing with invalid mappings.

### 🎙️ Staff Detail Page — AniHyou Visual Parity
- **New Character List Layout**: The "Characters" tab on the Staff/Voice Actor detail page now uses a compact single-row card format matching AniHyou's design. Each row shows the character image, bold character name, and a subtitle in `"Show Title • Role"` format (e.g. `Detective Conan • Main Character`).
- **TMDB Actor Role Support**: Staff detail pages sourced from TMDB now display the characters/roles an actor has played. Data is fetched from the `combined_credits` API, sorted by popularity and showing up to 30 entries.
- **Full-Width Tab Bar Buttons**: Both portrait and landscape tab bars (Info / Characters / Works) now use `weight(1f)` so each tab fills equal screen width — no more dead space on the right side.

### ❤️ Standardized Favoriting System & AniList Sync
- **Detail Page Favorite Button**: Integrated a premium heart button into the top action bar of `MediaEntryDetailPage` and `ApiResultDetailPage`, matching NuvioTV standards.
- **Safe Synchronization (Toggle-Flip Fix)**: Refactored `AniListSyncManager` and `ExternalListSyncManager` to implement a "check-then-toggle" synchronization pattern. Remote favorite states are verified before invoking mutations, successfully preventing toggle-flip state desyncs.

### 🐛 Notification Screen Critical Bug Fix
- **Fixed "Coroutine scope left the composition" Crash**: When scrolling through notifications in the AniList tab ("Tümü" / All filter), notifications would disappear and the error "Bildirimler yüklenemedi: The coroutine scope left the composition" appeared. The root cause was that data loading functions (`loadAniList`, `loadMal`, `loadTmdbSimkl`) were defined as `suspend fun` directly inside the composable — when a scroll-triggered recomposition occurred, these coroutines were cancelled.
- **Migrated to ViewModel Architecture (AniHyou Pattern)**: Created a new `KitsugiNotificationsViewModel` (`AndroidViewModel`). All data fetching is now inside `viewModelScope`, which survives scroll events and recomposition, bound to the Activity lifecycle instead of the composable scope.
- **StateFlow-Driven UI**: The screen now reactively collects ViewModel state via `collectAsState()`. Data is never lost on scroll, infinite scroll works reliably, and filter changes reset pagination atomically.

### ⏱️ Airing Countdown Text Fix
- **"X sonra yayınlanacak" Format**: Countdown labels in the Explore screen now follow the `"X sonra yayınlanacak"` pattern with week/month granularity for longer durations.

### 🧹 Navigation Cleanup
- **Removed Duplicate Notifications Button**: The notifications entry in the NavigationRail (landscape mode) was removed since it duplicated the bell icon already present in the bottom bar.

---



## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔔 Bildirim Merkezi
- **Yeni Bildirim Hub'ı**: AniList, MAL ve Simkl bildirimlerinizi tek bir yerden görüntüleyebileceğiniz yeni bir Bildirim Merkezi eklendi. Keşfet ekranının sağ üst köşesindeki zil simgesine tıklayarak erişebilirsiniz.
- **Kimlik Doğrulamalı Görünürlük**: Zil simgesi yalnızca en az bir platform hesabı bağlı olduğunda görünür; oturum açmamış kullanıcılarda keşif rastgele butonu görünmeye devam eder.
- **Platform Filtresi**: Bildirimler AniList, MAL ve Simkl sekmelerine ayrılmış olup hangi platform bildirimlerini görmek istediğinizi kolayca seçebilirsiniz.

### 📱 Tablet / Yatay Ekran Düzeltmeleri — Keşfet
- **Platform Toggle Hizalaması**: Yatay ekranda sol kenarda görünen Platform Toggle (MAL / AniList) artık ekran boyutuna göre uzamıyor; her platform butonu `44dp` sabit yükseklikle, kompakt ve düzgün bir şekilde görünüyor.

### 📱 Tablet / Yatay Ekran Düzeltmeleri — Profil
- **Sekme Barı Responsive Düzeni**: Kendi profil sayfanızda ve diğer kullanıcıların profil sayfasındaki sekme barı (Hakkında, Aktivite, İstatistikler, Favoriler, Sosyal), artık `LazyRow` yerine tam genişliğe yayılan ağırlıklı (`weight`) bir `Row` düzeni kullanıyor. Sekmeler tüm ekran boyutlarında (telefon, tablet, yatay mod) eşit biçimde dağılır ve taşmaz.

### 🎬 Video Oynatıcı Kontrol Paneli Jest Koruması
- **Jest Çakışması Engellendi**: Tam ekran video oynatıcıda üst (`PlayerTopBar`) ve alt kontrol paneli görünür durumdayken, bu paneller üzerindeki dokunmalar (örn. sarma çubuğu sürüklemesi veya alt buton satırının kaydırılması) artık arka plandaki ses, parlaklık veya sarma jestlerini tetiklemez.
- **Dinamik Yükseklik Ölçümü**: Kontrol panellerinin yükseklikleri `onGloballyPositioned` kullanılarak anlık olarak ölçülür ve jestlerin engelleneceği koordinatlar dinamik olarak tespit edilir.

### 📊 Detay Sayfası Sadeleştirmesi
- **Gereksiz Bilgilerin Kaldırılması**: Medya detay sayfasında dikey ve yatay ekran yerleşimlerinde bulunan, gereksiz bilgi yoğunluğu yaratan "Puan", "İlerleme" ve "ID" statik kartları (`MainStatsGrid`) tamamen kaldırılarak arayüz sadeleştirildi.

### 📱 Yatay Modda Sol Navigasyon Barı Hizalaması
- **Üste Hizalama**: Yatay modda ekranın sol tarafında bulunan navigasyon barı (NavigationRail), butonları dikeyde ortalamak yerine artık üste hizalı olarak başlar.
- **Boş Alan Kazanımı**: Sol barın üst tarafında oluşan gereksiz boşluk kaldırıldı, butonlar daha erişilebilir ve şık bir yerleşime kavuşturuldu.

### 🏷️ Keşfet ve Arama Arayüzünde Kütüphane Durumu Entegrasyonu
- **Liste Durum Rozetleri ve İlerleme Barları**: Keşfet ekranı (bütün yatay şeritler), arama sonuçları, "Hepsini Gör" grid sayfaları ve sıralama alt panellerindeki tüm medya kartları artık kütüphanenizdeki durumunuzu (İzleniyor, Tamamlandı, Planlandı vb.) ve izleme ilerlemenizi (örn. 12/24 bölüm) dinamik olarak gösterir.
- **Güçlendirilmiş Çapraz Platform Eşleştirmesi**: Simkl (`simklId`), AniList (MAL ID'si olmayan yapımlar için özel offset çözümü), TMDB (`tmdbId`) ve MyAnimeList entegrasyonları için O(1) karmaşıklıkta tam çapraz eşleme getirilerek durum rozetlerinin her platformda tutarlı görünmesi sağlandı.

### 🔄 Otomatik Arka Plan Liste Senkronizasyonu
- **Sessiz Arka Plan Güncellemesi**: Uygulama her açıldığında bağlı olan platformlardaki (AniList, MyAnimeList ve Simkl) kütüphane listeleriniz arka planda otomatik olarak yenilenir. Artık listeleri güncellemek için manuel "İçe Aktar" butonuna basmanıza gerek kalmaz.
- **24 Saatlik Zaman Sınırı (Throttle)**: API sunucularını yormamak ve veri tasarrufu sağlamak adına otomatik yenileme işlemi son başarılı güncellemenin üzerinden 24 saat geçtikten sonra tetiklenir.
- **Thread Optimizasyonu**: Otomatik senkronizasyon işlemi tamamen arka plan (IO) thread'i üzerinde sessizce yürütülür, uygulama arayüzünde herhangi bir donma veya gecikmeye yol açmaz.

### 🎙️ Karakter Seslendirmenleri Alt Sayfası
- **Çoklu Dil Seslendirmen Desteği**: Karakterler sekmesindeki her karakter kartına yeni bir ses simgesi eklendi. Bu simgeye tıklandığında, karakterin farklı dillerdeki (Japonca, İngilizce, Türkçe vb.) tüm seslendirmenlerini listeleyen şık bir alt sayfa açılır.
- **Kolay Navigasyon**: Alt sayfadan herhangi bir seslendirmen seçildiğinde, doğrudan o seslendirmenin (Ekip) detay sayfasına sorunsuz geçiş yapılır.

### 🏷️ Platform Kaynak Rozetleri
- **Görsel Ayrım**: Arama, keşfet ve sıralama listelerindeki poster görsellerinin sol alt köşesine şık platform rozetleri (AL: AniList, MAL: MyAnimeList, SK: Simkl, TMDB: The Movie Database) eklendi. Böylece hangi içeriğin hangi kaynaktan geldiği anında anlaşılır.

### 📋 Seçilebilir Detay Metinleri
- **Kopyalama ve Arama Desteği**: Detay sayfalarındaki açıklama (synopsis) metinleri, bilgi alanları ve karakter/ekip hakkındaki biyografiler artık kopyalanabilir hale getirildi. Seçilebilir metin (SelectionContainer) desteği sayesinde dilediğiniz kısmı kolayca kopyalayabilir veya çevirebilirsiniz.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔔 Notification Hub
- **Unified Notification Center**: A new Notification Hub has been added, consolidating AniList, MAL, and Simkl notifications into a single dedicated screen. Access it by tapping the bell icon in the top-right corner of the Explore screen.
- **Authentication-Gated Visibility**: The bell icon is only shown when at least one platform account is connected. Unauthenticated users continue to see the random discovery button instead.
- **Per-Platform Filtering**: Notifications are separated into AniList, MAL, and Simkl tabs, allowing you to quickly focus on the platform you care about.

### 📱 Tablet / Landscape Layout Fixes — Explore Screen
- **Platform Toggle Alignment**: The platform toggle (MAL / AniList) displayed in the left sidebar on landscape screens no longer stretches vertically to fill the screen. Each platform button now uses a fixed `44dp` height, appearing compact and correctly aligned at all screen sizes.

### 📱 Tablet / Landscape Layout Fixes — Profile Screens
- **Responsive Sub-Tabs Bar**: The sub-tab bar on both your own profile and other users' profiles (Info, Activity, Stats, Favorites, Social) has been migrated from a `LazyRow` to a weight-based `Row` layout. Tabs now distribute evenly across the full screen width on all device form factors (phone, tablet, landscape) without overflowing or being clipped.

### 🎬 Video Player Overlay Gesture Isolation
- **Prevent Gesture Conflicts**: When the top (`PlayerTopBar`) or bottom overlay control panels are visible in the fullscreen video player, touch interactions inside their boundaries (e.g., scrubbing the progress bar or scrolling action buttons) will no longer trigger background volume, brightness, or seek gestures.
- **Dynamic Boundary Measurement**: The dimensions of the control overlays are dynamically measured using `onGloballyPositioned` to accurately isolate control gestures from background interactions.

### 📊 Media Detail Page Streamlining
- **Cleaned Up Statistics Grid**: Removed redundant static stats cards ("Score", "Progress", "ID" - `MainStatsGrid`) from both portrait and landscape detail layouts, reducing visual clutter and streamlining content delivery.

### 📱 Landscape Left Navigation Rail Alignment
- **Top-Aligned Items**: The navigation rail on the left side of the screen in landscape mode is now top-aligned rather than vertically centered.
- **Reclaimed Screen Space**: Reclaims unused top space in the navigation rail, improving usability, accessibility, and visual structure.

### 🏷️ Explore & Search Library Status Integration
- **Status Badges & Progress Bars**: Media cards in the Explore screen (all horizontal shelves), search results, full-screen "See All" grids, and ranking bottom sheets now dynamically reflect your library watch status (Watching, Completed, Planned, etc.) and granular episode progress (e.g., 12/24 episodes).
- **Advanced Cross-Platform Mapping**: Refactored mapping algorithm using O(1) indexed caching to perfectly cross-reference Simkl (`simklId`), AniList (with offset mapping for non-MAL linked titles), TMDB (`tmdbId`), and MyAnimeList sources, ensuring status indicators remain fully synchronized.

### 🔄 Automated Background List Synchronization
- **Silent Background Sync**: Your library lists from connected platforms (AniList, MyAnimeList, and Simkl) are now automatically refreshed in the background upon application startup. Manual import is no longer required to keep your lists up to date.
- **24-Hour Synchronization Throttle**: To maintain API efficiency and optimize data usage, the automated refresh is throttled to run at most once every 24 hours since the last successful sync.
- **IO Thread Offloading**: The entire sync routine executes silently on the background IO thread, preserving fluid UI performance and preventing any main thread blocking.

### 🎙️ Character Voice Actors Bottom Sheet
- **Multi-Language Voice Cast**: Added a voice icon next to each character in the Characters tab. Tapping this icon opens a premium bottom sheet displaying all voice actors for that character across different languages (Japanese, English, etc.).
- **Seamless Navigation**: Selecting any voice actor inside the bottom sheet immediately navigates you to their respective Staff detail page.

### 🏷️ Platform Source Badges
- **Visual Branding**: Integrated premium source badges (AL: AniList, MAL: MyAnimeList, SK: Simkl, TMDB: TMDB) on the bottom-left corner of media posters across search results, rankings, and explore grids.

### 📋 Selectable Detail Content
- **Text Selection & Copying**: Media summaries (synopsis), metadata panels, and character/staff biographies are now fully selectable. You can long-press to copy or translate text anywhere on the detail screens.
