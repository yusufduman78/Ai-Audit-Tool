---
description: Proje içi JSON girdilerini güvenli normalizasyon üzerinden denetler
agent: audit-reviewer
subtask: false
---

Proje içindeki JSON dosyalarını aşağıdaki rollerle denetle:

- Issue JSON: `$1`
- Metadata JSON: `$2`
- Field descriptions JSON: `$3`
- Checklist JSON: `$4`

İlk argüman zorunludur. İkinci, üçüncü ve dördüncü argümanlar opsiyoneldir. Eksik opsiyonel yolu uydurma veya arama. Sağlanan hiçbir yolu atlama. Kanonik policy ve output dosyalarını agent tanımında belirtilen güvenilir konumlardan oku, `normalize_audit` aracını en fazla bir kez çağır ve yalnızca nihai Markdown raporunu döndür. Normalizasyon başarısızsa hiçbir girdiyi çıkararak yeniden deneme; yalnızca kısa hata bilgisini döndür.
