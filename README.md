# Generic AI Audit Tool

Generic AI Audit Tool, Jira benzeri yapılandırılmış kayıtları denetim için hazırlayan bir Java kütüphanesidir. Ham JSON verisini normalize eder, metadata, alan açıklamaları, checklist ve yorumlarla zenginleştirir, kanıta dayalı bir denetim promptu üretir ve bu promptu entegrasyon uygulamasının sağladığı LLM taşıma katmanına verir.

Projenin ana teslimatı `core` modülüdür. `demo` modülü ise aynı yaklaşımın yerel Ollama ve Spring Boot web arayüzüyle nasıl çalıştığını gösterir.

> Bu sistem karar desteği üretir. Model çıktısı nihai denetim kararı, sertifikasyon kanıtı veya otomatik uygunluk onayı değildir.

## Genel Akış

```text
Issue JSON + metadata + alan açıklamaları + checklist
                         |
                         v
                    AuditEngine
                         |
             normalize -> prompt oluştur
                         |
                         v
                  AgentTransport
                         |
                         v
              Kurum LLM endpointi
                         |
                         v
                 String denetim raporu
```

Core kütüphane dosya yolu, HTTP request gövdesi, kimlik doğrulama biçimi veya modele özel response alanı varsaymaz. Bu ayrıntılar, entegrasyon uygulamasının yazdığı `AgentTransport` implementasyonunda kalır.

## Temel Özellikler

- İç içe object ve array yapıları bulunan JSON kayıtlarını generic olarak gezer.
- Aktif, boş, null ve gürültü niteliğindeki alanları birbirinden ayırır.
- Teknik custom field kimliklerini metadata adı ve açıklamasıyla eşleştirir.
- Alan açıklamalarını metadata bilgisine ek bağlam olarak işler.
- Jira benzeri comment yapılarını yazar, zaman ve coverage bilgisiyle ayrı bağlamda tutar.
- Checklist maddelerini modele verilen denetim kriterleri olarak taşır.
- Prompt içindeki dinamik veriyi güvenilmeyen audit context olarak sınırlar.
- Ana kütüphane akışında model cevabını parse etmeden `String` olarak döndürür.
- Yerel demoda model seçimi, thinking ayarı ve yapılandırılmış rapor görünümü sunar.

## Modüller

| Bölüm | Sorumluluk | Üretim bağımlılığı olarak kullanılır mı? |
| --- | --- | --- |
| `core/` | Normalizasyon, prompt üretimi ve public kütüphane API'si | Evet |
| `opencode-adapter/` | Core normalizasyonunu kısıtlı OpenCode ajanına açan çalıştırılabilir CLI adapter | OpenCode kullanımı için |
| `demo/` | Spring Boot, Ollama adapterleri ve web arayüzü | Hayır, yalnızca gösterim ve yerel test |
| `evaluation/` | Senaryolar, beklenen sonuçlar ve demo girdileri | Test ve model değerlendirmesi için |
| `docs/` | Mimari, entegrasyon ve değerlendirme belgeleri | Başvuru kaynağı |

Modül sınırlarının gerekçesi için [Kütüphane ve Demo Ayrımı](docs/architecture/library_demo_ayrimi.md) belgesine bakın.

## Gereksinimler

- Java 21
- Maven 3.9 veya üzeri
- Yalnızca yerel demo ve gerçek model smoke testi için Ollama

Demo için önerilen mevcut model:

```bash
ollama pull qwen3:4b-instruct
```

Ollama masaüstü uygulaması açıksa API genellikle `http://localhost:11434` adresinde hazırdır. `ollama serve` komutunun “address already in use” hatası vermesi çoğunlukla servisin zaten çalıştığını gösterir.

## Hızlı Başlangıç

Tüm Java testlerini çalıştırın:

```bash
mvn clean test
```

Demoyu paketleyip başlatın:

```bash
mvn clean package -DskipTests
java -jar demo/target/audittool-demo-0.0.1-SNAPSHOT.jar
```

Ardından tarayıcıdan şu adresi açın:

```text
http://localhost:8080/demo/index.html
```

