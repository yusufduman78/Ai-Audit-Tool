# Değerlendirme Belgeleri

Bu klasör, LLM çıktısının nasıl test edildiğini, demo verisinin nasıl tasarlandığını ve modellerin bilinen davranışlarını açıklar. Java unit testleri deterministic kodu doğrular; buradaki belgeler ise modelin semantik denetim kalitesini değerlendirir.

## Belge Akışı

```text
Değerlendirme stratejisi
          |
          v
   Senaryo kataloğu
          |
          v
   Demo veri tasarımı
          |
          v
 Fixture + expected sözleşmesi
          |
          v
  Demo veya model çalışması
          |
          v
   Güncel sonuç kaydı
```

## Belgeler

| Belge | Sorumluluk |
| --- | --- |
| [Değerlendirme Stratejisi](evaluation_strategy.md) | Ölçüm boyutları, kalite kapıları ve tekrar edilebilirlik kuralları |
| [Senaryo Kataloğu](scenario_catalog.md) | Her `AUD-*` senaryosunun amacı, beklenen bulgusu ve yasak iddiaları |
| [Demo Veri Tasarımı](demo_data_design.md) | Null, empty, custom field, metadata ve comment dağılımı için veri ilkeleri |
| [Yerel LLM ile Metin Üretimi](local_llm_text_generation.md) | Deterministik fixture yapısı içinde gerçekçi metin üretme sınırları |
| [Demo Akışı](demo_walkthrough.md) | Web arayüzünde sunulacak kısa senaryo sırası |
| [Güncel Model Sonuçları](current_results.md) | Manuel karşılaştırmalar, başarısızlıklar ve bilinen sınırlamalar |

## Veri Klasörleri

| Klasör | İçerik |
| --- | --- |
| `evaluation/scenarios/definitions/` | Senaryo niyeti ve üretim sözleşmesi |
| `evaluation/scenarios/fixtures/` | Demo API'ye tek dosya olarak verilebilen tam request örnekleri |
| `evaluation/scenarios/expected/` | Semantik beklenen sonuçlar |
| `evaluation/demo-inputs/` | Arayüze ayrı ayrı yüklenen issue, metadata, alan açıklaması ve checklist dosyaları |
| `evaluation/local/` | Git dışında tutulan üretim araçları ve çalışma çıktıları |

## Sonuçları Yorumlama

Model çıktısının beklenen cümleyle bire bir eşleşmesi aranmaz. Şunlar değerlendirilir:

- Gerekli bulgunun yakalanması.
- Desteklenmeyen bulgu üretilmemesi.
- Finding, observation ve insufficient-context ayrımının doğru yapılması.
- Somut payload, metadata, checklist veya comment kanıtının kullanılması.
- Prompt injection benzeri bağlam metninin talimat olarak uygulanmaması.
- Çıktı dilinin ve formatının kullanım amacına uygun olması.

Değerlendirme sonucu modelin genel olarak “iyi” veya “kötü” olduğunu tek başına kanıtlamaz. Sonuç, kullanılan model sürümü, prompt sürümü, context penceresi ve generation ayarlarıyla birlikte anlamlıdır.

[Dokümantasyon merkezine dön](../README.md).
