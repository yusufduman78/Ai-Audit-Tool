# Generic AI Audit Tool

JSON tabanli is verisini normalize edip local LLM destegiyle audit analizi yapmayi hedefleyen Spring Boot projesi.

Ilk hedef Jira issue verileri olsa da core normalize yapisi farkli JSON payload'larla da calisabilecek sekilde tasarlanir.

## Gereksinimler

- Java 21
- Maven 3.9+

## Calistirma

```bash
mvn spring-boot:run
```

Uygulama varsayilan olarak `8080` portunda calisir.

```http
GET http://localhost:8080/api/health
```

Beklenen cevap:

```json
{
  "status": "OK"
}
```

## Test

```bash
mvn test
```

## Proje Yapisi

```text
docs/
  architecture/  yerel mimari plan ve calisma notlari
  diagrams/      UML ve diyagram dosyalari
src/
  main/          uygulama kaynak kodlari
  test/          test kodlari
```

## Konfigurasyon

Temel ayarlar `src/main/resources/application.properties` icindedir.

```properties
server.port=8080
ollama.url=http://localhost:11434
ollama.model=qwen3:4b
prompt.template-path=prompts/core_auditor.md
```
