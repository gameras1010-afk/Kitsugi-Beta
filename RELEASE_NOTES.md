# Kitsugi v2.4.21 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve Tasarım İyileştirmeleri
- **Doğal Sabit Platform Barı (Sticky Header Bar)**: Keşfet (`ExploreScreen`) ve Listem (`MyListScreen`) ekranlarındaki AniList / MAL / Simkl platform geçiş barları Jetpack Compose `stickyHeader` mimarisine taşındı.
- **Geri Bildirim Sistemi Yenilendi**: Uygulama içi geri bildirim penceresi responsive `KitsugiSheetOrDialog` (alttan yukarı açılan bottom sheet) yapısına geçirildi ve tüm bildirimler `kitsugibeta@gmail.com` e-posta adresine bağlandı.
- **Dinamik Sürüm ve APK İsimlendirmesi**: Otomatik derleme sistemleri artık APK çıktılarını sürüm numarası ve flavor etiketiyle üretir (örn. `Kitsugi-Beta-v2.4.21-gms.apk`).

### 🛠️ Hata Düzeltmeleri
- **Profil Sayfası Kaydırma Kilitlenmesi Çözüldü**: Profil sekmesinde (Simkl / MAL / AniList) yukarı kaydırıp üst başlık gizlendiğinde, listenin en tepesinden tekrar aşağı kaydırmayı engelleyen `NestedScrollConnection` offset tüketim mantığı düzeltildi. Artık kaydırma her iki yönde de %100 pürüzsüz ve takılmasız çalışıyor.
- **Deep-Link ve Yapım Geçişleri Düzeltildi**: Detay sayfalarında yapım şirketi (Stüdyo), Karakter, Personel ve ilişkili medyalara tıklandığında ViewModel nesnelerinin çakışması önlendi (benzersiz ViewModel keying).

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Design Improvements
- **Native Sticky Platform Header**: Platform tab bars (AniList / MAL / Simkl) on Explore and My List screens refactored to native `stickyHeader`.
- **Modernized Feedback Bottom Sheet**: In-app feedback dialog transformed into a responsive bottom sheet (`KitsugiSheetOrDialog`), routing all user submissions to `kitsugibeta@gmail.com`.
- **Dynamic APK Naming**: Build pipelines now produce versioned and flavor-tagged APK filenames (e.g., `Kitsugi-Beta-v2.4.21-gms.apk`).

### 🛠️ Bug Fixes
- **Profile Scroll Locking Fixed**: Resolved nested scroll connection logic where pulling down at the top of the profile screen (Simkl / MAL / AniList) failed to restore top bar visibility, locking scroll state. Scrolling is now 100% fluid in both directions.
- **Deep-Link Navigation Fixed**: Resolved ViewModel key collisions when navigating into studios, characters, staff, and related media items.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
