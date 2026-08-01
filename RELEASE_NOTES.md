# Kitsugi Release Notes - v2.4.133-beta

Bu sürümde dinamik eklentilerin (özellikle SynclerPlugin) ihtiyaç duyduğu AndroidX AppCompat ve AndroidX Navigation Fragment kütüphane bağımlılıkları projeye dahil edilerek başlangıç çökmeleri tamamen giderilmiştir.

### 🚀 Eklenti Sınıf Yükleme Çökmeleri Giderildi
- **Navigation Fragment Entegrasyonu:** Syncler ve benzeri eklentilerin çalışma zamanında ihtiyaç duyduğu `androidx.navigation.fragment.NavHostFragment` sınıfının eksikliğinden kaynaklanan `NoClassDefFoundError` çökmesi, projeye `navigation-fragment-ktx` kütüphanesi dahil edilerek çözüldü.
- **AppCompat Desteği:** Eklentilerde kullanılan AppCompat arayüz sınıfları (örneğin `AppCompatActivity` ve `AlertDialog`) için projeye `androidx.appcompat:appcompat` kütüphanesi eklendi.

### 🛡️ R8/ProGuard Koruma Kuralları
- ProGuard yapılandırma dosyasına (`proguard-rules.pro`) AppCompat sınıflarını koruma kuralları (`-keep class androidx.appcompat.** { *; }`) eklenerek R8 optimizasyonları esnasında bu sınıfların temizlenmesi önlendi.