Arayüzde issue, metadata, Türkçe alan açıklamaları ve checklist dosyaları ayrı ayrı yüklenebilir. Hazır girdiler `evaluation/demo-inputs/`, tam request fixture'ları ise `evaluation/scenarios/fixtures/` altındadır.

Adım adım demo anlatımı için [Demo Akışı](docs/evaluation/demo_walkthrough.md) belgesini kullanın.

## Kütüphane Olarak Kullanım

Core artifact henüz merkezi bir Maven deposunda yayımlanmıyorsa önce yerel Maven deposuna kurulur:

```bash
mvn -pl core -am clean install
```

Tüketen projeye bağımlılık eklenir:

```xml
<dependency>
    <groupId>com.yusuf</groupId>
    <artifactId>audittool-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

En küçük kullanım akışı şöyledir:

```java
AgentTransport transport = (prompt, endpoint) ->
        companyAiClient.send(prompt, endpoint.getUri(), endpoint.getHeaders());

AuditEngine engine = new AuditEngine(transport);
AuditInput input = new AuditInput(issue, metadata, fieldDescriptions, checklist);
AgentEndpoint endpoint = AgentEndpoint.of("https://ai.example.internal/message");

String report = engine.analyze(input, endpoint);
```

Buradaki `issue`, `metadata`, `fieldDescriptions` ve `checklist` değerleri Jackson `JsonNode` nesneleridir; dosya yolu değildir. Dosyadan okuma veya HTTP request gövdesini parse etme işi kütüphaneyi kullanan uygulamaya aittir.

Gerçek projede tek metotlu bir facade, transport implementasyonu, header yönetimi, hata sınırları ve test örneği için [Kütüphane Entegrasyon Rehberi](docs/integration/kutuphane_entegrasyonu.md) belgesine bakın.

## OpenCode Ajanı Olarak Kullanım

İlk kurulumda typed tool'un kullanacağı adapter JAR'ını test edip paketleyin:

```bash
mvn -pl opencode-adapter -am clean package
```

Repository kökünde OpenCode'u başlatın:

```bash
opencode
```

Ardından issue ve opsiyonel yardımcı JSON dosyalarını `/audit` komutuna verin:

```text
/audit <issue.json> <metadata.json> <field-descriptions.json> <checklist.json>
```

Bu akışta OpenCode ajanı ham JSON'u doğrudan yorumlamaz ve bash çalıştıramaz. Şemalı `normalize_audit` aracı, `opencode-adapter` üzerinden aynı core normalizasyonunu çalıştırır; ajan yalnızca doğrulanmış normalize bağlamı `core_auditor.md` politikasıyla değerlendirir. Kurulum, izin sınırları, hazır örnek ve model/endpoint ayrımı için [OpenCode Denetim Ajanı Kullanım Rehberi](docs/integration/opencode_agent_kullanimi.md) belgesine bakın.

## Demo API

| İstek | Amaç | LLM çağrısı |
| --- | --- | --- |
| `GET /demo/api/health` | Demo uygulamasının ayakta olduğunu doğrular | Hayır |
| `GET /demo/api/models` | Kurulu Ollama modellerini listeler | Hayır |
| `POST /demo/api/normalize` | Üretilen `AgentContext` yapısını gösterir | Hayır |
| `POST /demo/api/analyze` | Normalizasyon, prompt ve model akışını çalıştırır | Evet |

Örnek analiz isteği:

```bash
curl -X POST http://localhost:8080/demo/api/analyze \
  -H "Content-Type: application/json" \
  --data-binary @evaluation/scenarios/fixtures/aud-002-missing-acceptance-criteria.json
