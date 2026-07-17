# OpenCode Denetim Ajanı Kullanım Rehberi

Bu rehber, projedeki Java normalizasyon akışını OpenCode içinden bir denetim ajanı olarak kullanmayı açıklar. Bu kullanım biçiminde kullanıcı Java sınıflarını doğrudan çağırmaz; proje içindeki JSON dosyalarının yollarını `/audit` komutuna verir.

Core kütüphanesini başka bir Java uygulamasına gömmek için [Kütüphane Entegrasyon Rehberi](kutuphane_entegrasyonu.md) kullanılmalıdır. OpenCode ajanı bu entegrasyonun yerine geçmez; aynı normalizasyon mantığına erişen ayrı bir kullanım yüzeyidir.

## Bileşenler

| Bileşen | Görevi |
| --- | --- |
| `.opencode/agents/audit-reviewer.md` | Ajanın rolünü, izinlerini ve çalışma sırasını tanımlar |
| `.opencode/commands/audit.md` | Kullanıcının çağırdığı `/audit` komutunu ve dosya argümanlarını tanımlar |
| `.opencode/tools/normalize_audit.ts` | Şemalı dosya argümanlarını shell kullanmadan Java adapter'a iletir |
| `opencode-adapter` | Dosyaları güvenli biçimde okuyup core normalizasyonuna veren çalıştırılabilir Java modülüdür |
| `AuditContextPreparer` | `AuditInput` verisini normalize edilmiş agent bağlamına dönüştüren core API'sidir |
| `core_auditor.md` | Kanıt eşiği, sınıflandırma ve veri güvenliği kurallarını tanımlar |
| `output_markdown.md` | Nihai raporun başlıklarını ve Markdown biçimini tanımlar |

## Ön Koşullar

- Java 21
- Maven 3.9 veya üzeri
- OpenCode CLI
- OpenCode içinde kullanılabilir bir model/provider yapılandırması

İlk kurulumda adapter'ı test edip paketleyin:

```bash
mvn -pl opencode-adapter -am clean package
```

Bu işlem `opencode-adapter/target/audittool-opencode-adapter.jar` dosyasını üretir. Audit ajanı Maven veya build aracı çalıştırmaz; yalnızca önceden hazırlanmış bu JAR'ı typed tool üzerinden kullanır. Java kodu değiştiğinde komutu yeniden çalıştırın.

OpenCode'u repository kökünde başlatın:

```bash
opencode
```

## En Kısa Kullanım

Issue dosyası zorunludur. Diğer üç dosya opsiyoneldir ve şu sırayla verilir:

```text
/audit <issue.json> <metadata.json> <field-descriptions.json> <checklist.json>
```

Repository içindeki hazır sentetik örnek:

```text
/audit evaluation/demo-inputs/aud-016-real-jira-shape-synthetic/issue.json evaluation/demo-inputs/aud-016-real-jira-shape-synthetic/metadata.json evaluation/demo-inputs/aud-016-real-jira-shape-synthetic/field-descriptions.json evaluation/demo-inputs/aud-016-real-jira-shape-synthetic/checklist.json
```

Yalnızca issue ile çalışmak da mümkündür:

```text
/audit evaluation/demo-inputs/aud-016-real-jira-shape-synthetic/issue.json
```

Opsiyonel bir dosya atlanacaksa kendisinden sonraki argümanlar da verilmemelidir. Örneğin metadata olmadan yalnızca dördüncü sıradaki checklist dosyasını geçirmek bu konumsal komut sözleşmesinde desteklenmez. Böyle bir durumda eksik yardımcı girdiler olmadan çalıştırın veya dosyaları tam sırayla sağlayın.

## Komut Çalışınca Ne Olur?

1. OpenCode `/audit` komutunu `audit-reviewer` ajanına yönlendirir.
2. Ajan kanonik denetim politikasını ve Markdown çıktı sözleşmesini yalnızca izin verilen iki repository dosyasından okur.
3. Ajan, kullanıcıdan gelen proje içi yolların tamamını tek bir `normalize_audit` tool çağrısına yerleştirir.
4. Typed tool Java process'ini shell olmadan argüman listesiyle başlatır.
5. Adapter CLI seçeneklerini ve dosya yollarını doğrular, JSON'u Jackson ile parse eder ve `AuditContextPreparer` üzerinden core normalizasyonunu çalıştırır.
6. Adapter yalnızca sürümlü bir JSON envelope döndürür; typed tool bunun içindeki normalize bağlamı sınır marker'larıyla ajana verir.
7. Ajan ham JSON yerine normalize edilmiş `ENTITY`, `ACTIVE FIELDS`, `EMPTY FIELDS`, `COMMENTS` ve `CHECKLIST` bölümlerini değerlendirir.
8. Sonuç yalnızca okunabilir Markdown denetim raporu olarak döner.

Bu akışta Java kodu veriyi hazırlar; model kanıtlar arasındaki ilişkileri yorumlar. Normalizasyon tek başına bulgu üretmez.

## İzin ve Güvenlik Sınırı

`audit-reviewer` genel amaçlı bir kodlama ajanı değildir. Proje agent tanımı:

