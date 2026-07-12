# Generic AI Audit Tool

Generic AI Audit Tool, Jira benzeri yapilandirilmis JSON kayitlarini normalize eden ve local Ollama modeliyle kanita dayali denetim raporu ureten bir Spring Boot uygulamasidir.

Ilk kullanim alani Jira issue verileridir. Normalize katmani belirli bir issue tipine bagli degildir; nested object, array, custom field, metadata, checklist ve comment bilgilerini ortak bir agent context yapisina donusturur.

## Neler Yapar?

- JSON payload icindeki aktif, bos, null ve noise alanlari ayirir.
- Custom field metadata bilgisini teknik field ID'leriyle eslestirir.
- Checklist maddelerini ve Jira comment gecmisini ayri baglam olarak tasir.
- Local LLM icin denetlenebilir bir prompt olusturur.
- Ollama JSON Schema destegiyle yapilandirilmis audit raporu ister.
- Kurulu Ollama modellerini arayuzden secmeye ve desteklenen modellerde thinking modunu analiz bazinda acmaya izin verir.
- Bulgulari, gozlemleri ve son oneriyi web arayuzunde gosterir.
- Model raporunu parse edip zorunlu alanlar ve severity degerleri bakimindan dogrular.

## Gereksinimler

- Java 21
- Maven 3.9+
- Ollama 0.31 veya daha yeni bir surum
- Varsayilan model icin yaklasik 3 GB bos disk alani

Varsayilan ve mevcut evaluation setinde daha basarili model:

```bash
ollama pull qwen3:4b-instruct
```

Ollama masaustu uygulamasi calisiyorsa API genellikle `http://localhost:11434` adresinde zaten aciktir. `ollama serve` komutunda "address already in use" gorulmesi de servisin acik oldugunu gosterebilir.

## Calistirma

Proje kok dizininde:

```bash
mvn spring-boot:run
```

Ardindan web arayuzunu ac:

```text
http://localhost:8080
```

Arayuzde issue, metadata ve checklist JSON dosyalari ayri ayri yuklenebilir. Hazir uc dosyali ornekler `evaluation/demo-inputs/` altindadir. `evaluation/scenarios/fixtures/` altindaki tam `AnalyzeRequest` fixture dosyalari da yalnizca `Issue JSON` alanina yuklenebilir; arayuz bunlarin icindeki metadata ve checklist bilgisini otomatik kullanir.

Model listesi Ollama'dan canli olarak alinir. Secim sadece o analiz istegi icin gecerlidir ve `application.properties` dosyasindaki varsayilani degistirmez. Thinking yetenegi Ollama tarafindan bildirilmeyen modellerde arayuz bu secenegi kapali tutar.

Uygulamayi durdurmak icin uygulamanin calistigi terminalde `Control+C` kullanilir.

## API

Saglik kontrolu:

```bash
curl http://localhost:8080/api/health
```

Kurulu Ollama modellerini ve varsayilan profili gormek:

```bash
curl http://localhost:8080/api/models
```

Normalize edilen baglami gormek:

```bash
curl -X POST http://localhost:8080/api/normalize \
  -H "Content-Type: application/json" \
  --data-binary @evaluation/scenarios/fixtures/aud-002-missing-acceptance-criteria.json
```

