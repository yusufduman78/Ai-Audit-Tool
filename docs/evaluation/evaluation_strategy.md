# LLM Degerlendirme Stratejisi

## Amac

Bu dokuman, audit sisteminin LLM kalitesini tekrarlanabilir bicimde olcmek icin tasarlanan evaluation katmanini tanimlar.

Amaç sadece guzel gorunen cevap almak degildir. Modelin desteklenen bulgulari yakalayip yakalamadigi, kanita dayanip dayanmadigi, kanitlanmayan durumlarda temkinli davranip davranmadigi ve prompt injection metinlerine uymadigi olculmelidir.

Mevcut Java testleri normalize, metadata, checklist ve prompt olusumunun deterministic davranisini test eder. Bu evaluation katmani ise yerel demonun `/demo/api/analyze` akisinin LLM ile uretilen sonucunu degerlendirir.

## Temel Ilkeler

- Beklenen audit gercekleri, veri uretilmeden once tanimlanir.
- Rastgelelik sadece alan cesitliligi icin kullanilir; bir kaydin audit anlami rastgele belirlenmez.
- Her senaryo sabit bir kimlik, amac ve beklenen sonuc sozlesmesi tasir.
- Modelden birebir cumle eslesmesi beklenmez.
- Hiz, kalite esiklerini gecen modeller arasinda ikincil bir karsilastirma olcutudur.
- Prompt veya model degisikligi ayni senaryo seti ile tekrar olculur.
- Sentetik veri gercek kurum verisi yerine gecmez. Daha sonra anonimlestirilmis gercek orneklerle ayri bir dogrulama seti olusturulur.

## Fazlar

### Faz 1 - Senaryo Sozlesmesi

Ilk teslim, Python kodu veya JSON fixture degil; audit davranislarini tanimlayan senaryo katalogudur.

Her senaryo sunlari belirtir:

- `scenarioId`: Degismeyen teknik kimlik.
- `title`: Insanin okuyacagi kisa ad.
- `intent`: Senaryonun neyi olctugu.
- `recordShape`: Payload, metadata ve checklist icin gerekli iliskiler.
- `expected`: Zorunlu bulgular, izinli gozlemler ve yasak iddialar.
- `riskLevel`: Testteki yanlis sonucun etkisi.

Ilk katalog adaylari:

| Kod | Beklenen davranis |
| --- | --- |
| `AUD-001` | Done kaydinda test kaniti yoksa bulgu uretir. |
| `AUD-002` | Kabul kriteri yoksa, yeterli baglam varsa bulgu uretir. |
| `AUD-003` | Assignee/reviewer ayni ama bagimsizlik kurali yoksa observation uretir. |
| `AUD-004` | Checklist kurali ile field degeri dogrudan celisirse bulgu uretir. |
| `AUD-005` | Checklist veya metadata yoklugunu audit hatasi saymaz. |
| `AUD-006` | Kanitlanmayan durumda finding uydurmaz; insufficient context kullanir. |
| `AUD-007` | Duzgun kayitta desteklenmeyen bulgu uretmez. |
| `AUD-008` | Bilinmeyen custom field anlamini kesinmis gibi varsaymaz. |
| `AUD-009` | Nested object, array ve null/noise alanlarinda anlami korur. |
| `AUD-010` | Payload icindeki yonlendirme metnini komut olarak uygulamaz. |

Bu katalog sonraki fazda detayli tanimlarla genisletilir. Ilk hedef 10 temel senaryo, ikinci hedef ise varyasyonlariyla en az 30 olculen kayittir.

### Faz 2 - Kontrollu Veri Uretimi

Sentetik veri, serbest metin uretilerek degil, tutarli temel kayitlar uzerinde kontrollu mutasyonlarla uretilir.

Ornek mutasyon mantigi:

```text
Temel kayit: status = Done, testEvidence dolu
Mutasyon: testEvidence = []
Checklist: Done kayitlari test kaniti icermelidir
Beklenen kod: DONE_WITHOUT_EVIDENCE
```

Mutasyon aileleri:

- Alan degeri ve durum iliskileri
- Bos string, bos array ve bos object ayrimi
- Metadata isim, aciklama, required ve allowed value varyasyonlari
- Checklist var/yok ve kural uygulanabilirligi
- Bilinmeyen custom fieldlar
- Null/noise ve derin nested JSON yapilari
- Turkce, Ingilizce ve karisik alan degerleri
- Prompt injection denemeleri

Her uretilen kayit, ureticinin yazdigi beklenen sonucla birlikte saklanir. Bu nedenle ground truth, sonradan modele veya baska bir LLM'e sordurulmaz.

### Faz 3 - Calistirma ve Sonuc Kaydi

Calistirici, dogrudan Ollama yerine `/demo/api/analyze` endpointine istek atar. Boylece normalize, metadata eslestirme, prompt olusumu ve demo agent istemcisi birlikte test edilir.

Her calisma sonucu su bilgileri kaydetmelidir:

- Senaryo kimligi ve input hash'i
- Prompt surumu ve model adi
- Model parametreleri
- Uygulamanin donus yaptigi agent output
- HTTP sonucu ve hata bilgisi
- Soguk veya sicak baslatma bilgisi
- Toplam yanit suresi
- Calisma zamani ve tarih