```

Demo request sözleşmesinde `payload` zorunlu; `metadata`, `fieldDescriptions`, `checklist` ve `agentOptions` opsiyoneldir. Bu HTTP sözleşmesi demo modülüne aittir ve core kütüphane API'siyle karıştırılmamalıdır.

## Demo Yapılandırması

Varsayılan ayarlar `demo/src/main/resources/demo/application-demo.properties` dosyasındadır. Başlıca environment variable değerleri şunlardır:

```bash
export OLLAMA_MODEL=qwen3:4b-instruct
export OLLAMA_THINKING_ENABLED=false
export OLLAMA_CONTEXT_WINDOW=8192
export OLLAMA_MAX_OUTPUT_TOKENS=1200
```

Spring Boot `.env` dosyasını kendiliğinden yüklemez. Değerleri terminalden export edin veya IDE run configuration içine ekleyin. Tüm örnekler [.env.example](.env.example) dosyasında bulunur.

Thinking desteği modelden modele değişir ve daha küçük modelin daha hızlı sonuç vereceğini garanti etmez. Mevcut karşılaştırmalar [Güncel Model Sonuçları](docs/evaluation/current_results.md) belgesinde tutulur.

## Testler

Standart test paketi dış servise ihtiyaç duymaz:

```bash
mvn clean test
```

Yerel Ollama açıkken core'dan gerçek HTTP endpointine kadar olan akış manuel olarak çalıştırılabilir:

```bash
RUN_OLLAMA_INTEGRATION=true mvn -pl demo -am \
  -Dtest=OllamaAuditEngineIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Bu smoke testi normal `mvn clean test` çalışmasına bilinçli olarak dahil değildir; kurulu modele ve yerel donanıma bağlıdır.

## Proje Yapısı

```text
core/
  src/main/java/com/yusuf/audittool/
    api/          Public kütüphane API'si
    agent/        Entegrasyon transport sözleşmesi
    normalize/    JSON normalizasyonu
    metadata/     Metadata eşleştirmesi
    checklist/    Checklist dönüşümü
    prompt/       Context rendering ve prompt üretimi
    model/        Core veri modelleri
  src/main/resources/prompts/

demo/
  src/main/java/com/yusuf/audittool/demo/
    ollama/       Yerel Ollama istemcileri
    structured/   Demo rapor parse ve doğrulama akışı
    web/          Demo HTTP controllerları
  src/main/resources/static/demo/

evaluation/
  scenarios/      Fixture, tanım ve expected sözleşmeleri
  demo-inputs/    Ayrı yüklenebilen demo JSON dosyaları

docs/
  architecture/   Güncel mimari ve tasarım kararları
  integration/    Kütüphane entegrasyon rehberi
  evaluation/     Model ve senaryo değerlendirme belgeleri
  diagrams/       Elle yönetilen UML kaynakları
```

## Dokümantasyon

| İhtiyaç | Belge |
| --- | --- |
| Nereden başlamalıyım? | [Dokümantasyon Merkezi](docs/README.md) |
| Sistem içeride nasıl çalışıyor? | [Güncel Sistem Mimarisi](docs/architecture/guncel_sistem_mimarisi.md) |
| Core ile demo neden ayrıldı? | [Kütüphane ve Demo Ayrımı](docs/architecture/library_demo_ayrimi.md) |
| Başka bir projeye nasıl eklerim? | [Kütüphane Entegrasyon Rehberi](docs/integration/kutuphane_entegrasyonu.md) |
| Prompt profilleri nasıl kullanılıyor? | [Güncel Sistem Mimarisi - Prompt Üretimi](docs/architecture/guncel_sistem_mimarisi.md#prompt-üretimi) |
| Demo senaryolarını nasıl çalıştırırım? | [Demo Akışı](docs/evaluation/demo_walkthrough.md) |
| Model kalitesi nasıl ölçülüyor? | [Değerlendirme Stratejisi](docs/evaluation/evaluation_strategy.md) |
| Bilinen model sonuçları neler? | [Güncel Model Sonuçları](docs/evaluation/current_results.md) |

Tüm belgeler ve okuma sırası [docs/README.md](docs/README.md) içinde bir araya getirilmiştir.

## Kapsam Sınırları

- Core, Jira API'sinden veri çekmez; kendisine verilen JSON'u işler.
- Core, kurum endpointinin request ve response sözleşmesini tahmin etmez.
- Core, LLM çıktısını kesin doğru kabul etmez ve ana akışta parse etmeye zorlamaz.
- Metadata veya checklist yokluğu tek başına denetim bulgusu değildir.
- DO-178C perspektifi kanıt disiplini sağlar; sistem sertifikasyon veya uygunluk kararı vermez.
