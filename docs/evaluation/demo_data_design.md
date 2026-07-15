# Demo Veri Tasarimi

> Senaryoların audit anlamı [Senaryo Kataloğu](scenario_catalog.md) tarafından belirlenir. Local LLM yalnızca kontrollü metin alanlarını üretir; sınırlar [Yerel LLM ile Sentetik Metin Üretimi](local_llm_text_generation.md) belgesindedir.

## Amac

Bu dokuman, evaluation setinde kullanilacak sentetik issue verilerinin gercek Jira kullanimina yakin olmasi icin kurallari tanimlar. Veri, kurum bilgisi veya gercek kisi verisi icermeden; field yogunlugu, custom field cesitliligi, comment gecmisi ve requirement change baglami bakimindan gercekci olmalidir.

Bu bir Jira export formati sozlesmesi degildir. Uretici, bu tasarimdaki mantigi `AnalyzeRequest`in `payload`, `metadata` ve `checklist` alanlarina donusturecektir.

## Payload Profilleri

### Kucuk Profil

Normalize ve prompt testleri icin kullanilir.

- 10 ila 30 field
- 5 ila 10 aktif field
- Bir veya iki empty field
- Az sayida null ve noise alan
- Sifir ila iki comment

### Tipik Kurum Profili

Gercekci demo ve model degerlendirmesi icin ana profildir.

- Yaklasik 2.000 toplam field
- 30 ila 40 aktif veya empty field
- Kalan fieldlarin buyuk cogunlugu `null`
- 5 ila 15 metadata eslesmesi
- 0 ila 10 comment
- Jira standart alanlari ile custom fieldlar birlikte

Bu profil, kurumda tanimli ancak ilgili issue tipinde kullanilmayan global fieldlarin JSON'a `null` olarak gelmesi durumunu temsil eder.

### Yogun Comment Profili

Comment baglaminin davranisini olcmek icin kullanilir.

- 10 ila 30 comment
- Gerekce, test sonucu, onay, soru ve alakasiz bilgi karisimi
- Bazi comment'lerde author ve tarih eksikligi
- `FULL`, `PARTIAL` ve `UNKNOWN` coverage varyasyonlari

Mevcut MVP comment extractor tum kullanilabilir comment'leri tasir. Gercek JSON goruldugunde comment limitine gerek olup olmadigi ayri degerlendirilir.

## Field Durumlari

Her field asagidaki durumlardan biriyle uretilir:

| Durum | JSON degeri | Normalize beklentisi |
| --- | --- | --- |
| Active | Metin, sayi, boolean veya anlamli object | `activeFields` |
| Empty string | `""` | `emptyFields` / `EMPTY_STRING` |
| Empty array | `[]` | `emptyFields` / `EMPTY_ARRAY` |
| Empty object | `{}` | `emptyFields` / `EMPTY_OBJECT` |
| Unused | `null` | Prompt disi, yalnizca statistics |
| Noise | `self`, URL, avatar, schema vb. | Prompt disi |

`null` ile empty degerler ayni senaryoda birlikte bulunmalidir. Modelin bir fieldin kullanilmadigini, ancak var olan bos bir fieldin baglama gore anlamli olabilecegini ayirmasi beklenir.

## Standart Jira Alanlari

Temel kayitlar asagidaki alanlardan uygun olanlari kullanir:

```text
key
summary
description
status
priority
issuetype
assignee
reporter
reviewer
labels
resolution
components
fixVersions
comment
```

Bu alanlarin hepsi her kayitta bulunmaz. Bir alani her kayda eklemek yerine, issue type ve senaryo amacina gore aktif, empty veya null durumunda uretmek gerekir.

## Domain Custom Field Aileleri

Custom field ID'leri sentetik tutulur. Anlam, yalnizca metadata `name` ve `description` ile verilir; agent teknik ID uzerinden anlam uydurmamalidir.

### Requirement ve Traceability

```text
customfield_20001 - Requirement Type
customfield_20002 - Parent Requirement
customfield_20003 - System Requirement Reference
customfield_20004 - Requirement Traceability
customfield_20005 - Affected Requirements
customfield_20006 - Verification Method
customfield_20007 - Verification Evidence
```

### Requirement Change ve Konfigurasyon

```text
customfield_20101 - Change Request Type
customfield_20102 - Change Rationale
customfield_20103 - Baseline
customfield_20104 - Affected Configuration Items
customfield_20105 - Impact Analysis
customfield_20106 - Verification Impact
customfield_20107 - Change Approval
customfield_20108 - Change Control Decision
customfield_20109 - Cause of Change
```

### Safety, Security ve Operasyon

