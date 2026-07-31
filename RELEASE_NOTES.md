# Kitsugi Release Notes - v2.4.126-beta

Bu sürümde CS3 (Cloudstream) kaynaklarında kaynak bazlı kapak görsellerinin getirilmesi ve oynatıcıda her zaman görünen +85s intro atlama butonu eklendi.

### 🖼️ CS3 Kaynak Bazlı Kapak Görselleri

- **Cloudstream (CS3) Thumbnail Desteği:** Cloudstream eklentilerinden dönen `LoadResponse.posterUrl` alanı artık reflection ile okunuyor. Elde edilen kapak görseli, o eklentiden üretilen her `StreamSource`'a aktarılıyor.
- **Tüm Link Türlerinde Geçerli:** Doğrudan link, embed extractor (VK, Sibnet, Filemoon vb.), extractor fallback ve ham embed fallback dahil tüm CS3 stream oluşturma yollarında `thumbnailUrl` alanı dolduruluyor.
- **Çoklu Alan Adı Desteği:** `posterUrl`, `poster`, `coverImage`, `coverUrl`, `backgroundPosterUrl` gibi farklı CS3 eklenti versiyonlarında kullanılan alan adları refleksif olarak taranıyor; hangisi doluysa kullanılıyor.

### ⏩ Oynatıcıda Kalıcı "+Xs Giriş Atlama" Butonu

- **Her Zaman Görünen Skip Butonu:** Oynatıcı kontrol çubuğuna kalıcı bir `+85s` butonu eklendi. AniSkip/AnimeSkip verisi olmayan kaynaklarda bile giriş bölümünü tek tıkla atlatmak mümkün.
- **AniSkip Önceliği:** AniSkip veya AnimeSkip sistemleri aktif bir intro/outro/özet aralığı tespit ettiğinde, sabit butonun yerine dinamik "Giriş'i Atla" / "Bitişi Atla" butonu gösterilir.
- **Ayarlanabilir Süre:** Atlama süresi `getAnimeSkipIntroLength()` ile SharedPreferences'tan okunuyor; varsayılan 85 saniyedir ve MPV script üzerinden da değiştirilebilir.

