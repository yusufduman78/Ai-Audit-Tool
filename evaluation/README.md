# Evaluation Fixture'ları

Bu klasör, LLM davranışını aynı girdiler ve aynı beklenen sonuç sözleşmeleriyle karşılaştırmak için kullanılan test verilerini içerir. Fixture'lar production uygulamasının runtime bağımlılığı değildir.

Değerlendirme yönteminin açıklaması için [Değerlendirme Belgeleri](../docs/evaluation/README.md) dizininden başlayın.

## Klasörler

| Klasör | İçerik |
| --- | --- |
| `scenarios/fixtures/` | Demo API'ye gönderilebilen tam request JSON dosyaları |
| `scenarios/definitions/` | Büyük veya generator ile üretilecek senaryoların deterministic tanımları |
| `scenarios/expected/` | Modelin görmediği semantik beklenen sonuç sözleşmeleri |
| `demo-inputs/` | Web arayüzüne ayrı ayrı yüklenen issue, metadata, field descriptions ve checklist dosyaları |
| `local/` | Git dışında tutulan generator, model run ve geçici çıktı dosyaları |

## Tam Fixture ile API Testi

Demo çalışırken normalize sonucunu görmek için:

```bash
curl -X POST http://localhost:8080/demo/api/normalize \
  -H "Content-Type: application/json" \
  --data-binary @evaluation/scenarios/fixtures/aud-001-done-without-evidence.json
```

Model analizini çalıştırmak için aynı dosyayı `/demo/api/analyze` endpointine gönderin:

```bash
curl -X POST http://localhost:8080/demo/api/analyze \
  -H "Content-Type: application/json" \
  --data-binary @evaluation/scenarios/fixtures/aud-001-done-without-evidence.json
```

## Ayrı Dosyalarla Arayüz Testi

`demo-inputs/` altındaki bir senaryo klasöründen şu dört dosya seçilir:

1. `issue.json`
2. `metadata.json`
3. `field-descriptions.json`
4. `checklist.json`

Arayüz bu dosyaları tarayıcıda tek demo request gövdesine birleştirir. Backend'e dört ayrı HTTP isteği gönderilmez.

Beklenen sonuçlar bire bir metin eşleşmesi değildir. Finding/observation sınıflandırması, kullanılan kanıt ve desteklenmeyen iddialar [Senaryo Kataloğu](../docs/evaluation/scenario_catalog.md) ile değerlendirilir. Bilinen model sonuçları [Güncel Model Sonuçları](../docs/evaluation/current_results.md) belgesindedir.
