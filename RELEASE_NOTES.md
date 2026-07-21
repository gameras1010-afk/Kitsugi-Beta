# Kitsugi v2.4.44 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📊 İstatistik Ekranı Düzen Optimizasyonu (Stats Screen Layout Optimization)
- **Gereksiz Boşluklar Temizlendi**: Ekrandaki çok büyük boşluklar ve gereksiz dikey boşluklar (Spacer) kaldırılarak dikey kaydırma alanı daha verimli hale getirildi.
- **Hizalama ve Düzen**: Tüm alt bölümler `Arrangement.spacedBy(16.dp)` kullanılarak tutarlı bir dikey hizalamaya kavuşturuldu.
- **Gereksiz Alt Boşluk Kaldırıldı**: Sayfa sonundaki 80.dp yüksekliğindeki boşluk kaldırılarak sayfanın daha kompakt görünmesi sağlandı.

### 🏷️ Medya Düzenleyici Platform Etiketi Düzeltmesi (AniList Platform Label Fix)
- **AniList & MAL Etiket Karışıklığı Giderildi**: AniList kaynaklı veya bağlantılı animelerde düzenleme sayfası açıldığında hatalı şekilde "MyAnimeList" görünmesi engellendi. Medya kaynağı ve kullanıcının bağlı hesabına göre dinamik platform ismi ("AniList", "MyAnimeList", "Simkl") görüntülenecek şekilde güncellendi.
- **Otomatik Veri Kaynak Onarımı**: AniList hesabı ile eklenen fakat eski veritabanı kayıtlarında "jikan/mal" olarak kalan içerikler otomatik olarak "anilist" kaynağına dönüştürüldü.

### 🛡️ +18 İçerik Bulanıklaştırma (Adult Content Blur Parite Tamamlandı)
- **Profil ve Aktivite Koruması**: Gizlilik ayarlarında "+18 İçerikleri Bulanıklaştır" açık olduğundan profil favorileri, aktivite akışı ve aktivite detay pencerelerindeki tüm +18 kapaklar otomatik bulanıklaştırılır (`blurAdultMedia`).

### 📱 Profil Ekranı Navigasyon ve Durum Koruma (Navigation State Preservation)
- **Yenilenmeyen Sayfa Konumu**: Profil sekmesindeki detay sayfalarına veya alt kısımlara girip geri dönüldüğünde sayfanın başa atması ve verilerin gereksiz yere tekrar yüklenmesi engellendi. En son kalınan kaydırma konumu korunur.

### 🎨 Detay Sayfası Tasarım ve Kaydırılabilir Eylem Barı Standartlaştırması
- **"Listem" Tasarım Paritesi**: Tüm detay sayfalarında (MAL, Simkl, AniList, Karakter, Stüdyo, Seslendirme/Personel) Geri, Paylaş ve Favori (Kalp) butonları üst barda dairesel yarı-saydam buton stiliyle tektipleştirildi.
- **Kaydırma Anında Kesintisiz Eylem Barı**: Stüdyo, Karakter ve Personel detay sayfalarında aşağı kaydırıldığında ekrana gelen yapışkan/yüzen başlık barına Paylaş ve Favori (AniList kaynaklarında) butonları eklendi. Böylece kaydırma esnasında da butonlar kesintisiz kullanılabilir hale getirildi.

### 🔧 Profil Ekranı Kapsamlı Düzeltmeler (v2.4.44)
- **Takipçi/Takip Edilen Düzeltmesi**: AniList GraphQL şemasına göre yanlış sorgu (`User.followers`) yerine doğru sorgu (`Page.followers(userId:)`) kullanılacak şekilde güncellendi. Artık takipçiler ve takip edilenler düzgün çekiliyor.
- **Favoriler Sayfalama (Pagination)**: Favori anime, manga, karakter, personel ve stüdyo kategorileri artık tek seferde 24 ile sınırlı değil. AniHyou referansına göre her kategori ayrı ayrı paginated API çağrısı ile çekiliyor. 25'ten fazla favorisi olan kullanıcılar "Daha Fazla Yükle" butonu ile tüm favorilerini görebilir.
- **Favoriler Arka Planı**: Ana profil sorgusu artık gereksiz `favourites` bloğunu barındırmıyor; bu sayede profil yüklenmesi daha hızlı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📊 Stats Screen Layout Optimization
- **Cleaned Up Whitespace**: Removed redundant vertical Spacers and the excessive 80.dp bottom spacer to prevent empty air.
- **Standardized Spacing**: Sections now use a consistent `Arrangement.spacedBy(16.dp)` layout for visual cleanliness and compact representation.

### 🏷️ Media Editor Platform Label Fix
- **AniList & MAL Labeling Parity**: Fixed issue where AniList-linked entries displayed "MyAnimeList" in the edit sheet (`KitsugiEditMediaSheet`). Platform names now resolve dynamically based on entry metadata and connected user accounts.
- **Database Source Self-Healing**: Legacy entries linked to AniList are auto-repaired to reflect "anilist" as their true source.

### 🛡️ Adult Content Blur Coverage
- **Profile & Activity Privacy**: Full propagation of `blurAdultMedia` setting across profile favorites, activity feeds, and detail sheets.

### 📱 Profile Screen Navigation Stability
- **Scroll State Preservation**: Prevented unnecessary data resets and scroll reset when navigating back from detailed sub-views in the Profile tab.

### 🔧 Profile Section Comprehensive Fixes (v2.4.44)
- **Followers/Following Fix**: Fixed broken AniList GraphQL query — replaced wrong `User.followers` with the correct `Page.followers(userId:)` / `Page.following(userId:)` schema.
- **Favorites Pagination**: Favorites (anime, manga, characters, staff, studios) are now fetched separately per-category with `hasNextPage` tracking. Users with more than 25 favorites can now see all of them via a "Load More" button.
