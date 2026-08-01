# Kitsugi Release Notes - v2.4.134-beta

Bu sürümde, Stremio altyazı eklentileri (TurkceAltyazi.org, YTS Subtitles vb.) üzerinden dizi ve anime altyazılarının çekilememesi ve indirilememesi sorunu giderilmiştir.

### 🎬 Stremio Altyazı Eklentileri Çekim Hatası Düzeltildi
- **URL Encoding Desteği:** Dizi ve anime bölümlerinin Stremio altyazı eklentilerine gönderilen sorgu parametrelerindeki `id` (örneğin IMDb ID'si olan `tt2560140:1:1` formatı) artık standart RFC 3986 formatında URL-encode edilerek (`tt2560140%3A1%3A1`) gönderilmektedir.
- **Sunucu İletişim Uyumsuzluğu Giderildi:** Web sunucularının ve yönlendiricilerin unencoded `:` (iki nokta) karakterini hatalı yorumlaması ve istekleri reddetmesi veya boş yanıt döndürmesi engellenmiştir. Bu sayede TurkceAltyazi.org ve YTS gibi Türkçe ve diğer harici altyazı sağlayıcıları aktif olarak çalışmaktadır.
