# Kitsugi Arayüz ve Navigasyon Güncellemesi Sürüm Notları 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎨 Arayüz İyileştirmeleri ve Navigasyon Düzenlemeleri
- **MyList Puan Görünürlüğü:** Liste ekranlarındaki 2 sütunlu ızgara (grid) görünümünde, puanı olmayan ("unrated") içeriklerin puan rozetinin gizlenmesi engellendi. Bu rozetler artık global "puanları gizle" (hideScores) tercihine sadık kalınarak `"—"` yer tutucu göstergesiyle gösteriliyor.
- **TMDB "Yakında Yayında" Ayrımı:** TMDB platformundaki "Yakında Yayında" yönlendirmeleri, günlük/haftalık yayın takviminden tamamen kopartıldı. Bunun yerine TMDB'nin gelecek/yakında çıkacak dizi ve filmlerini listeleyen özgün sayfalama sistemine (`ExploreCategoryType.UPCOMING_MEDIA_TMDB`) yönlendirildi. Keşfet sekmesindeki "Yakında Yayında" bölümünün sağ üst köşesinde yer alan ok butonu da bu yeni liste ekranını açacak şekilde güncellendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎨 UI Improvements & Navigation Fixes
- **MyList Score Visibility:** Fixed the issue where the score badge was suppressed/hidden for unrated media entries in the 2-column grid layout. The score badge is now always visible unless the global "hide scores" preference is enabled, showing a `"—"` placeholder for unrated entries.
- **TMDB "Airing Soon" Decoupling:** Decoupled all "Coming Soon" (Yakında Yayında) navigation pathways under the TMDB platform from the general calendar schedule. Clicking these buttons now redirects to a dedicated, paginated upcoming media grid view (`ExploreCategoryType.UPCOMING_MEDIA_TMDB`). The "Airing Soon" section arrow in the TMDB explore view has also been redirected to this new upcoming grid view.
