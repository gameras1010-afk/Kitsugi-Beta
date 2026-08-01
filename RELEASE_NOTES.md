# Kitsugi Release Notes - v2.4.129-beta

Bu sürümde Kitaplığım (MyList) senkronizasyon/arama deneyimi tamamen iyileştirildi, dizi/film (TMDB) indirmelerine altyazı desteği getirildi ve altyazı eklentileri için çift kimlikli (IMDb + Kitsu) paralel arama sistemi kuruldu.

### 🔄 Kitaplığım (MyList) Senkronizasyon & Arama
- **Son Güncelleme Zamanı (updatedAt):** Veritabanı sürümü 27'ye yükseltilerek tüm kayıtlara `updatedAt` sütunu eklendi. Artık listemdeki içerikler en son izlenme veya eklenme tarihine göre kusursuz bir şekilde sıralanıyor.
- **Küresel Arama (Global Search):** Kitaplığım ekranında bir arama yapıldığında platform tabanlı filtreler otomatik olarak devre dışı bırakılır. Arama sorgusu girildiğinde veritabanındaki tüm AniList, MAL ve Simkl içerikleri tek bir ekranda listelenir.
- **Giriş/Boş Ekran Uyarılarının Bypass Edilmesi:** Arama yapıldığı esnada giriş yapılmamış tablar veya boş sayfalar için çıkan uyarılar gizlenerek doğrudan arama sonuçları gösterilir.

### 🌍 Çift Kimlikli Paralel Altyazı Arama (IMDb + Kitsu)
- **Çift Sorgu Desteği:** Altyazı aratılırken sadece tek bir kimlik formatı seçmek yerine, eğer videonun hem IMDb hem de Kitsu kimlikleri mevcutsa, her iki kimlik ile de altyazı eklentileri paralel olarak taranır ve gelen sonuçlar birleştirilir.
- **Evrensel Eklenti Uyumu:** Sadece IMDb destekleyen genel altyazı sağlayıcıları (TürkçeAltyazı.org, YTS, OpenSubtitles) ile anime eklentilerinden gelen altyazılar aynı anda çözümlenip hem oynatıcıda gösterilir hem de indirilebilir.

### 📥 TMDB İndirmelerine Altyazı Desteği
- **İndirme Metadata Genişletmesi:** İndirme modeline (`AnimeDownload`) `tmdbId` desteği eklendi.
- **Otomatik Altyazı Çözümleme:** Dizi ve film (TMDB) indirmeleri başlatıldığında TMDB kimlikleri altyazı servisine iletilerek doğru IMDb kodları çözümlenir ve altyazıları video ile birlikte sorunsuz indirilir.
