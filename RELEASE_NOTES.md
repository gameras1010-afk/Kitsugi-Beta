# Kitsugi v2.4.99 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Fanart.tv & Galeri Senkronizasyonu Düzeltmeleri
- **API Anahtarı Entegrasyonu Giderildi:** Fanart.tv tarafında geçersiz olan gömülü (built-in) API anahtarı yerine, kullanıcının ayarlardan girdiği kişisel anahtarın doğrudan kullanılması sağlandı. Bu sayede tüm Fanart.tv istekleri başarıyla sonuçlanmaktadır.
- **Akıllı TVDB ID Fallback Desteği:** TV şovları ve animelerde TMDB ID'si bulunmadığında veya 0 olduğunda, MAL ve AniList ID'leri üzerinden TVDB kimliğini çözmek için `animeapi.my.id` fallback zinciri uygulandı.
- **Sinematik Yükleme & Galeri Senkronizasyonu:** Detay sayfalarında arka planda resimler yüklenirken yükleme ekranının erken kapanma hatası giderildi. `KitsugiCinematicLoadingScreen` artık galeri ögeleri de yüklenene kadar açık kalır, böylece galeri butonu doğrudan dolu ve aktif olarak açılır.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Fanart.tv & Gallery Sync Fixes
- **API Key Integration Resolved:** Fixed a critical bug where an invalid built-in project API key was prioritized over the user's personal key. The user's own key is now correctly passed as the primary `api_key` parameter for all Fanart.tv requests.
- **Smart TVDB ID Fallbacks:** Implemented an fallback lookup chain using `animeapi.my.id` to resolve the required TVDB ID via MAL and AniList identifiers when TMDB ID mappings are incomplete or missing (0).
- **Cinematic Loading & Gallery Synchronization:** Resolved premature dismissal of the detail loading screen. The `KitsugiCinematicLoadingScreen` is now kept active until both detail metadata and Fanart gallery items are successfully retrieved, ensuring the "Gallery" action button displays instantly with fully loaded content.
