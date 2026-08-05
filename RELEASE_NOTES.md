# Kitsugi-Beta v2.4.143 — Sürüm Notları

Bu Sürümde; Türkçe altyazı alma, otomatik altyazı seçimi, sıkıştırılmış paket/batch altyazı çözme ve oynatıcı altyazı öncelik hiyerarşisi kararlı hale getirilerek kapsamlı bir şekilde güncellendi.

---

### 🌐 Türkçe Altyazı Eklentileri & Otomatik Geçiş

- **AltyaziDB Entegrasyonu:** Çalışmayan eski `TurkceAltyazi.org` eklentisi varsayılan eklentilerden kaldırılarak yerine kararlı çalışan **AltyaziDB** (`https://altyazidb.online`) eklendi. Eski veritabanı kayıtları otomatik temizlendi.
- **AniSub.co Kararlılığı:** Animeler için ana Türkçe altyazı kaynağı olan AniSub.co entegrasyonu güçlendirildi.
- **Eklenti Devre Kesici (Circuit Breaker):** Zaman aşımı yaşayan veya çöken altyazı eklentileri (`SubtitleRepositoryImpl.kt`), 3 ardışık başarısızlık durumunda 5 dakika boyunca sorgulanmaz (bypass edilir). Bu sayede oynatıcının takılması veya dondurulması tamamen önlenir.

---

### 📦 Paket (Batch/Zip) Altyazı Ayıklama & Çakışma Önleyici Eşleştirme

- **Toplu Zip/Rar Ayıklama:** Zip formatında inen paket altyazıların (`AddonStreamClient.kt`) sadece ilk dosyasını ayıklama hatası giderildi. Artık paketteki tüm altyazı dosyaları ayıklanır.
- **Çakışma Önleyici Gelişmiş Eşleştirme:** Dosya isimlerindeki gürültüler (çözünürlük, CRC hash, yıl vb.) temizlendikten sonra, oynatılan bölümün numarası özel bir algoritma ile tespit edilir. 
  - *Mob Psycho 100*, *3-gatsu no Lion*, *5-toubun no Hanayome* gibi isminde sayı içeren anime başlıklarında, isimdeki sayılarla bölüm numarasının çakışması matematiksel olarak engellenmiştir.
- **Önbellek Desteği:** Bir sezonun toplu altyazı paketi indirildiğinde sonraki bölüme geçildiğinde internetten tekrar indirme yapılmaz, önbellekteki doğru bölümün dosyası anında oynatıcıya yüklenir.

---

### 🎬 Oynatıcı Öncelik Yönetimi & İsimle Arama

- **Dahili > Harici Önceliği:** Oynatıcı motorunda (`Media3PlayerEngine.kt`, `PlayerSubtitleUtils.kt`), video kaynağına gömülü olan (dahili) Türkçe altyazılar, harici eklentilerden gelen dosyalara göre her zaman öncelikli olarak seçilir.
- **İsimle Arama Fallback:** Video akışının dosya adı anlamsız karakterler içeriyorsa, altyazı eklentilerinin veritabanında doğru altyazıyı bulabilmesi için temizlenmiş anime başlığı ve bölüm numarasından oluşan anlamlı bir dosya adı (örn. `Attack on Titan - S01E01.mkv`) simüle edilir.

---

### 💬 UI Geri Bildirimi

- **Altyazı Bulunamadı Uyarısı:** Otomatik altyazı arama işlemi bittiğinde hiçbir Türkçe altyazı bulunamaz veya indirilemezse oynatıcı ekranında kullanıcıya bir uyarı mesajı (Toast) gösterilir.
