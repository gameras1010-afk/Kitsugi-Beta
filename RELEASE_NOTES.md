# Kitsugi v2.4.103 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔗 MyAnimeList Kaynak Atama Düzeltmesi (KRİTİK)

- **MAL Anime Artık MAL Editörüyle Açılıyor:** Önceki sürümlerde MyAnimeList'ten (Jikan) gelen animeleri düzenlemek veya listeye eklemek istediğinde, her iki hesap (AniList + MAL) bağlıysa bile uygulama yanlışlıkla **AniList düzenleme ekranını** açıyordu. Animenin MAL verisini AniList'e kaydediyordu; bu nedenle MAL hesabına hiç yansımıyordu. Artık MAL/Jikan kaynaklı animeler her zaman doğru servise yönlendiriliyor.

- **Arama Sonuçlarında Kaynak Önceliği Düzeltildi (`AppViewModel`):** `addApiSelectionToList` fonksiyonu artık `result.source` bilgisini esas alıyor. `jikan` ve `mal` kaynaklı sonuçlar `"mal"` olarak işleniyor; AniList bağlı olup olmamasından bağımsız olarak MAL tercih ediliyor. Servis bağlı değilse diğerine **graceful fallback** yapılıyor.

- **Düzenleme Ekranı Kaynak Çözümlemesi Düzeltildi (`AppDialogHost`):** Var olan bir kaydı düzenlerken açılan `resolvedSource` mantığı düzeltildi. `jikan` ve `mal` kaynakları tek bir dalda birleştirildi: MAL bağlıysa her zaman MAL editörü, yalnızca MAL bağlı değil ve AniList bağlıysa AniList editörü açılıyor.

- **"Listeme Ekle" Butonu Kaynak Sorunu Düzeltildi (`MyListScreen`):** Kütüphane ekranındaki arama diyaloğu, seçilen animenin kaynağını tab index'ine (0=AniList, 1=MAL) bakarak değil, `result.source` alanından okuyarak belirliyor. `jikan` → `mal` olarak normalize ediliyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔗 MyAnimeList Source Assignment Fix (CRITICAL)

- **MAL Anime Now Opens in the MAL Editor:** In previous versions, when editing or adding an anime from MyAnimeList (Jikan) with both AniList and MAL accounts connected, the app incorrectly opened the **AniList editor** — saving MAL data to AniList instead of MyAnimeList. Entries now always route to the correct service based on their declared source.

- **Search Result Source Priority Fixed (`AppViewModel`):** `addApiSelectionToList` now uses `result.source` as the authoritative signal. `jikan` and `mal` results map to `"mal"` regardless of which services are connected. Graceful fallback to the other connected service happens only if the preferred one is not linked.

- **Edit Dialog Source Resolution Fixed (`AppDialogHost`):** The `resolvedSource` logic when opening an existing entry for editing has been corrected. `"jikan"` and `"mal"` cases are consolidated: if MAL is connected it always opens the MAL editor; AniList is only used as a fallback when MAL is not connected.

- **"Add to List" Button Source Fix (`MyListScreen`):** The search dialog inside the Library screen now reads `result.source` directly instead of guessing from the active tab index. `"jikan"` is normalized to `"mal"` so stored entries always have a canonical source string.
