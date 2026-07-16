# OpenCode Denetim Ajanı Kullanım Rehberi

Bu rehber, projedeki Java normalizasyon akışını OpenCode içinden bir denetim ajanı olarak kullanmayı açıklar. Bu kullanım biçiminde kullanıcı Java sınıflarını doğrudan çağırmaz; proje içindeki JSON dosyalarının yollarını `/audit` komutuna verir.

Core kütüphanesini başka bir Java uygulamasına gömmek için [Kütüphane Entegrasyon Rehberi](kutuphane_entegrasyonu.md) kullanılmalıdır. OpenCode ajanı bu entegrasyonun yerine geçmez; aynı normalizasyon mantığına erişen ayrı bir kullanım yüzeyidir.

## Bileşenler

| Bileşen | Görevi |
| --- | --- |
| `.opencode/agents/audit-reviewer.md` | Ajanın rolünü, izinlerini ve çalışma sırasını tanımlar |
| `.opencode/commands/audit.md` | Kullanıcının çağırdığı `/audit` komutunu ve dosya argümanlarını tanımlar |
| `opencode-adapter` | Dosyaları güvenli biçimde okuyup core normalizasyonuna veren çalıştırılabilir Java modülüdür |
| `AuditContextPreparer` | `AuditInput` verisini normalize edilmiş agent bağlamına dönüştüren core API'sidir |
| `core_auditor.md` | Kanıt eşiği, sınıflandırma ve veri güvenliği kurallarını tanımlar |
| `output_markdown.md` | Nihai raporun başlıklarını ve Markdown biçimini tanımlar |

## Ön Koşullar

- Java 21
- Maven 3.9 veya üzeri
- OpenCode CLI
- OpenCode içinde kullanılabilir bir model/provider yapılandırması

İlk kontrolde Java testlerini çalıştırın:

```bash
mvn -pl opencode-adapter -am test
```

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
2. Denetim politikası ile Markdown çıktı sözleşmesi ajanın güvenilir bağlamına eklenir.
3. Ajan yalnızca izin verilen Maven komutuyla `opencode-adapter` modülünü paketler.
4. Ajan adapter JAR'ını, kullanıcının verdiği proje içi dosya yollarıyla çalıştırır.
5. Adapter dosya yollarını doğrular, JSON'u Jackson ile parse eder ve `AuditContextPreparer` üzerinden core normalizasyonunu çalıştırır.
6. Ajan ham JSON yerine normalize edilmiş `ENTITY`, `ACTIVE FIELDS`, `EMPTY FIELDS`, `COMMENTS` ve `CHECKLIST` bölümlerini değerlendirir.
7. Sonuç yalnızca okunabilir Markdown denetim raporu olarak döner.

Bu akışta Java kodu veriyi hazırlar; model kanıtlar arasındaki ilişkileri yorumlar. Normalizasyon tek başına bulgu üretmez.

## İzin ve Güvenlik Sınırı

`audit-reviewer` genel amaçlı bir kodlama ajanı değildir. Proje agent tanımı:

- kaynak kodu değiştirme araçlarını kapatır;
- ham issue, metadata ve checklist dosyalarını `read` ile açmasına izin vermez;
- yalnızca iki güvenilir prompt dosyasını okuyabilir;
- yalnızca adapter'ı paketleyen ve çalıştıran iki komut biçimine izin verir;
- repository dışındaki dosya yollarını Java adapter seviyesinde reddeder.

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
| `Issue input is required` | İlk dosya yolu verilmedi | `/audit` çağrısına issue yolunu ilk argüman olarak ekleyin |
| `Input file must stay inside worktree` | Repository dışındaki dosya seçildi | Dosyayı kontrollü biçimde repository içindeki bir test klasörüne alın |
| `Invalid JSON` | Dosya parse edilebilir JSON değil | JSON sözdizimini doğrulayın |
| Maven veya Java komutu reddedildi | Ajan izin verilen komuttan farklı bir varyant denedi | Ajanın talimattaki komutu birebir yeniden çalıştırmasını sağlayın |
| `Token refresh failed: 401` | OpenCode provider oturumu sona erdi | OpenCode provider girişini yenileyin veya kullanılabilir başka model seçin |
| Rapor yerine adapter hatası döndü | Girdi okunamadı veya normalizasyon başlayamadı | Hata mesajında belirtilen dosya ve argümanı düzeltin |

## Mentöre Teslim Akışı

Mentör repository'yi çektikten sonra:

1. `mvn -pl opencode-adapter -am test` ile Java tarafını doğrular.
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
