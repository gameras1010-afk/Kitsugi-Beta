# Kitsugi v2.4.100 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎬 Akış (Stream) Sistemi İyileştirmeleri

- **IMDb ID Zorunluluğu Kaldırıldı:** Daha önce IMDb ID çözümlenemediğinde video akış sayfası tamamen bloklanıyor ve "ID bulunamadı" hata ekranı gösteriliyordu. Artık ID çözümleme başarısız olsa bile CS eklentileri başlık (isim) bazlı aramaya devam ediyor; video kaynakları yine de listeleniyor.

- **Yanlış Sezon/Bölüm Eşleştirmesi Düzeltildi:** Birden fazla sezonlu animelerde (örn. Wistoria: Wand and Sword S2) yanlış sezonun (S1E1) çekilmesine neden olan mantık hatası giderildi. Sezon numarası artık bölüm listesinden doğru şekilde aktarılıyor.

- **Sezon Parametresi Düzeltildi:** Bölüm seçim diyaloğunda sezon numarası daha önce her zaman `1` olarak gönderiliyordu. Artık kullanıcının o an baktığı gerçek sezon numarası (örn. S2, S3) Cloudstream plugin aramasına doğru şekilde iletiliyor.

### 🔍 CS Eklenti Bölüm Eşleştirmesi Güçlendirildi

- **Reflection Hiyerarşisi Genişletildi:** `CsEpisodeMatcher` artık sadece doğrudan sınıf alanlarını değil, tüm üst sınıf (superclass) hiyerarşisini tarayarak episode/season alanlarını buluyor. Bazı Cloudstream eklentilerinde alanlar alt sınıflarda tanımlandığından bu durum daha önce eşleştirme başarısızlığına yol açıyordu.

- **Akıllı İndeks-Bazlı Fallback:** Episode meta verisi (season/episode numaraları) hiç doldurulmamış eklentilerde (yaygın Türkçe eklentilerde görülen durum), bölüm numarası artık preferred bucket (Subbed → Dubbed → None) içinde liste indeksi olarak yorumlanıyor. Bu sayede S2E1 isteği, yanlışlıkla S1'in ilk bölümüne gitmiyor.

- **Preferred Bucket Önceliği:** Sezon-bağımsız fallback aramalarda artık önce Subbed bucket aranıyor, bulunamazsa diğer bucket'lara geçiliyor. Bu daha doğru dil/senkron önceliği sağlıyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎬 Stream System Improvements

- **Removed IMDb ID Requirement Block:** Previously, if IMDb ID resolution failed, the stream picker screen would display a full-page error and prevent users from seeing any sources. Now, CS plugins continue with title-based searching even when ID resolution fails — streams are still found and displayed.

- **Fixed Incorrect Season/Episode Mapping:** A logic error that caused multi-season anime (e.g. Wistoria: Wand and Sword S2) to fetch the wrong season (S1E1) has been resolved. The correct season number is now properly propagated through the streaming pipeline.

- **Fixed Hardcoded Season Parameter:** The episode options dialog was always passing `season = 1` to the stream activity. It now correctly passes the actual season number the user is currently viewing (e.g. S2, S3).

### 🔍 CS Plugin Episode Matching Improvements

- **Reflection Now Traverses Full Class Hierarchy:** `CsEpisodeMatcher` now walks the entire superclass chain when looking for `episode` and `season` fields via reflection. Previously, fields defined in parent classes of episode types were silently missed, causing match failures on some plugins.

- **Smart Index-Based Fallback:** For plugins that store episodes as flat lists with no season/episode metadata (common in Turkish plugins), the episode number is now interpreted as a 1-based index within the preferred bucket (Subbed → Dubbed → None). This prevents S2 requests from incorrectly resolving to the first episode of S1.

- **Preferred Bucket Priority in Fallbacks:** Season-agnostic fallback searches now prioritize the Subbed bucket first before checking all episodes, ensuring better language/sync accuracy.
