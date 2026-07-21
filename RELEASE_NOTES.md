# Kitsugi v2.4.55 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Kritik Performans & Derleme Optimizasyonu (Kasma/Donma Çözümü)
- **Release Moduna Geçiş:** GitHub üzerinden yayınlanan APK derlemeleri artık `Debug` mod yerine tamamen **Release (Minified & R8/Proguard ile Optimize edilmiş)** modda derlenmektedir. Jetpack Compose üzerindeki tüm gereksiz kontrol yükleri kaldırılmış ve zayıf/farklı cihazlarda yaşanan **kasma, donma ve takılma sorunları tamamen çözülmüştür**.
- **Kod Karıştırma & Koruma:** Uygulama kaynak kodları R8 derleyicisi ile karıştırılarak (obfuscated) tersine mühendisliğe karşı güvenli hale getirilmiştir.

### 📺 TV Modu Ayrıştırma Hazırlığı
- **TV Yedeklemesi:** TV arayüzü ve TV entegrasyonuna ait tüm kod tabanı harici yedekleme dizinine güvenle taşınmış, ana mobil kod tabanı hafifletilmiştir.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Critical Performance & Compilation Optimization (Lag/Stutter Fix)
- **Release Mode Compiler:** GitHub Release builds have been migrated from `Debug` to fully optimized **Release (Minified & R8/Proguard Optimized)** compiler settings. This eliminates Jetpack Compose development overhead, resolve **lag/stutter issues on low-end/different devices**, and significantly improves rendering frame rates.
- **Code Obfuscation & Security:** Applied R8 compiler rules to obfuscate class structures and protect code against reverse engineering.

### 📺 TV Code Separation Preparation
- **TV Integration Backup:** TV components, companion network code, and navigation elements have been backed up to the backup directory to streamline and lighten the primary mobile codebase.
