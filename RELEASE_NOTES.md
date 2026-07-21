# Kitsugi v2.4.48 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📋 Medya Listesi Filtreleme & Bottom Sheet Dönüşümü
- **FAB Odaklı Etkileşim:** `KitsugiUserMediaListScreen` üzerindeki kalabalık oluşturan statik üst filtre çiplerinin yerini sağ alttaki FAB butonuyla açılan `UserMediaListStatusBottomSheet` aldı.
- **Tasarım Paritesi:** Kullanıcı medya listelerinde "Listem" ekranıyla %100 görsel ve işlevsel filtreleme uyumu sağlandı.

### ⚡ Otomatik Senkronizasyon & Bildirim İyileştirmeleri
- **Gereksiz Ağ İstekleri Giderildi:** İçerik detay sayfaları açıldığında özet (synopsis) verisinin çekilmesiyle tetiklenen arka plan güncellemeleri optimize edildi.
- **Eşitleme Bildirimi Düzeltildi:** Detay sayfalarına girildiğinde durduk yere ekranda beliren "başarıyla eşitlendi / güncellendi" snackbar bildirimi ve harici platform senkronizasyonu engellendi.

### 🛡️ +18 İçerik Bulanıklaştırma (Adult Blur) Güvencesi
- **Profil & Detay Kapsamı:** Ayarlardan bulanıklaştırma seçeneği aktifken profil favorileri, istatistikleri, sosyal akış, medya listeleri ve detay hero/poster görsellerinde +18 ögelerin tam blurlanması sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📋 Media List Filtering & Bottom Sheet UX
- **FAB-Driven Interaction:** Replaced static top status filter rows in `KitsugiUserMediaListScreen` with an interactive bottom sheet modal (`UserMediaListStatusBottomSheet`) triggered by the floating action button.
- **Design Parity:** Achieved full visual and functional parity between personal and external user media lists.

### ⚡ Automatic Sync & Notification Refinement
- **Eliminated Unnecessary Re-fetches:** Optimized background data updates triggered when loading media synopses on detail pages.
- **Redundant Sync Toast Fix:** Prevented unexpected "successfully synced / updated" snackbars when simply opening media detail views.

### 🛡️ Adult Content Blur Enforcement
- **Comprehensive Coverage:** Verified complete visual blurring across profile favorites, stats, social activity streams, media list cards, and detail page hero/poster graphics when adult blur is enabled.