- kaynak kodu değiştirme araçlarını kapatır;
- ham issue, metadata ve checklist dosyalarını `read` ile açmasına izin vermez;
- yalnızca iki güvenilir prompt dosyasını okuyabilir;
- bash, web, alt ajan, dış dizin ve düzenleme araçlarını kapatır;
- yalnızca şemalı `normalize_audit` aracını çağırabilir;
- repository dışındaki, mutlak, symlink ile dışarı çıkan, shell karakterli, JSON olmayan veya 20 MiB üzerindeki dosya yollarını Java adapter seviyesinde reddeder;
- normalizasyon başarısız olduğunda girdileri azaltarak ikinci kez denemez ve audit raporu üretmez.

Typed tool en fazla 30 saniye bekler ve adapter response boyutunu 8 MiB ile sınırlar. Adapter `--issue`, `--metadata`, `--field-descriptions` ve `--checklist` dışındaki seçenekleri; tekrarlanan veya değeri eksik seçenekleri kabul etmez. `--worktree` model tarafından verilemez; güvenilir kök OpenCode oturumunun worktree bilgisinden ve process çalışma dizininden alınır.

Payload, metadata açıklamaları, yorumlar ve checklist metinleri güvenilmeyen denetim verisidir. İçlerinde `ignore previous instructions` benzeri bir ifade bulunması ajanın rolünü veya çıktı kurallarını değiştirmez.

## Model ve Endpoint Ayrımı

OpenCode ajanı, OpenCode içinde seçilmiş provider/model ile çalışır. `/audit` komutu doğrudan `AgentEndpoint` veya kurum HTTP endpointi almaz.

Kurumun AI sunucusu OpenCode provider olarak yapılandırılabiliyorsa ajan o modelle kullanılabilir. Kurum yalnızca özel bir HTTP request/response sözleşmesi sağlıyorsa [Kütüphane Entegrasyon Rehberi](kutuphane_entegrasyonu.md) içindeki `AgentTransport` yaklaşımı kullanılmalıdır.

Özetle iki yol vardır:

| İhtiyaç | Kullanılacak yol |
| --- | --- |
| OpenCode içinde dosya yollarıyla etkileşimli denetim | `/audit` ve `audit-reviewer` |
| Başka bir Java ürününden kurum endpointine programatik çağrı | `AuditEngine` ve `AgentTransport` |

## Beklenen Çıktı

Rapor şu bölümleri içerir:

```markdown
## Özet
## Bulgular
## Gözlemler ve Yetersiz Bağlam
## Önerilen Aksiyonlar
```

Bir bulgu bulunması zorunlu değildir. Sağlanan kayıt checklist ve metadata beklentilerini karşılıyorsa `Bulgular` bölümü desteklenen bulgu olmadığını açıkça söyleyebilir.

## Sorun Giderme

| Belirti | Muhtemel neden | Çözüm |
| --- | --- | --- |
| `MISSING_REQUIRED_OPTION` | İlk dosya yolu verilmedi | `/audit` çağrısına issue yolunu ilk argüman olarak ekleyin |
| `ADAPTER_NOT_BUILT` | Adapter JAR'ı henüz üretilmedi | `mvn -pl opencode-adapter -am clean package` çalıştırın |
| `INVALID_INPUT_PATH` | Yol mutlak, worktree dışında, taşınabilir değil veya `.json` ile bitmiyor | Proje içindeki geçerli göreli JSON yolunu kullanın |
| `INVALID_JSON` | Dosya parse edilebilir JSON değil | JSON sözdizimini doğrulayın |
| `INPUT_TOO_LARGE` | Dosya 20 MiB sınırını aşıyor | Girdiyi kontrollü biçimde küçültün |
| `ADAPTER_TIMEOUT` | Normalizasyon 30 saniyede bitmedi | Girdi boyutunu ve yerel Java sürecini inceleyin |
| `Token refresh failed: 401` | OpenCode provider oturumu sona erdi | OpenCode provider girişini yenileyin veya kullanılabilir başka model seçin |
| Rapor yerine adapter hatası döndü | Girdi okunamadı veya normalizasyon başlayamadı | Hata mesajında belirtilen dosya ve argümanı düzeltin |

## Mentöre Teslim Akışı

Mentör repository'yi çektikten sonra:

1. `mvn -pl opencode-adapter -am clean package` ile Java tarafını doğrular ve adapter JAR'ını üretir.
2. Repository kökünde `opencode` komutunu çalıştırır.
3. Kendi OpenCode model/provider ayarını seçer.
4. Proje içindeki JSON yollarını `/audit` komutuna verir.
5. Üretilen raporu karar desteği olarak inceler; nihai uygunluk veya sertifikasyon kararı olarak kullanmaz.

## İlgili Belgeler

- [Ana README](../../README.md)
- [Kütüphane Entegrasyon Rehberi](kutuphane_entegrasyonu.md)
- [Güncel Sistem Mimarisi](../architecture/guncel_sistem_mimarisi.md)
- [Kütüphane ve Demo Ayrımı](../architecture/library_demo_ayrimi.md)
- [Değerlendirme Stratejisi](../evaluation/evaluation_strategy.md)
