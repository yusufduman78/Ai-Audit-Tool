# Ayrı Demo Girdileri

Bu klasördeki her senaryo web arayüzüne dört ayrı JSON dosyası olarak yüklenebilir:

1. `issue.json` -> Issue JSON
2. `metadata.json` -> Metadata JSON
3. `field-descriptions.json` -> Türkçe Alan Açıklamaları JSON
4. `checklist.json` -> Checklist JSON

Dosyalar, birleştirilmiş `AnalyzeRequest` fixture'larından farklı olarak kullanıcının verileri ayrı kaynaklardan sağlamasını taklit eder. Türkçe alan açıklamaları opsiyoneldir; verilmezse metadata içindeki açıklamalar kullanılmaya devam eder.

Önerilen başlangıç senaryosu `aud-002-missing-acceptance-criteria/` klasörüdür. Sunum sırası ve beklenen davranışlar [Demo Akışı](../../docs/evaluation/demo_walkthrough.md), tam fixture kullanımı ise [Evaluation Fixture'ları](../README.md) belgesinde açıklanır.
