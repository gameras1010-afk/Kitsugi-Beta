# Kitsugi Release Notes - v2.4.135-beta

Bu sürümde, kullanıcıların video izleme/akış seçimi ekranındayken manuel olarak kaynak değiştirmesini ve tüm eklentiler üzerinde arama yapabilmesini sağlayan Manuel Arama özelliği entegre edilmiştir.

### 🔍 Eklentilerde Manuel Arama Entegrasyonu (Cloudstream 3 Uyumlu)
- **Manuel Arama Desteği:** Akış seçimi ekranına (hem dikey hem de yatay modlarda) yeni bir arama simgesi (büyüteç butonu) yerleştirilmiştir. Bu sayede otomatik arama sonuç vermediğinde veya alternatif kaynaklar aranmak istendiğinde kullanıcılar manuel arama başlatabilir.
- **Toplu Eklenti Araması:** Arama çubuğuna girilen anime, dizi veya film isimleri tüm aktif Cloudstream 3 (CS3) eklentilerinde eş zamanlı olarak aranır ve sonuçlar listelenir.
- **Akış Değiştirme:** Arama sonuçlarından herhangi bir kaynağa tıklandığında, sistem doğrudan o kaynağın URL ve API verilerini kullanarak yeni yayınları (StreamSource) çözümler ve ekranı günceller.
