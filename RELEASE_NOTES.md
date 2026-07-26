# Kitsugi v2.4.102 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🔗 AniList & MyAnimeList Senkronizasyon Düzeltmesi (KRİTİK)

- **Senkronizasyon Artık Otomatik Çalışıyor:** Daha önceki sürümlerde AniList veya MyAnimeList hesabın bağlı olsa bile düzenleme/kaydetme işlemleri sessizce eşitleme yapılmadan geçiyordu. Bunun nedeni `syncEnabledAnilist` ve `syncEnabledMal` flaglarının varsayılan değerinin `false` olmasıydı; kullanıcı bu gizli toggle'ı ayarlardan açmadığı sürece sync hiç çalışmıyordu. Artık hesabın bağlıysa sync **otomatik olarak** çalışır.

- **Anime Ekleme, Düzenleme, Silme Eşitleniyor:** Listene yeni anime ekleyince, durum/puan/bölüm güncelleyince veya silince, yapılan değişiklik anında AniList ve/veya MyAnimeList'e yansıtılıyor.

- **Çevrimdışı Kuyruğu Düzeltildi:** İnternetin olmadığı an yapılan değişiklikler kuyruğa alınıp online olunca otomatik gönderiliyordu; bu akış settings parse hatası nedeniyle de bozuluyordu. Artık settings okunamazsa bile sync atlanmıyor.

### 🎬 Akış (Stream) Sistemi İyileştirmeleri

- **IMDb ID Zorunluluğu Kaldırıldı:** ID çözümlenemediğinde video akış sayfası bloklanmıyor; CS eklentileri başlık bazlı aramaya devam ediyor.

- **Yanlış Sezon/Bölüm Eşleştirmesi Düzeltildi:** Çok sezonlu animelerde (ör. S2, S3) yanlış sezonun getirilmesine neden olan mantık hatası giderildi.

- **Sezon Parametresi Düzeltildi:** Bölüm seçim diyaloğunda artık her zaman `1` yerine gerçek sezon numarası iletiliyor.

### 🔍 CS Eklenti Bölüm Eşleştirmesi

- **Reflection Hiyerarşisi Genişletildi:** `CsEpisodeMatcher` artık tüm üst sınıf zincirini tarayarak episode/season alanlarını buluyor.

- **Akıllı İndeks-Bazlı Fallback:** Episode meta verisi olmayan Türkçe eklentilerde bölüm numarası liste indeksinden türetiliyor.

- **Preferred Bucket Önceliği:** Subbed → Dubbed → None sırasıyla arama yapılıyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🔗 AniList & MyAnimeList Sync Fix (CRITICAL)

- **Sync Now Works Automatically:** In previous versions, even with AniList or MAL connected, edits and updates were silently skipped. The root cause was `syncEnabledAnilist` and `syncEnabledMal` defaulting to `false` — sync was never triggered unless a hidden toggle was manually enabled in settings. Now sync runs **automatically** whenever an account is connected.

- **Add, Edit, Delete All Sync Correctly:** Adding a new anime, updating status/score/episode progress, or deleting an entry now correctly propagates to AniList and/or MyAnimeList in real time.

- **Offline Queue Fixed:** Changes made while offline are queued and sent when connectivity is restored. This queue was also broken by the same settings parse issue and is now fixed.

### 🎬 Stream System Improvements

- **Removed IMDb ID Requirement Block:** CS plugins now fall back to title-based search when ID resolution fails; stream sources are still listed.

- **Fixed Incorrect Season/Episode Mapping:** Multi-season anime (e.g. S2, S3) no longer fall back to S1E1 due to a propagation bug.

- **Fixed Hardcoded Season Parameter:** The episode options dialog now passes the actual current season number instead of always using `1`.

### 🔍 CS Plugin Episode Matching

- **Reflection Traverses Full Class Hierarchy:** `CsEpisodeMatcher` now walks the entire superclass chain to find episode/season fields.

- **Smart Index-Based Fallback:** For flat-list plugins with no metadata (common in Turkish plugins), episode number is resolved via 1-based index within the preferred bucket.

- **Preferred Bucket Priority:** Fallback searches now check Subbed → Dubbed → None in order for better accuracy.
