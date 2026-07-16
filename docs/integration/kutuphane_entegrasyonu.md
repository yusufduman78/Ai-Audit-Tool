# Kütüphane Entegrasyon Rehberi

Bu rehber, `audittool-core` modülünün başka bir Java uygulamasından nasıl kullanılacağını açıklar. Hedef kullanım, issue ve yardımcı JSON verilerini alan tek bir facade metodunun kurum LLM endpointine denetim isteği göndermesi ve model raporunu `String` olarak döndürmesidir.

Projeyi OpenCode içinde dosya yolları verilen etkileşimli bir ajan olarak çalıştırmak farklı bir kullanım biçimidir. Bunun için [OpenCode Denetim Ajanı Kullanım Rehberi](opencode_agent_kullanimi.md) kullanılmalıdır.

İç mimariyi önce görmek isterseniz [Güncel Sistem Mimarisi](../architecture/guncel_sistem_mimarisi.md), core ile demonun sınırı için [Kütüphane ve Demo Ayrımı](../architecture/library_demo_ayrimi.md) belgesini okuyun.

## İçindekiler

- [Entegrasyon sözleşmesi](#entegrasyon-sözleşmesi)
- [Core artifact'ini hazırlama](#1-core-artifactini-hazırlama)
- [Kurum transportunu yazma](#2-kurum-transportunu-yazma)
- [Endpointi tanımlama](#3-endpointi-tanımlama)
- [Uygulama facade'ını oluşturma](#4-uygulama-facadeını-oluşturma)
- [JSON dosyalarından kullanım](#5-json-dosyalarından-kullanım)
- [HTTP controller içinden kullanım](#6-http-controller-içinden-kullanım)
- [Hata yönetimi](#8-hata-yönetimi)
- [Unit test örneği](#9-unit-test-örneği)
- [Gerçek entegrasyon kontrol listesi](#10-gerçek-entegrasyon-öncesi-kontrol-listesi)

## Entegrasyon Sözleşmesi

Core kütüphanenin dışarıya açtığı temel tipler şunlardır:

| Tip | Sorumluluk |
| --- | --- |
| `AuditInput` | Issue, metadata, alan açıklamaları ve checklist `JsonNode` değerlerini taşır |
| `AgentEndpoint` | Mutlak HTTP/HTTPS endpoint adresini ve isteğe eklenecek header'ları taşır |
| `AgentTransport` | Kurumun gerçek request/response sözleşmesini uygular |
| `AuditEngine` | Normalizasyon, prompt üretimi ve transport çağrısını tek akışta yönetir |

Ana çağrı şudur:

```java
String report = auditEngine.analyze(auditInput, agentEndpoint);
```

`AuditEngine` dosya yolu almaz. Girdi değerleri parse edilmiş Jackson `JsonNode` nesneleridir. Bu sayede JSON'un dosyadan, HTTP request gövdesinden, Jira istemcisinden veya başka bir servisten gelmesi core davranışını değiştirmez.

## 1. Core Artifact'ini Hazırlama

Artifact merkezi bir Maven deposunda yayımlanmıyorsa bu repository içinde yerel Maven deposuna kurun:

```bash
mvn -pl core -am clean install
```

Tüketen projenin `pom.xml` dosyasına bağımlılığı ekleyin:

```xml
<dependency>
    <groupId>com.yusuf</groupId>
    <artifactId>audittool-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Kurum içi Nexus veya Artifactory kullanılıyorsa aynı artifact o depoya yayımlanabilir. Tüketen proje açısından API değişmez.

## 2. Kurum Transportunu Yazma

Endpoint URL'si, kurumun HTTP mesaj sözleşmesini tek başına açıklamaz. Örneğin promptun request içinde `message`, `prompt` veya başka bir alanda gönderilmesi; model cevabının `response`, `content` veya iç içe bir JSON yolundan okunması mümkündür.

Bu nedenle sözleşmeye özel kod `AgentTransport` arkasında tutulur:

```java
import com.yusuf.audittool.agent.AgentTransport;
import com.yusuf.audittool.api.AgentEndpoint;

public final class CompanyAgentTransport implements AgentTransport {

    private final CompanyAiClient client;

    public CompanyAgentTransport(CompanyAiClient client) {
        this.client = client;
    }

    @Override
    public String send(String prompt, AgentEndpoint endpoint) {
        return client.sendMessage(
                endpoint.getUri(),
                endpoint.getHeaders(),
                prompt
        );
    }
}
```

`CompanyAiClient`, kurumun zaten kullandığı HTTP istemcisi veya entegrasyon SDK'sı olabilir. Şu ayrıntılar bu sınıfın içinde kalmalıdır:

- Request body alan adları.
- Authentication ve kurum header'ları.
- Timeout, proxy ve TLS ayarları.
- Response JSON içinden final metnin okunması.
- Kurum hata kodlarının Java exception'larına çevrilmesi.

Kurum mesaj sözleşmesi öğrenilene kadar core içinde tahmini bir request body tanımlanmamalıdır. Sözleşme geldiğinde `NormalizeService`, `PromptBuilder` veya `AuditEngine` değişmez; yalnızca transport tamamlanır.

## 3. Endpointi Tanımlama

Endpoint uygulama başlatılırken bir kez oluşturulabilir:

```java
import java.net.URI;
import java.util.Map;

import com.yusuf.audittool.api.AgentEndpoint;

AgentEndpoint endpoint = new AgentEndpoint(
        URI.create("https://ai.example.internal/message"),
        Map.of("Authorization", "Bearer " + accessToken)
);
```

`AgentEndpoint` yalnızca mutlak `http` veya `https` adreslerini kabul eder. Header map'i constructor sırasında kopyalanır. Token gibi gizli değerler kaynak koda veya dokümantasyona yazılmamalı; uygulamanın secret/configuration mekanizmasından alınmalıdır.

Endpoint aynı kalıyorsa her analizde yeniden parametre olarak toplamak yerine facade içinde saklanabilir. Endpoint tenant, model veya ortam bazında değişiyorsa çağrı düzeyinde seçilebilir.

## 4. Uygulama Facade'ını Oluşturma

Tüketen projede küçük bir facade, core tiplerini uygulamanın geri kalanından ayırır:

```java
import java.util.Objects;

import com.yusuf.audittool.agent.AgentTransport;
import com.yusuf.audittool.api.AgentEndpoint;
import com.yusuf.audittool.api.AuditEngine;
import com.yusuf.audittool.api.AuditInput;

import tools.jackson.databind.JsonNode;

public final class AuditFacade {

    private final AuditEngine auditEngine;
    private final AgentEndpoint agentEndpoint;

    public AuditFacade(AgentTransport transport, AgentEndpoint agentEndpoint) {
        this.auditEngine = new AuditEngine(transport);
        this.agentEndpoint = Objects.requireNonNull(agentEndpoint);
    }

    public String analyze(
            JsonNode issue,
            JsonNode metadata,
            JsonNode fieldDescriptions,
            JsonNode checklist
    ) {
        AuditInput input = new AuditInput(
                issue,
                metadata,
                fieldDescriptions,
                checklist
        );
        return auditEngine.analyze(input, agentEndpoint);
    }
}
```

Uygulama başlangıcında nesneler bir kez hazırlanır:

```java
CompanyAiClient client = new CompanyAiClient(/* kurum ayarları */);
AgentTransport transport = new CompanyAgentTransport(client);
AgentEndpoint endpoint = AgentEndpoint.of("https://ai.example.internal/message");

AuditFacade auditFacade = new AuditFacade(transport, endpoint);
```

Analiz sırasında yalnızca veriler verilir:

```java
String report = auditFacade.analyze(
        issueJson,
        metadataJson,
        fieldDescriptionsJson,
        checklistJson
);
```

Metadata, alan açıklamaları ve checklist opsiyoneldir; bu değerler yoksa `null` verilebilir. Issue payload zorunludur.

## 5. JSON Dosyalarından Kullanım

Dosya okuma core'un değil, çağıran uygulamanın sorumluluğudur:

```java
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

JsonMapper mapper = new JsonMapper();

JsonNode issue = mapper.readTree(Files.readString(Path.of("issue.json")));
JsonNode metadata = mapper.readTree(Files.readString(Path.of("metadata.json")));
JsonNode descriptions = mapper.readTree(Files.readString(Path.of("field-descriptions.json")));
JsonNode checklist = mapper.readTree(Files.readString(Path.of("checklist.json")));

String report = auditFacade.analyze(issue, metadata, descriptions, checklist);
```

Bu örnek yalnızca dosyadan okuma senaryosudur. Üretim uygulamasında dosya yolu yerine Jira istemcisinden veya controller request gövdesinden gelen `JsonNode` değerleri doğrudan kullanılabilir.

## 6. HTTP Controller İçinden Kullanım

Spring MVC kullanan tüketen projede request modeli core'dan bağımsız tutulabilir:

```java
public record AuditRequest(
        JsonNode issue,
        JsonNode metadata,
        JsonNode fieldDescriptions,
        JsonNode checklist
) {
}
```

Controller yalnızca facade'ı çağırır:

```java
@PostMapping("/audit")
public String audit(@RequestBody AuditRequest request) {
    return auditFacade.analyze(
            request.issue(),
            request.metadata(),
            request.fieldDescriptions(),
            request.checklist()
    );
}
```

Bu endpoint örneği tüketen uygulamaya aittir. Core modül kendi HTTP endpointini zorunlu kılmaz.

## 7. Dönen Cevap

Core akışında model cevabı trim edilmiş ham `String` olarak döner. Varsayılan prompt, okunabilir Markdown başlıkları ister:

```markdown
## Özet
## Bulgular
## Gözlemler ve Yetersiz Bağlam
## Önerilen Aksiyonlar
```

Core cevabı JSON'a zorlamaz, parse etmez ve başlık eksikliği nedeniyle raporu kaybetmez. Çağıran uygulama bu metni doğrudan gösterebilir, saklayabilir veya kendi güvenilir parser sözleşmesini ayrıca uygulayabilir.

Yerel web demosundaki yapılandırılmış JSON rapor akışı demo özelliğidir. Ana kütüphane sözleşmesi değildir. Ayrıntı için [Kütüphane ve Demo Ayrımı](../architecture/library_demo_ayrimi.md#prompt-profilleri) bölümüne bakın.

## 8. Hata Yönetimi

| Durum | Beklenen davranış |
| --- | --- |
| Issue payload `null` | `IllegalArgumentException` |
| Endpoint göreli veya HTTP/HTTPS dışı | `IllegalArgumentException` |
| Transport `null` veya boş cevap döndürür | `AgentRuntimeException` |
| Kurum endpointi timeout veya hata döndürür | Transport bunu uygun bir runtime exception ile sarmalamalıdır |
| Metadata/checklist yok | Analiz mevcut bağlamla devam eder |

Transport hata mesajlarında access token, tam prompt veya hassas response body loglanmamalıdır. Uygulama seviyesinde correlation ID kullanılabilir; ancak issue içeriğinin loglanması ayrı bir veri güvenliği kararıdır.

## 9. Unit Test Örneği

Core entegrasyonu gerçek LLM olmadan test edilebilir:

```java
AtomicReference<String> capturedPrompt = new AtomicReference<>();

AgentTransport fakeTransport = (prompt, endpoint) -> {
    capturedPrompt.set(prompt);
    return "## Özet\nKayıt incelendi.";
};

AuditFacade facade = new AuditFacade(
        fakeTransport,
        AgentEndpoint.of("https://ai.example.test/message")
);

String report = facade.analyze(issue, metadata, descriptions, checklist);

assertTrue(capturedPrompt.get().contains("ACTIVE FIELDS"));
assertTrue(capturedPrompt.get().contains("CHECKLIST"));
assertEquals("## Özet\nKayıt incelendi.", report);
```

Bu test normalizasyon ile prompt üretiminin transporta ulaştığını doğrular. Kurum HTTP sözleşmesi için ayrıca `CompanyAgentTransport` seviyesinde mock HTTP testi yazılmalıdır.

## 10. Gerçek Entegrasyon Öncesi Kontrol Listesi

- [ ] Kurum endpointinin tam URL'si ve ortamları belirlendi.
- [ ] Authentication ve zorunlu header'lar belirlendi.
- [ ] Promptun request body içindeki alanı belirlendi.
- [ ] Model cevabının response JSON içindeki yolu belirlendi.
- [ ] Timeout, maksimum response boyutu ve retry politikası belirlendi.
- [ ] Hassas payload ve promptların loglanmaması doğrulandı.
- [ ] Fake transport ile core entegrasyon testi yazıldı.
- [ ] Mock HTTP ile kurum transport sözleşmesi test edildi.
- [ ] Anonimleştirilmiş temsilî veriyle kontrollü smoke testi yapıldı.
- [ ] Model çıktısının karar desteği olduğu kullanıcıya gösterildi.

## İlgili Belgeler

- [Ana README](../../README.md)
- [Güncel Sistem Mimarisi](../architecture/guncel_sistem_mimarisi.md)
- [Kütüphane ve Demo Ayrımı](../architecture/library_demo_ayrimi.md)
- [OpenCode Denetim Ajanı Kullanım Rehberi](opencode_agent_kullanimi.md)
- [Değerlendirme Stratejisi](../evaluation/evaluation_strategy.md)
- [Dokümantasyon Merkezi](../README.md)
