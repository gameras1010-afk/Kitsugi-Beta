# Kitsugi Ağ ve Performans Güncellemesi Sürüm Notları 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Ağ Donma ve Bağlantı Kilitlenme Sorunlarının Çözümü (OkHttp Thread Starvation Fixes)
- **Anlık Ağ İptali (Immediate Cancellation on Exit):** "İzle" (yayın seçici) sayfasından geri çıkıldığı anda, arka planda çalışmaya devam eden tüm OkHttp ağ istekleri (sağlayıcı arama ve link çözücüler) anında iptal edilir. Bu sayede arka planda asılı kalan ve tüm ağ motorunu kilitleyen "Thread Sızıntısı/Açlığı" tamamen önlenmiştir.
- **İptal Duyarlı Interceptor Yapısı:** `RetryInterceptor` (429 Too Many Requests) ve `CloudflareInterceptor` bekleme döngüleri, ağ isteği iptal edildiği anda uykuyu yarıda kesip thread'leri serbest bırakacak şekilde güncellendi. Yavaş siteler için gerekli bekleme süreleri korunurken, sayfa kapatıldığında kaynakların anında geri kazanılması sağlandı.
- **Bellek İçi TMDB Önbelleği (TmdbApiClient Cache):** Ana sayfa ve detay ekranlarında sıklıkla tetiklenen TMDB API anahtarı ve dil sorgularındaki `runBlocking` kilitleri tamamen kaldırılarak RAM tabanlı asenkron önbelleğe geçirildi. Ağ/Ana thread üzerindeki disk I/O yükü sıfırlandı.
- **Eklenti Güvenlik İzolasyonu:** `CsPluginLoader` içerisindeki eklenti yükleme adımları zaman aşımı (`withTimeout`) ve hata korumalarıyla izole edildi. Hatalı veya kilitlenen eklentilerin ana uygulamayı çökertmesi veya dondurması kalıcı olarak engellendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Network Freeze & Connection Starvation Fixes (OkHttp Thread Starvation Fixes)
- **Immediate Network Cancellation on Exit:** The moment you exit the "Watch" (stream picker) screen, all ongoing background OkHttp network requests (extension search & stream link resolve jobs) are aborted instantly. This completely eliminates background thread leaks that used to freeze the entire network engine.
- **Cancellation-Aware Retry Interceptors:** Wait loops inside `RetryInterceptor` (handling 429 Too Many Requests) and `CloudflareInterceptor` are now fully cancellation-aware. If the request is cancelled, they immediately abort their sleep/await states and release dispatcher threads.
- **In-Memory TMDB Client Caching:** Replaced thread-blocking `runBlocking` calls for TMDB API key and language settings with a memory-efficient, asynchronously-updated cache structure. This eliminates main/network thread disk I/O bottlenecks.
- **Plugin Loading Sandbox:** Integrated strict timeout limits (`withTimeout`) and exception wrappers inside `CsPluginLoader` to isolate third-party extension loading, preventing malicious or poorly-written plugin code from crashing or freezing the main application process.
