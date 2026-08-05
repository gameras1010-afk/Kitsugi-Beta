# Anisub CLI

Türkçe anime alt yazı platformu anisub.co için cli aracı.

## Kurulum
Rust ve cargo kurulu olduğundan emin olunuz.

```bash
cargo install anisub-cli
```

## Kullanım

### Giriş Yapma (Opsiyonel)
Kendi tokeniniz ile indirme yapmak için giriş yapabilirsiniz. 
[Token almak için tıkla.](https://anisub.co/ayarlar#api)

```bash
anisub-cli login
```

### Arama Yapma ve İndirme
Anime veya altyazı adıyla arama yapabilirsiniz.

```bash
anisub-cli search "bleach"
```
veya belirli bir klasöre indirmek için `-o` parametresini kullanın:
```bash
anisub-cli search "bleach" -o ~/Indirilenler/Altyazilar
```

### Kısayollar (Arama Ekranında)
- **Aşağı/Yukarı Ok:** Sonuçlar arasında gezinme
- **Enter:** Seçili altyazıyı indir
- **Q / ESC:** Çıkış yap
