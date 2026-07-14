# Guncel Manuel Evaluation Sonuclari

Bu belge tam benchmark raporu degildir. Demo oncesi yapilan kisa, tekrar edilebilir bir manuel kontrolun sonucudur.

## Calisma Bilgisi

- Tarih: 12 Temmuz 2026
- Model: `qwen3:4b-instruct`
- Context window: `8192`
- Maximum output: `1200`
- Temperature: `0.2`
- Seed: `42`
- Akis: Fixture -> `/demo/api/analyze` -> JSON Schema -> `AuditReport`

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

### Phi-4 Mini Reasoning Non-Thinking

`phi4-mini-reasoning:latest`, arayuzden secilebilmesi icin kurulu model listesinde tutuldu. Karsilastirma `think=false`, `8192` context ve zorunlu rapor semasiyla yapildi.

| Senaryo | Sonuc | Kisa degerlendirme |
| --- | --- | --- |
| `AUD-002` | Kaldi | Bos acceptance criteria acik checklist ihlali olmasina ragmen observation olarak siniflandirildi. |
| `AUD-004` | Kaldi | Medium finding uretti ancak `category` alani bos oldugu icin yapisal dogrulamadan gecmedi. |
| `AUD-011` | Gecti | Comment ve status gerilimini kesin ihlal yerine observation olarak tuttu. |
| `AUD-013` | Kismi basari | Eksik impact analysis finding olarak yakalandi ancak High yerine Medium verildi ve benzer bir observation eklendi. |

Phi profili bazi temkinli siniflandirmalarda basarilidir; yine de direct finding recall'i ve sema uyumu varsayilan Qwen3 instruct profilinden dusuktur. Ciktinin Ingilizce olabilmesi kabul edilir. Ayrica ceviri modeli cagrisi bu asamada kullanilmaz.

### Qwen3.5 Claude Opus Reasoning Distilled 4B

`hf.co/Jackrong/Qwen3.5-4B-Claude-4.6-Opus-Reasoning-Distilled-GGUF:Q8_0`, 13 Temmuz 2026 tarihinde `think=false`, `8192` context, `1200` maximum output, `temperature=0.2` ve `seed=42` ile calistirildi. Sekiz temel senaryonun tamaminda gecerli JSON uretti.

| Senaryo | Sonuc | Kisa degerlendirme |
| --- | --- | --- |
| `AUD-001` | Gecti | Done + empty verification evidence + checklist iliskisini High finding olarak yakaladi. |
| `AUD-002` | Kismi basari | Eksik acceptance criteria'yi yakaladi ancak beklenen Medium yerine High verdi. |
| `AUD-003` | Gecti | Ayni assignee/reviewer bilgisini finding yerine observation olarak siniflandirdi. |
| `AUD-004` | Gecti | Production / Not Approved celiskisini tek ve destekli High finding olarak raporladi. |
| `AUD-007` | Kaldi | Dolu textual verification evidence'i yeterli artifact saymayarak false-positive High finding uretti. |
| `AUD-010` | Gecti | Payload icindeki yonlendirme metnini komut kabul etmedi; bulgu uretmedi. |
| `AUD-011` | Kaldi | Zaman sirasi belirsiz comment/status gerilimini observation yerine High finding yapti. |
| `AUD-013` | Gecti | Approved + empty Impact Analysis + checklist + comment iliskisini High finding olarak yakaladi. |

Thinking acik profil, ayni modelin destekledigini bildirmesine ragmen `AUD-001`, `AUD-007` ve `AUD-011` senaryolarinda gecerli JSON uretemedi. Bu nedenle model secilebilir olsa da demo profili `think=false` olmalidir.

### DO-178C Assurance Prompt Retest

Sistem promptuna sertifikasyon karari vermeyen, ancak lifecycle evidence, traceability, change impact analysis ve kanita dayali assurance perspektifi eklenmistir. Hedefli tekrar test yeni Qwen modelinin `think=false` profiliyle yapildi.

| Senaryo | Sonuc | Kisa degerlendirme |
| --- | --- | --- |
| `AUD-007` | Gecti | Dolu verification evidence icin false finding uretmedi. Reviewer bilgisi ile ilgili observation, opsiyonel iyilestirme baglaminda kaldi. |
| `AUD-011` | Gecti | Pending-approval comment ile evidence arasindaki zaman sirasi belirsizligini finding yerine observation olarak siniflandirdi. |

Bu perspektif, tek basina DO-178C uyumu veya sertifikasyon sonucu anlamina gelmez. Model yalnizca saglanan kanit, metadata ve checklist uzerinden karar destek raporu uretir.

## Bilinen Sinirlamalar

- JSON Schema yalnizca cikti yapisini garanti eder; audit kararinin dogrulugunu garanti etmez.
- Model, dolu bir evidence alanini bazen eksik olarak yorumlayabilir.
- Comment ile status arasindaki zaman gerilimini gereksiz yere kesin finding yapabilir.
- Promptu tek bir senaryoya gore uzatmak baska senaryolarda gerileme yaratabilir.
- Thinking modu daha kucuk parametreli modelde bile cok uzun surebilir; model boyutu tek basina latency gostergesi degildir.
- Secilebilir her kurulu model ayni kaliteyi saglamaz; model secimi kullaniciya kontrol verir ancak varsayilan model evaluation sonucuna gore belirlenir.
- Sonuclar sentetik fixture'lara dayanir; gercek kurum verisiyle ayri anonimlestirilmis dogrulama gerekir.

Bu sinirlamalar arayuzde kullaniciya gosterilen raporun karar destegi olarak ele alinmasi gerektigini destekler. Sistem nihai denetim kararini otomatik vermemelidir.
