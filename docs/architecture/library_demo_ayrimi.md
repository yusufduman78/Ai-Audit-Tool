# Kütüphane ve Demo Ayrımı

Bu belge, `core`, `opencode-adapter` ve `demo` modüllerinin neden ayrıldığını ve hangi kodun hangi tarafta kalması gerektiğini tanımlar. Public API'nin ayrıntıları [Kütüphane Entegrasyon Rehberi](../integration/kutuphane_entegrasyonu.md), OpenCode kullanımı [OpenCode Denetim Ajanı Kullanım Rehberi](../integration/opencode_agent_kullanimi.md), iç çalışma akışı ise [Güncel Sistem Mimarisi](guncel_sistem_mimarisi.md) belgesindedir.

## Karar Özeti

Projenin asıl teslimatı, başka bir Java uygulamasının çağırabileceği denetim motorudur. Web arayüzü, Ollama bağlantısı, model kataloğu ve JSON rapor kartları bu motorun yerel gösterimidir.

```text
audittool-core  <- üretimde tüketilecek kütüphane
      ^                    ^
      |                    |
audittool-demo      opencode-adapter
yerel web gösterimi  kısıtlı agent CLI'ı
```

Bağımlılık tek yönlüdür: `demo` ile `opencode-adapter`, `core` artifact'ini kullanır; `core` bu iki modülün paketlerini import etmez.

## Sorumluluk Matrisi

| Sorumluluk | Core | OpenCode adapter/ajan | Demo | Tüketen kurum uygulaması |
| --- | --- | --- | --- | --- |
| JSON normalizasyonu | Evet | Core üzerinden | Core üzerinden | Core üzerinden |
| Metadata ve checklist eşleştirmesi | Evet | Core üzerinden | Core üzerinden | Core üzerinden |
| Ortak audit promptu | Evet | Core promptunu okur | Core üzerinden | Core üzerinden |
| Dosya yolu -> normalize context CLI | Hayır | Evet | Hayır | Kendi ihtiyacına göre |
| Kurum HTTP mesaj sözleşmesi | Hayır | Hayır | Hayır | `AgentTransport` ile |
| Yerel Ollama sözleşmesi | Hayır | OpenCode provider'a bağlı | Evet | İsteğe bağlı |
| Web dosya yükleme arayüzü | Hayır | Hayır | Evet | Kendi ihtiyacına göre |
| Structured JSON rapor kartları | Hayır | Hayır | Evet | Kendi ihtiyacına göre |
| Nihai iş kararı | Hayır | Hayır | Hayır | Yetkili kullanıcı |

## Core Paketleri

```text
core/src/main/java/com/yusuf/audittool
  api/          AuditEngine, AuditInput ve AgentEndpoint
  agent/        AgentTransport sözleşmesi ve runtime exception
  normalize/    JSON traversal, sınıflandırma, comment ve source extraction
  metadata/     Field metadata ve açıklama eşleştirmesi
  checklist/    Checklist dönüşümü
  prompt/       Context rendering ve prompt şablonları
  model/        Ortak normalize edilmiş veri modelleri
```

Core'un dışarıya önerilen giriş yüzeyi `api` ve `agent` paketleridir. Entegrasyon uygulaması normal kullanımda `NormalizeService`, `PromptBuilder` veya iç model sınıflarını tek tek kurmak zorunda değildir; `AuditEngine` bu akışı birleştirir.

## Core Kullanım Akışı

1. Entegrasyon uygulaması JSON değerlerini `AuditInput` içinde verir.
2. `AuditEngine`, girdiyi `AgentContext` yapısına normalize eder.
3. Ortak audit talimatı ve Markdown çıktı profiliyle prompt oluşturulur.
4. Prompt, çağıranın sağladığı `AgentTransport` nesnesine verilir.
5. Transport kurumun gerçek endpoint sözleşmesini uygular.
6. Modelden gelen final metin `String` olarak çağırana döner.

```java
AgentTransport transport = (prompt, endpoint) ->
        companyAiClient.send(prompt, endpoint.getUri(), endpoint.getHeaders());

AuditEngine engine = new AuditEngine(transport);
AuditInput input = new AuditInput(issue, metadata, fieldDescriptions, checklist);

String report = engine.analyze(
        input,
        AgentEndpoint.of("https://ai.example.internal/message")
);
```

Endpoint URL'si request/response sözleşmesini açıklamadığı için core body alan adlarını tahmin etmez. Kurum sözleşmesi öğrenildiğinde yalnızca transport implementasyonu eklenir.