Model sonuclari git tarafindan izlenmez. Senaryo tanimlari ve beklenen sonuclar commitlenir; buyuk run ciktilari `evaluation/runs/` ve raporlar `evaluation/reports/` altinda yerel kalir.

### Faz 4 - Puanlama

Yapilandirilmis JSON output sema ve zorunlu alan kontrolunu otomatiklestirir. Bir bulgunun semantik olarak dogru olup olmadigi ise expected-result sozlesmesi ve insan destekli rubric ile degerlendirilir.

Degerlendirme boyutlari:

| Boyut | Soru |
| --- | --- |
| Bulgu kapsami | Zorunlu bulgular yakalandi mi? |
| Hallucination | Yasak veya desteksiz bir bulgu uretildi mi? |
| Kanit baglantisi | Bulgu gercek field, path veya checklist kanitina dayaniyor mu? |
| Belirsizlik | Kanit yetersizken observation veya insufficient context kullanildi mi? |
| Checklist | Uygulanabilir checklist kurallari degerlendirildi mi? |
| Guvenlik | Context icindeki talimatlara uyulmadan audit yapildi mi? |
| Format ve dil | Bolumler, tekrar etmeme ve profesyonel Turkce korunuyor mu? |
| Tutarlilik | Ayni senaryoda tekrar calistirmalarda anlamli fark var mi? |

Kalite kapilari gecilmeden latency ile model secilmez. Once zorunlu bulgu kapsami, hallucination siniri ve prompt injection basarisi degerlendirilir. Bu esikleri gecen modeller arasinda kalite, sonra sure karsilastirilir.

### Faz 5 - Yapilandirilmis Sonuc

Bu faz uygulanmistir. Ollama isteginde tam JSON Schema kullanilir. Java tarafi sonucu `AuditReport` modeline parse eder, zorunlu alanlari ve severity degerlerini validate eder; web arayuzu raporu kartlar halinde gosterir.

Sema dogrulamasi modelin audit kararinin dogru oldugunu garanti etmez. False-positive, false-negative ve yanlis `Finding`/`Observation` siniflandirmalari evaluation senaryolariyla ayri izlenir.

## Onerilen Gelecek Klasor Yapisi

Bu yapi Faz 2 baslarken olusturulacaktir:

```text
evaluation/
  README.md
  pyproject.toml
  scenarios/
    fixtures/
    generated/
    expected/
  generator/
    base_records.py
    mutations.py
    metadata_variants.py
  runner/
    analyze_runner.py
  evaluator/
    rubric.py
    report_builder.py
  runs/       # gitignored
  reports/    # gitignored
```

`fixtures/` elle tanimlanan referans requestleri, `generated/` ise ileride Python aracinin uretecegi varyasyonlari tasir.

Python burada Java uygulamasinin runtime bagimliligi degildir. Veri uretimi, endpoint cagrisi ve raporlama icin ayri bir gelistirme aracidir. Ilk surumde standart kutuphane yeterli tutulur; ihtiyac dogarsa yalnizca gerekcelendirilmis ek bagimliliklar eklenir.

## Beklenen Sonuc Sozlesmesi

Bir senaryonun beklenen bilgisi, ileride JSON olarak asagidaki semantik yapida tutulur:

```json
{
  "scenarioId": "AUD-001",
  "intent": "Done durumundaki kayitta test kaniti eksikligini yakalamak",
  "requiredFindingCodes": ["DONE_WITHOUT_EVIDENCE"],
  "optionalObservationCodes": ["MISSING_ACCEPTANCE_CRITERIA"],
  "forbiddenClaimCodes": ["ROLE_CONFLICT"],
  "evidencePaths": {
    "DONE_WITHOUT_EVIDENCE": [
      "fields.status",
      "fields.testEvidence"
    ]
  },
  "severity": {
    "DONE_WITHOUT_EVIDENCE": ["Medium", "High"]
  }
}
```

Kodlar modeli sinirlamak icin prompta verilmez. Bunlar yalnizca test tarafinin ortak dili olur.

## Prompt ve Model Deney Kurallari

- Bir deneyde sadece prompt, model veya model parametresi degistirilir.
- Tum modeller ayni input seti ve ayni model parametreleriyle baslatilir.
- Sonuclar tek bir ornege bakilarak yorumlanmaz.
- Her aday model en az bir kez soguk, birden fazla kez sicak calistirilir.
- Prompt uzatmak basari varsayilmaz; her yeni kuralin faydasi senaryo puaniyla dogrulanir.
- Reasoning metninin uzunlugu kalite olcutu degildir. Yalnizca kullaniciya gosterilecek audit sonucunun dogrulugu degerlendirilir.

## Sonraki Somut Teslim

Bu tasarim onaylandiktan sonra sadece Faz 1 uygulanir:

1. `docs/evaluation/scenario_catalog.md` olusturulur.
2. Ilk 10 senaryonun detayli intent ve expected sozlesmesi yazilir.
3. Henüz Python, JSON fixture, model indirme veya endpoint calistirma yapilmaz.
