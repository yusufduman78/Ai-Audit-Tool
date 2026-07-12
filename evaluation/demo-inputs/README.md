# Ayrılmış Demo Girdileri

Bu klasördeki her senaryo web arayüzünde dört ayrı dosya olarak kullanılabilir:

1. `issue.json` -> Issue JSON
2. `metadata.json` -> Metadata JSON
3. `field-descriptions.json` -> Türkçe Alan Açıklamaları JSON
4. `checklist.json` -> Checklist JSON

Dosyalar, birleştirilmiş `AnalyzeRequest` fixture'larından farklı olarak gerçek kullanıcının ayrı kaynaklardan veri yüklemesini taklit eder. Türkçe alan açıklamaları opsiyoneldir; verilmezse metadata içindeki description alanları kullanılmaya devam eder.

Önerilen başlangıç senaryosu `aud-002-missing-acceptance-criteria` klasörüdür.
