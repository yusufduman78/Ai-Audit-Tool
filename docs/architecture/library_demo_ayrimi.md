# Kütüphane ve Demo Ayrımı

## Amaç

Projenin asıl teslimatı, başka bir Java uygulamasının çağırabileceği denetim motorudur. Web arayüzü, Ollama bağlantısı ve JSON rapor kartları bu motoru yerelde göstermek için korunmuş demo parçalarıdır. Kurum entegrasyonu demo sınıflarına bağlanmak zorunda değildir.

## Ana Paketler

```text
core/src/main/java/com.yusuf.audittool
  api/          Diğer projelerin kullanacağı giriş sınıfı ve veri nesneleri
  agent/        Kuruma özel mesajlaşma kodunun uygulayacağı transport arayüzü
  normalize/    JSON alanlarını ortak AgentContext yapısına dönüştürür
  metadata/     Field metadata ve açıklamalarını alanlarla eşleştirir
  checklist/    Checklist girdisini ortak modele dönüştürür
  prompt/       Normalize edilmiş bağlamdan sistem promptu üretir
  model/        Core katmanın ortak veri modelleri

demo/src/main/java/com.yusuf.audittool.demo
  ollama/       Yerel Ollama istemcisi ve model kataloğu
  structured/   Demo için JSON rapor parserı ve validatoru
  web/          Yalnızca demo HTTP controllerları
  model/        Demo request/response modelleri
```

`demo` modülü `audittool-core` modülüne bağlanabilir. Core modülü demo paketini import etmez. Bu tek yönlü bağımlılık, Ollama veya Spring MVC demosu kaldırılsa bile denetim motorunun korunmasını sağlar.

## Kütüphane Akışı

1. Entegrasyon uygulaması issue, metadata, alan açıklamaları ve checklist JSON düğümlerini `AuditInput` ile verir.
2. `AuditEngine`, bu girdiyi `NormalizeService` ile `AgentContext` yapısına dönüştürür.
3. `PromptBuilder`, ortak denetim talimatlarını ve Markdown çıktı başlıklarını kullanarak promptu oluşturur.
4. `AuditEngine`, promptu entegrasyon uygulamasının sağladığı `AgentTransport` nesnesine verir.
5. `AgentTransport`, kurumun gerçek HTTP mesaj sözleşmesini uygular.
6. Modelden gelen metin değiştirilmeden `String` olarak çağıran uygulamaya döner.

```java
AgentTransport transport = (prompt, endpoint) -> {
    // Kurumun request body, kimlik doğrulama ve response okuma sözleşmesi burada uygulanır.
    return companyAiClient.send(prompt, endpoint.getUri(), endpoint.getHeaders());
};

AuditEngine engine = new AuditEngine(transport);
AuditInput input = new AuditInput(payload, metadata, fieldDescriptions, checklist);
AgentEndpoint endpoint = new AgentEndpoint(
        URI.create("https://ai.example.internal/message"),
        Map.of("Authorization", "Bearer ...")
);

String report = engine.analyze(input, endpoint);
```

Endpoint adresi tek başına request/response sözleşmesini açıklamaz. Bu nedenle core kütüphane, body alan adlarını veya response JSON yolunu tahmin etmez. Kurum sözleşmesi öğrenildiğinde yalnızca bir `AgentTransport` implementasyonu eklenir; normalize ve prompt akışı değişmez.

## Prompt Profilleri

- `core/src/main/resources/prompts/core_auditor.md`: Tüm kullanımlar için ortak denetim kuralları.
- `core/src/main/resources/prompts/output_markdown.md`: Kütüphane akışında kullanılan, yalnızca rapor başlıklarını isteyen profil.
- `core/src/main/resources/prompts/output_json.md`: Yerel web demosunun kart görünümü için kullandığı yapılandırılmış profil.

Kütüphane akışı modeli JSON şemasına zorlamaz ve raporu parse etmez. Kurum uygulaması isterse dönen Markdown metnini doğrudan gösterebilir veya kendi sözleşmesine göre sonradan işleyebilir.

## Demo Paketleri

```text
com.yusuf.audittool.demo
  DemoAuditToolApplication.java  Spring Boot demo başlangıcı
  ollama/                        Ollama istemcisi, model kataloğu ve ayarları
  structured/                    JSON rapor parserı ve validatoru
  web/                           Yalnızca demo HTTP controllerları
  model/                         Demo request/response modelleri

demo/src/main/resources
  demo/application-demo.properties
  static/demo/                   Demo HTML, CSS ve JavaScript dosyaları
```

Demo web adresleri de ayırt edici bir ön ek kullanır:

- Arayüz: `/demo/index.html`
- Sağlık: `/demo/api/health`
- Modeller: `/demo/api/models`
- Normalize: `/demo/api/normalize`
- Analiz: `/demo/api/analyze`

Kök adres `/`, kolaylık için `/demo/index.html` adresine yönlendirir.

## Test Ayrımı

Core testleri `core/src/test/java/com/yusuf/audittool` altındaki gerçek paketlerin yanındadır. Demo testleri ise `demo/src/test/java/com/yusuf/audittool/demo` altında Ollama, web ve structured alt klasörlerine ayrılmıştır. `mvn clean test` iki modülü birlikte çalıştırır.

## Maven Modülleri

Kök `pom.xml` yalnızca reactor görevi görür. Kütüphane ve demo ayrı ayrı derlenebilir:

```bash
mvn -pl core clean package
mvn -pl demo -am clean package
```

Demo jarı `demo/target/audittool-demo-0.0.1-SNAPSHOT.jar` olarak üretilir. Başka bir uygulamanın kullanacağı temel artifact ise `core/target/audittool-core-0.0.1-SNAPSHOT.jar` dosyasıdır.
