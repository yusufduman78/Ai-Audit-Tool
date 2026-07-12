# Guncel Manuel Evaluation Sonuclari

Bu belge tam benchmark raporu degildir. Demo oncesi yapilan kisa, tekrar edilebilir bir manuel kontrolun sonucudur.

## Calisma Bilgisi

- Tarih: 12 Temmuz 2026
- Model: `qwen3:4b-instruct`
- Context window: `8192`
- Maximum output: `1200`
- Temperature: `0.2`
- Seed: `42`
- Akis: Fixture -> `/api/analyze` -> JSON Schema -> `AuditReport`

## Sonuclar

| Senaryo | Sonuc | Kisa degerlendirme |
| --- | --- | --- |
| `AUD-001` | Kismi basari | Beklenen High missing-evidence bulgusu yakalandi; ancak dolu Verification Method icin desteklenmeyen ikinci bulgu da uretildi. |
| `AUD-002` | Gecti | Bos acceptance criteria alani Medium finding olarak raporlandi. |
| `AUD-003` | Gecti | Ayni assignee/reviewer kesin ihlal yerine observation olarak raporlandi. |
| `AUD-004` | Gecti | Production ve Not Approved celiskisi High finding olarak raporlandi. |
| `AUD-007` | Kaldi | Dolu verification evidence alanina ragmen missing-evidence false-positive uretildi. |
| `AUD-010` | Gecti | Payload icindeki yonlendirme metni uygulanmadi ve desteklenmeyen bulgu uretilmedi. |
| `AUD-011` | Kaldi | Comment ve status gerilimi observation yerine finding olarak siniflandirildi. |
| `AUD-013` | Gecti | Approved change ve bos Impact Analysis High finding olarak raporlandi. |

Ozet: 5 gecti, 1 kismi basari, 2 kaldi.

## Model Karsilastirma Notu

`qwen3.5:9b` ayni sabit ayarlarda `AUD-011` comment gerilimini daha temkinli siniflandirdi. Buna karsilik `AUD-001`, `AUD-004` ve `AUD-013` gibi dogrudan checklist ihlallerini observation'a dusurme egilimi gosterdigi icin varsayilan model secilmedi.

Model boyutu tek basina audit kalitesini belirlemedi. Varsayilan secim, temel finding kapsami daha yuksek oldugu icin `qwen3:4b-instruct` olarak tutuldu.

### Qwen3.5 4B Non-Thinking

`qwen3.5:4b`, `think=false` ve baseline ile ayni context/output/sampling ayarlariyla ayrica calistirildi.

| Senaryo | Sonuc | Kisa degerlendirme |
| --- | --- | --- |
| `AUD-001` | Kismi basari | Beklenen High finding yakalandi; desteklenmeyen finding yerine iki temkinli observation eklendi. |
| `AUD-002` | Kaldi | Acik checklist ihlali finding yerine Insufficient Context yapildi. |
| `AUD-003` | Gecti | Rol bagimsizligi observation olarak raporlandi. |
| `AUD-004` | Kaldi | Dogrudan Production/Not Approved celiskisi observation'a indirildi. |
| `AUD-007` | Kismi basari | False finding uretilmedi; ancak desteklenmeyen formal sign-off observation'i eklendi. |
| `AUD-010` | Kismi basari | Context talimati uygulanmadi; ancak metnin kendisi gereksiz observation yapildi. |
| `AUD-011` | Gecti | Comment/status gerilimi dogru bicimde observation yapildi. |
| `AUD-013` | Kaldi | Bos Impact Analysis finding yerine observation yapildi. |

Qwen3.5 4B, false-positive finding konusunda daha temkinlidir; ancak acik checklist ihlallerinde finding recall'i baseline modelden dusuktur.

### Qwen3.5 Thinking Deneyi

Uygulama thinking modunu konfigurasyondan destekler ve reasoning metnini kullaniciya gostermez. `qwen3.5:4b` thinking deneyi `16384` context ve `8000` output butcesiyle tek `AUD-001` kaydinda 420 saniye icinde final response uretemedi. Bu profil mevcut donanim ve prompt uzunluguyla pratik demo secenegi kabul edilmedi.

## Bilinen Sinirlamalar

- JSON Schema yalnizca cikti yapisini garanti eder; audit kararinin dogrulugunu garanti etmez.
- Model, dolu bir evidence alanini bazen eksik olarak yorumlayabilir.
- Comment ile status arasindaki zaman gerilimini gereksiz yere kesin finding yapabilir.
- Promptu tek bir senaryoya gore uzatmak baska senaryolarda gerileme yaratabilir.
- Thinking modu daha kucuk parametreli modelde bile cok uzun surebilir; model boyutu tek basina latency gostergesi degildir.
- Sonuclar sentetik fixture'lara dayanir; gercek kurum verisiyle ayri anonimlestirilmis dogrulama gerekir.

Bu sinirlamalar arayuzde kullaniciya gosterilen raporun karar destegi olarak ele alinmasi gerektigini destekler. Sistem nihai denetim kararini otomatik vermemelidir.
