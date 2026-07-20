# Kitsugi v2.4.20 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Performans ve Tasarım İyileştirmeleri
- **Doğal Sabit Platform Barı (Sticky Header Bar)**: Keşfet (`ExploreScreen`) ve Listem (`MyListScreen`) ekranlarındaki AniList / MAL / Simkl platform geçiş barları pop-up overlay yerine Jetpack Compose `stickyHeader` mimarisine taşındı. Sayfa aşağı kaydırıldığında bar üst kenara doğal bir şekilde yapışır.
- **Geri Bildirim Sistemi Yenilendi**: Uygulama içi geri bildirim penceresi responsive `KitsugiSheetOrDialog` (alttan yukarı açılan bottom sheet) yapısına geçirildi ve tüm bildirimler `kitsugibeta@gmail.com` e-posta adresine bağlandı.
- **Dinamik Sürüm ve APK İsimlendirmesi**: Otomatik derleme sistemleri artık APK çıktılarını sürüm numarası ve flavor etiketiyle üretir (örn. `Kitsugi-Beta-v2.4.20-gms.apk`).
- **Profil Performans Optimasyonları**: Profil ekranındaki istatistik hesaplamaları hafızaya alındı (memoized), kaydırma takılmaları giderildi.

### 🛠️ Hata Düzeltmeleri
- **Deep-Link ve Yapım Geçişleri Düzeltildi**: Detay sayfalarında yapım şirketi (Stüdyo), Karakter, Personel ve ilişkili medyalara tıklandığında ViewModel nesnelerinin çakışması önlendi (benzersiz ViewModel keying).
- AniList ID (100M offset) dönüşüm ve yönlendirme doğrulamaları tam kilitlendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Performance & Design Improvements
- **Native Sticky Platform Header**: Platform tab bars (AniList / MAL / Simkl) on Explore and My List screens refactored to native `stickyHeader`. Tabs now seamlessly pin to the top edge during scrolling.
- **Modernized Feedback Bottom Sheet**: In-app feedback dialog transformed into a responsive bottom sheet (`KitsugiSheetOrDialog`), routing all user submissions to `kitsugibeta@gmail.com`.
- **Dynamic APK Naming**: Build pipelines now produce versioned and flavor-tagged APK filenames (e.g., `Kitsugi-Beta-v2.4.20-gms.apk`).
- **Profile Performance Optimizations**: Memoized statistic calculations in profile dashboards to eliminate scrolling frame drops.

### 🛠️ Bug Fixes
- **Deep-Link Navigation Fixed**: Resolved ViewModel key collisions when navigating into studios, characters, staff, and related media items.
- Ensured 100,000,000 AniList ID offset is consistently maintained during cross-platform detail navigation.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
