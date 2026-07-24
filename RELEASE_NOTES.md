# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### 👤 Profil Arayüzü İyileştirmeleri ve Kararlılık Güncellemeleri
- **isAdult Filtre Entegrasyonu**: Profil sekmesindeki favori medyaya tıklandığında, `isAdult` özelliği kütüphane kayıtlarından otomatik olarak okunarak detay sayfasına doğru şekilde iletilir. Bu sayede yetişkin içerikli medyalardaki "+18 Sansür/Blur" koruması ilk kareden itibaren doğru şekilde çalışır.
- **Akıllı Navigasyon Çubuğu Kontrolü**: Kullanıcı profili ekranında listenin en üstüne kaydırıldığında (Scroll to Top) alt navigasyon barı (`bottomBar`) otomatik olarak yeniden görünür hale getirildi.
- **Pürüzsüz Galeri Kapatma Efekti**: Resim galerisinden çıkış yaparken arka planda oluşan kısa süreli "parlama/peek" hatası düzeltildi. Arka plan karartması ve galeri içeriği çıkış animasyonuyla tam senkronize olacak şekilde kararıp kapanır.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 👤 Profile UI Stabilization & Blur Propagation Updates
- **isAdult Propagation for Favorites**: When clicking a favorite media item on the profile screen, the `isAdult` flag is correctly propagated from library entries to the detail view. This ensures the cinematic loading screen displays adult media blurs instantly from the first frame.
- **Scroll-Aware Bottom Nav Reset**: Enabled scroll-aware bottom navigation resetting on the user profile page. Scrolling back to the top of the list automatically restores the visibility of the bottom navigation bar.
- **Smooth Image Gallery Exit**: Fixed the brief background flash/peek artifact when exiting the image gallery. The background dim/scrim and gallery content now fade out in full synchronization.
