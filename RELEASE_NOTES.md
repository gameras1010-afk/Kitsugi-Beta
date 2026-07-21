# Kitsugi v2.4.58 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎨 Detay Sayfası Kenarlık & Tasarım Standardizasyonu
- **Görsel Kenar Kaymaları Çözüldü:** Tüm detay sayfalarındaki `HorizontalPager` kaydırma alanları standartlaştırıldı. İçeriklerin ekran kenarlarından taşması (edge-bleeding) engellendi ve hızlı kaydırma sırasındaki titremeler/donmalar giderildi.
- **Kutulu Kart Tasarımı (Surface):** Detay sayfalarında yer alan "Durum Dağılımı", "Puan Dağılımı", "Tartışma Konuları", "Aktiviteler" ve "İncelemeler" gibi tüm sekmeler, uygulamanın genel premium estetiğine uygun olarak yuvarlak köşeli (`18.dp`) ve arka plan rengi olan kart yapılarına (`Surface`) yerleştirildi.
- **Renk Karşıtlığı Kontrastı:** İçi içe geçen yorum kartlarında karmaşayı önlemek amacıyla iç kartların arka plan renkleri `SurfaceSoft` olarak uyarlanarak görsellik ve okunabilirlik en üst düzeye çıkarıldı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎨 Detail Page Spacing & Layout Standardization
- **Edge Bleeding Resolved:** Standardized layout spacing constraints for the `HorizontalPager` component across all detail pages to prevent content from touching screen edges and eliminate scrolling jitters during rapid swipe navigation.
- **Encapsulated Card Containers:** Grouped the "Status Distribution", "Score Distribution", "Forum Topics", "Activities", and "Reviews" sections into stylized, rounded-corner (`18.dp`) container cards (`Surface`) to achieve a unified, premium visual signature.
- **Nested Card Contrast:** Updated background colors for nested topic, activity, and review cards to `SurfaceSoft` inside primary containers, offering clean contrast and high aesthetic refinement.
