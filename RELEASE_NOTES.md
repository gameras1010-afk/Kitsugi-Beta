# Kitsugi v2.4.43 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🏷️ Medya Düzenleyici Platform Etiketi Düzeltmesi (AniList Platform Label Fix)
- **AniList & MAL Etiket Karışıklığı Giderildi**: AniList kaynaklı veya bağlantılı animelerde düzenleme sayfası açıldığında hatalı şekilde "MyAnimeList" görünmesi engellendi. Medya kaynağı ve kullanıcının bağlı hesabına göre dinamik platform ismi ("AniList", "MyAnimeList", "Simkl") görüntülenecek şekilde güncellendi.
- **Otomatik Veri Kaynak Onarımı**: AniList hesabı ile eklenen fakat eski veritabanı kayıtlarında "jikan/mal" olarak kalan içerikler otomatik olarak "anilist" kaynağına dönüştürüldü.

### 🛡️ +18 İçerik Bulanıklaştırma (Adult Content Blur Parite Tamamlandı)
- **Profil ve Aktivite Koruması**: Gizlilik ayarlarında "+18 İçerikleri Bulanıklaştır" açık olduğundan profil favorileri, aktivite akışı ve aktivite detay pencerelerindeki tüm +18 kapaklar otomatik bulanıklaştırılır (`blurAdultMedia`).

### 📱 Profil Ekranı Navigasyon ve Durum Koruma (Navigation State Preservation)
- **Yenilenmeyen Sayfa Konumu**: Profil sekmesindeki detay sayfalarına veya alt kısımlara girip geri dönüldüğünde sayfanın başa atması ve verilerin gereksiz yere tekrar yüklenmesi engellendi. En son kalınan kaydırma konumu korunur.

### 🎨 Detay Sayfası Tasarım ve Paylaşım Barı Standartlaştırma
- **"Listem" Tasarım Paritesi**: Tüm detay sayfalarında (MAL, Simkl, AniList, Karakter, Stüdyo) Geri ve Paylaş butonları "Listem" görünümüne uygun dairesel yarı-saydam buton stiliyle tektipleştirildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🏷️ Media Editor Platform Label Fix
- **AniList & MAL Labeling Parity**: Fixed issue where AniList-linked entries displayed "MyAnimeList" in the edit sheet (`KitsugiEditMediaSheet`). Platform names now resolve dynamically based on entry metadata and connected user accounts.
- **Database Source Self-Healing**: Legacy entries linked to AniList are auto-repaired to reflect "anilist" as their true source.

### 🛡️ Adult Content Blur Coverage
- **Profile & Activity Privacy**: Full propagation of `blurAdultMedia` setting across profile favorites, activity feeds, and detail sheets.

### 📱 Profile Screen Navigation Stability
- **Scroll State Preservation**: Prevented unnecessary data resets and scroll reset when navigating back from detailed sub-views in the Profile tab.


