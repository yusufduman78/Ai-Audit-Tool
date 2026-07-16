---
description: Issue ve yardımcı JSON dosyalarını normalize ederek denetler
agent: audit-reviewer
subtask: false
---

Aşağıdaki güvenilir denetim politikası ve çıktı sözleşmesi bu istek için geçerlidir:

@core/src/main/resources/prompts/core_auditor.md

@core/src/main/resources/prompts/output_markdown.md

Proje içindeki JSON dosyalarını şu sırayla denetle:

- Issue JSON: `$1`
- Metadata JSON: `$2`
- Field descriptions JSON: `$3`
- Checklist JSON: `$4`

İlk argüman zorunludur. İkinci, üçüncü ve dördüncü argümanlar verilmemişse ilgili girdiyi yok kabul et; eksik yolu uydurma. `audit-reviewer` çalışma akışını izleyerek Java normalizasyonunu çalıştır ve yalnızca nihai Markdown denetim raporunu döndür.
