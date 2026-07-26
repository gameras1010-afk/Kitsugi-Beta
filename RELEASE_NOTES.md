# Kitsugi v2.4.106 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🖼️ Resimler Sekmesi (Yeni Tab)

- **Ayrı "Resimler" Sekmesi:** Anime/dizi/film detay sayfalarında galeri içeriği artık "Bilgi" sekmesinde değil, ayrı bir **"Resimler"** sekmesinde gösterilir. Bu sayede Bilgi sekmesi daha temiz ve hızlı yüklenir.
- **Her iki detay tipi destekleniyor:** Hem arama sonuçlarından açılan detay sayfası (`ApiResultDetailPage`) hem de listendeki kayıtların detay sayfası (`MediaEntryDetailPage`) bu yeni sekme yapısına geçirildi.
- **Galeri tam sayfa:** Resimler sekmesi tıklandığında TMDB, Fanart.tv ve AniList poster/backdrop görselleri kategorilere göre tam ekran gözlemlenebilir.

### 💬 Tartışma Konuları Yorum Düzeni

- **AniHyou Tarzı Düz Liste:** Konu detayı açılır penceresindeki yorumlar artık kart arka planı olmayan, ince bir yatay ayırıcı çizgiyle ayrılan temiz dikey liste görünümüne geçti.
- **Yazar + Tarih Aynı Satırda:** Yorum kartlarında yazar adı ve zaman bilgisi artık aynı satırın solunda ve sağında hizalı olarak gösterilir.
- **Alt Yanıtlar Dikey Çizgiyle:** İç içe yanıtlar (`ChildComment`) sol taraflarında dikey bir `VerticalDivider` çizgisiyle girintili listelenir; tam olarak AniHyou tasarımıyla eşleşen bir hiyerarşi sunar.
- **Aksiyonlar Temizlendi:** Çeviri, kopyala, beğen ve yanıtla butonları daha kompakt ve sezgisel bir düzende yeniden konumlandırıldı.

### ⚙️ Arka Plan İyileştirmeleri

- **Yeni Kayıtlarda Sıfır Puan:** Listeye eklenen yeni yapımlar artık API üzerindeki genel puanı değil, boş (`null`) bir puan ile başlar. Kullanıcı kendi puanını ayrıca girerek belirleyebilir.
- **Platform Bağımsız Tekrar Kontrolü:** Aynı yapımın farklı kaynaklardan (AniList, MAL, Jikan) ikinci kez eklenmesi artık platform ID'leri üzerinden bütünleşik olarak engellenir.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🖼️ Dedicated Images Tab (New)

- **Separate "Resimler" (Images) Tab:** Gallery content is no longer embedded in the "Overview" tab. A dedicated **Images** tab now hosts all poster, backdrop, and fanart visuals — keeping the Overview tab clean and fast-loading.
- **Both detail types supported:** Both API search result pages (`ApiResultDetailPage`) and local list entry pages (`MediaEntryDetailPage`) have been migrated to this new tab architecture.
- **Full-page gallery:** Tapping the Images tab shows TMDB, Fanart.tv, and AniList visuals organized by category in a full-screen gallery viewer.

### 💬 Forum Thread Comment Layout

- **AniHyou-Style Flat List:** Comment cards inside the topic detail sheet now use a clean flat layout — no card backgrounds, separated by a thin `HorizontalDivider` line matching AniHyou's `ThreadCommentView` design.
- **Author + Timestamp on One Row:** Each comment now displays the author avatar, username, and relative timestamp on a single row (left/right aligned).
- **Child Replies with Vertical Indent Line:** Nested replies (`ChildComment`) are rendered with a left-side `VerticalDivider` for a clear hierarchical visual — identical to AniHyou's `ChildCommentView` pattern.
- **Cleaner Action Row:** Translate, Copy, Like, and Reply buttons have been reorganized into a more compact and intuitive layout.

### ⚙️ Background Improvements

- **Null Score on New Entries:** Newly added entries no longer inherit the API's global community score. They start with a `null` (unscored) state so users can assign their own rating.
- **Platform-Agnostic Duplicate Guard:** Duplicate detection now checks against all linked platform IDs (AniList, MAL, TMDB) — not just the current source — preventing cross-platform double entries.