```text
customfield_20201 - Security Classification
customfield_20202 - Criticality Level
customfield_20203 - Safety Impact
customfield_20204 - Cybersecurity Assessment
customfield_20205 - Operational Environment
customfield_20206 - Integration Environment
customfield_20207 - Supplier Impact
```

Bu alanlar savunma ve sistem muhendisligi baglamina yakin, ancak kurum veya urun bilgisi icermeyen genel isimlerdir. Tum field aileleri her kayitta bulunmaz.

## Metadata Kurallari

Her senaryoda metadata zorunlu degildir. Varyasyonlar bilincli olarak uretilir:

- Known custom field: ID, name ve description gelir.
- Constraint field: allowed values ve gerekirse required bilgisi gelir.
- Unknown custom field: Yalnizca `customfield_XXXXX` key'i gelir.
- Partially described field: Name gelir, description gelmez.
- Missing metadata: Field degeri gelir, metadata hic gelmez.

Metadata eslestigi zaman agent field anlamini kullanabilir. Eslestigi kanitlanmayan bir custom field icin kesin is kuralina dayali finding beklenmez.

## Comment Kurallari

Comment'ler aktif field degildir; `CommentContext`te ayri tasinir. Her comment asagidaki tiplerden biri olabilir:

- Test durumu veya test sonucu
- Onay veya red gerekcesi
- Requirement change etkisi
- Aciklama talebi veya belirsizlik
- Teknik karar veya risk notu
- Alakasiz sohbet veya durum bildirimi
- Prompt injection benzeri metin

Comment, structured evidence'i destekleyebilir veya onunla celisebilir. Ornegin "test tamamlandi" yorumu, `Verification Evidence` bosken tek basina kesin dogrulama kaniti sayilmaz.

## Requirement Change Kayitlari

Requirement change senaryolari ayri bir record ailesi olarak uretilir. Tipik iliskiler:

```text
Change status = Approved
Change Rationale = active
Affected Requirements = active veya empty
Impact Analysis = active veya empty
Baseline = active veya empty
Change Approval = active veya empty
Verification Impact = active veya empty
Cause of Change = metadata allowed values icinden secilen active deger
```

Ornek audit durumlari:

- Onaylanmis change kaydinda impact analysis bos.
- Baseline belirtilmeden requirement change onaylanmis.
- Affected requirement listesi ile comment'teki etki ifadesi celisiyor.
- Change approval mevcut, fakat verification impact belirsiz.

Bu bulgular ancak metadata veya checklist ile anlam destekleniyorsa zorunlu finding olarak etiketlenir.

## Uretim Kurallari

- Her kayit bir `scenarioId` ile baglantili olur.
- Rastgelelik sabit seed ile tekrarlanabilir olmali.
- Rastgelelik sadece isim, tarih, ID ve ikincil field dagilimi icin kullanilir.
- Finding gerektiren iliskiler deterministik olarak kurulur.
- Duzgun kayitlarda finding yaratacak celiski bulunmaz.
- Null field sayisi, aktif field sayisini ezici bicimde gecmelidir.
- URL ve avatar gibi noise alanlar aktif field listesine girmemelidir.
- Comment body'leri sentetik, anonim ve prompt injection testleri disinda komut icermeyen metinlerden olusur.

## Mevcut Fixture Seti

İlk fixture ailesi aşağıdaki davranışları kapsayacak şekilde oluşturulmuştur:

| Fixture | Profil | Ana amac |
| --- | --- | --- |
| `AUD-001` | Küçük | Done fakat verification evidence boş |
| `AUD-007` | Küçük | Tam ve tutarlı requirement |
| `AUD-013` | Küçük | Onaylı change, impact analysis boş |
| `AUD-011` | Küçük | Comment ile structured field arasında zaman gerilimi |
| `AUD-015` | Tipik kurum | Yaklaşık 2.000 field, çoğunluğu null |

Bu fixture'lar tek başına model benchmarkı değildir; normalize davranışı ve semantik expected sözleşmesi birlikte değerlendirilir. Gerçek dosyalar `evaluation/scenarios/`, arayüzde ayrı yüklenebilen paketler `evaluation/demo-inputs/` altındadır.

## Bakım Kuralı

Yeni bir fixture eklenmeden önce audit anlamı [Senaryo Kataloğu](scenario_catalog.md) içinde tanımlanmalıdır. Ardından fixture ve expected sözleşmesi birlikte hazırlanır. Local LLM yalnızca doğal dil alanlarını zenginleştirir; hangi alanın null, empty veya active olduğunu ve beklenen audit sonucunu değiştiremez.