LLM analizini calistirmak:

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  --data-binary @evaluation/scenarios/fixtures/aud-002-missing-acceptance-criteria.json
```

Temel request sozlesmesi:

```json
{
  "payload": { "key": "REQ-001", "fields": {} },
  "metadata": {},
  "checklist": [],
  "agentOptions": {
    "model": "qwen3:4b-instruct",
    "thinkingEnabled": false
  }
}
```

`payload` zorunludur. `metadata`, `checklist` ve `agentOptions` opsiyoneldir. `agentOptions` verilmezse uygulama konfigurasyonundaki varsayilan model ve thinking ayari kullanilir.

## Konfigurasyon

Varsayilan ayarlar `src/main/resources/application.properties` icindedir:

```properties
server.port=8080
ollama.url=http://localhost:11434
ollama.model=qwen3:4b-instruct
ollama.context-window=8192
ollama.max-output-tokens=1200
ollama.temperature=0.2
ollama.seed=42
ollama.thinking-enabled=false
ollama.top-p=0.8
ollama.top-k=20
```

Ayarlar environment variable ile override edilebilir. Ornekler `.env.example` dosyasinda bulunur. Spring Boot `.env` dosyasini kendiliginden yuklemez; degerler terminalden export edilmeli veya IDE run configuration icine eklenmelidir.

```bash
export OLLAMA_MODEL=qwen3.5:9b
export OLLAMA_THINKING_ENABLED=false
mvn spring-boot:run
```

Sabit `seed` ve dusuk `temperature`, ayni model ve prompt ile tekrar calistirmalarda sonucu daha tutarli hale getirir. Bu ayarlar semantik dogrulugu garanti etmez.

### Model ve Thinking Secimi

Web arayuzu `/api/models` uzerinden makinede kurulu modelleri listeler. `qwen3:4b-instruct` mevcut evaluation setinde finding kapsami daha iyi oldugu icin varsayilandir. `phi4-mini-reasoning:latest` secilebilir durumda tutulur; Ingilizce veya Turkce cikti uretebilir ancak testlerde bazi acik ihlalleri observation olarak siniflandirmistir.

Bu asamada raporu Turkceye cevirmek icin ikinci bir model cagrisi yapilmaz. Ikinci cagri gecikmeyi ve anlamsal kayip riskini artirir. Modelin urettigi dil oldugu gibi gosterilir.

### Qwen3.5 Thinking Modu

`qwen3.5:4b` ve `qwen3.5:9b` ayni model checkpointi icinde thinking ve non-thinking modlarini destekler. Uygulamada mod environment variable ile secilebilir:

```bash
export OLLAMA_MODEL=qwen3.5:4b
export OLLAMA_THINKING_ENABLED=true
export OLLAMA_CONTEXT_WINDOW=16384
export OLLAMA_MAX_OUTPUT_TOKENS=8000
mvn spring-boot:run
```

Ollama, Qwen3.5 thinking ile runtime JSON Schema birlikte kullanildiginda final JSON'u gizli thinking alanina yazabildigi icin uygulama thinking modunda runtime schema kisitini gondermez. Final response yine `AuditReportParser` ve `AuditReportValidator` tarafindan kontrol edilir; reasoning metni API cevabina veya arayuze tasinmaz.

Yerel testte `qwen3.5:4b` thinking profili tek bir audit kaydinda 420 saniye icinde final rapora ulasamadi. Bu nedenle thinking modu desteklenir ancak varsayilan demo profili degildir.

## Test

```bash
mvn clean test
```

Java testleri normalize, metadata, checklist, comment extraction, prompt olusturma, thinking/non-thinking agent istemcisi ve controller akisini kapsar. LLM davranisi model bagimli oldugu icin fixture sonuclari ayrica evaluation belgeleriyle incelenir.

## Proje Yapisi

```text
src/main/java/                 Spring Boot uygulama kodu
src/main/resources/prompts/   Sistem promptu
src/main/resources/static/    Web arayuzu
src/test/java/                 Java testleri
evaluation/scenarios/         Fixture ve expected-result sozlesmeleri
evaluation/demo-inputs/       Ayri issue, metadata ve checklist demo dosyalari
docs/architecture/            Mimari kararlar ve UML notlari
docs/evaluation/              Demo ve model degerlendirme belgeleri
```

## Demo ve Degerlendirme

- Demo sirasi: `docs/evaluation/demo_walkthrough.md`
- Senaryo katalogu: `docs/evaluation/scenario_catalog.md`
- Guncel manuel sonuc ozeti: `docs/evaluation/current_results.md`
- Ayrintili evaluation yaklasimi: `docs/evaluation/evaluation_strategy.md`

Model sonucu bir karar destegidir; nihai denetim karari degildir. Evaluation seti, false-positive ve false-negative davranislarini gorunur kilmak icin projede tutulur. Mevcut bilinen sinirlamalar guncel sonuc belgesinde acikca kaydedilmistir.