Tek metotlu facade ve dosya/HTTP kullanım örnekleri için [Entegrasyon Rehberi - Uygulama Facade'ı](../integration/kutuphane_entegrasyonu.md#4-uygulama-facadeını-oluşturma) bölümüne bakın.

## Prompt Profilleri

Ortak audit mantığı ile çıktı biçimi ayrı dosyalardır:

| Dosya | Kullanım |
| --- | --- |
| `core_auditor.md` | Her iki akışta ortak rol, kanıt, güvenlik ve sınıflandırma kuralları |
| `output_markdown.md` | Core kütüphanenin doğrudan okunabilir metin raporu |
| `output_json.md` | Yerel demonun structured rapor kartları |

Core `AuditEngine`, Markdown profilini açıkça seçer. Demo Spring akışı varsayılan JSON profilini kullanır. Böylece çıktı biçimindeki bir değişiklik ortak audit kurallarını kopyalamayı gerektirmez.

Prompt üretiminin ayrıntıları [Güncel Sistem Mimarisi - Prompt Üretimi](guncel_sistem_mimarisi.md#prompt-üretimi) bölümündedir.

## OpenCode Adapter Sınırı

`opencode-adapter`, core'un ikinci bir audit motoru değildir. `NormalizeContextCommand` yalnızca proje içindeki JSON dosyalarını doğrular, `AuditInput` oluşturur ve `AuditContextPreparer` sonucunu standart çıktıya yazar.

Denetim kararını OpenCode içindeki `audit-reviewer` ajanı verir. Ajan ortak `core_auditor.md` politikasını ve `output_markdown.md` profilini kullanır. Böylece Java ürün entegrasyonu ile OpenCode ajanı aynı normalizasyon ve denetim ilkelerini paylaşır; model çağrısının sahibi farklı kalır.

## Demo Paketleri

```text
demo/src/main/java/com/yusuf/audittool/demo
  DemoAuditToolApplication.java  Spring Boot başlangıcı
  ollama/                        Ollama istemcileri, model kataloğu ve ayarlar
  structured/                    JSON rapor parserı ve validatoru
  web/                           Demo controllerları
  model/                         Demo request/response modelleri

demo/src/main/resources
  demo/application-demo.properties
  static/demo/                   HTML, CSS ve JavaScript
```

Demo adresleri `/demo` ön ekiyle ayrılır:

- Arayüz: `/demo/index.html`
- Sağlık: `/demo/api/health`
- Modeller: `/demo/api/models`
- Normalize: `/demo/api/normalize`
- Analiz: `/demo/api/analyze`

Kök `/` adresi kolaylık amacıyla demo arayüzüne yönlendirilebilir; bu davranış core kütüphane sözleşmesi değildir.

## İki Ollama Adapteri Neden Var?

| Adapter | Kullanıldığı akış | Çıktı |
| --- | --- | --- |
| `OllamaAgentClient` | Web demo `AuditService` | JSON profile ve structured rapor denemesi |
| `OllamaAgentTransport` | Core entegrasyon smoke testi | Markdown profile ve ham `String` rapor |

`OllamaAgentClient`; model seçimi, thinking ve runtime JSON Schema gibi demo özelliklerini taşır. `OllamaAgentTransport` ise `AgentTransport` abstraction'ının gerçek bir HTTP sunucusuna bağlanabildiğini gösteren küçük adapterdir.

Bu iki sınıfın birleştirilmesi zorunlu değildir; farklı ürün davranışlarını gösterirler. Kurum entegrasyonu ikisini de kullanmak zorunda değildir.

## Test Ayrımı

| Test grubu | Konum | Dış model gerekir mi? |
| --- | --- | --- |
| Core unit testleri | `core/src/test/java` | Hayır |
| OpenCode adapter unit testleri | `opencode-adapter/src/test/java` | Hayır |
| Demo web/structured testleri | `demo/src/test/java` | Hayır |
| Mock Ollama transport testi | `demo/src/test/java/.../ollama` | Hayır |
| Gerçek Ollama smoke testi | `OllamaAuditEngineIntegrationTest` | Evet, yalnızca açıkça etkinleştirilirse |

Standart test:

```bash
mvn clean test
```

Manuel gerçek Ollama testi:

```bash
RUN_OLLAMA_INTEGRATION=true mvn -pl demo -am \
  -Dtest=OllamaAuditEngineIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Build ve Artifact'ler

Kök `pom.xml` Maven reactor görevi görür:

```bash
mvn -pl core clean package
mvn -pl opencode-adapter -am clean package
mvn -pl demo -am clean package
```

Üretilen ana dosyalar:

```text
core/target/audittool-core-0.0.1-SNAPSHOT.jar
opencode-adapter/target/audittool-opencode-adapter.jar
demo/target/audittool-demo-0.0.1-SNAPSHOT.jar
```

Core artifact başka uygulamanın bağımlılığıdır. OpenCode adapter jar'ı yalnızca ajan tarafından çağrılan yerel CLI'dır. Demo jar ise çalıştırılabilir yerel gösterimdir.

## Sınır Koruma Kuralları

- Kuruma özel endpoint alan adları core'a eklenmez.
- Spring controller ve web response modelleri core'a taşınmaz.
- Ollama model parametreleri core public API'sini kirletmez.
- Ortak normalizasyon davranışı demo içinde kopyalanmaz.
- Core'da üretilen model metni demo parserı olmadan da kullanılabilir kalır.
- Demo kaldırıldığında core build ve testleri çalışmaya devam etmelidir.

## İlgili Belgeler

- [Dokümantasyon Merkezi](../README.md)
- [Güncel Sistem Mimarisi](guncel_sistem_mimarisi.md)
- [Kütüphane Entegrasyon Rehberi](../integration/kutuphane_entegrasyonu.md)
- [OpenCode Denetim Ajanı Kullanım Rehberi](../integration/opencode_agent_kullanimi.md)
- [Demo Akışı](../evaluation/demo_walkthrough.md)
