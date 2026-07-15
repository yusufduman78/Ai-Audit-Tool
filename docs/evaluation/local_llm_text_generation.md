# Local LLM ile Sentetik Metin Uretimi

> Bu belge [Demo Veri Tasarımı](demo_data_design.md) içindeki kontrollü fixture yaklaşımının yalnızca doğal dil üretimi bölümünü açıklar. Ground truth ve beklenen sonuçlar [Senaryo Kataloğu](scenario_catalog.md) tarafından belirlenir.

## Amac

Evaluation fixture'larinin field degerleri ve comment'leri, tamamen sabit placeholder metinlerden daha gercekci olmalidir. Local LLM bu noktada requirement aciklamasi, change rationale, verification evidence ve comment body gibi dogal dil metinlerini uretmek icin kullanilir.

LLM, audit sonucunun ground truth'unu belirlemez. Senaryo tanimi once gelir; LLM sadece bu tanimin izin verdigi metni yazar.

## Sorumluluk Siniri

### Deterministik Generator Belirler

- Scenario ID ve seed
- Field adlari, custom field ID'leri ve metadata
- Hangi field'in active, empty, null veya noise oldugu
- Allowed value secimi
- Checklist maddeleri
- Expected finding, observation ve forbidden claim kodlari
- Comment coverage ve comment sayisi
- Tarih, kimlik ve toplu field dagilimi
- Dogrulugu aynen korunmasi gereken summary ve verification evidence gibi olgusal metinler

### Local LLM Uretir

- Change rationale
- Impact analysis metni, yalnizca senaryo bunun dolu olmasini istiyorsa
- Reviewer, test engineer veya configuration manager comment body'leri
- Birbirini tekrar etmeyen, iddiasiz kisa destek baglami degerleri

LLM, `null` veya empty olmasi gereken bir field icin metin uretmez. Expected result dosyasini, checklisti veya allowed value listesini degistiremez.

## Uretim Akisi

```text
Scenario definition
        ↓
Deterministik field plan
        ↓
Text generation request
        ↓
Local LLM JSON output
        ↓
Validation
        ↓
Snapshot fixture JSON
        ↓
Expected-result contract ile birlikte commit
```

Her fixture olusturulurken LLM yeniden cagrilmaz. LLM ciktisi bir kez uretilir, dogrulanir ve snapshot JSON olarak saklanir. Benchmark calismalari bu sabit snapshot'lari kullanir.

## Text Generation Request

Python generator, modele tum raw issue JSON'u vermek yerine yalnizca gerekli text brief'ini verir.

Ornek brief:

```text
Scenario: approved requirement change
Language: English
Field intent: Change Rationale
Known facts:
- Cause of Change is Safety Concern.
- Baseline is 3.2.
- Impact Analysis must remain empty.
- Verification Impact is already defined.
Write one concise, professional rationale.
Do not invent people, organizations, ticket IDs, attachments, dates, approvals, or field values.
Return JSON with one field: text.
```

Comment uretiminde author role ve senaryo durumu da brief'e eklenir:

```text
Role: reviewer
Known facts: verification evidence exists, approval is pending
Write one concise comment.
Do not claim that approval is complete.
```

## JSON Cikti Sozlesmesi

Ilk surumde LLM'den kucuk JSON objeleri istenir:

```json
{
  "text": "The timeout update addresses the identified safety concern while preserving the approved baseline constraints."
}
```

Birden fazla metin gerekiyorsa alan adlari sabit tutulur:

```json
{
  "summary": "Authentication timeout requirement update",
  "changeRationale": "...",
  "comments": [
    {
      "role": "reviewer",
      "body": "..."
    }
  ]
}
```

LLM output JSON parse edilemezse generator fallback template kullanir veya o fixture uretimini hata olarak bildirir. Parse edilemeyen serbest metin sessizce kabul edilmez.

## Validation Kurallari

LLM ciktisi fixture'a yazilmadan once asagidaki kontrollerden gecer:

- Bos veya asiri uzun metin reddedilir.
- Beklenmeyen URL, e-posta, kisi adi veya kurum adi reddedilir.
- Senaryoda verilmeyen ticket ID, attachment, tarih veya approval iddiasi reddedilir.
- Empty veya null planlanmis field icin text uretilmez.
- Metin, scenario'nun zorunlu gercekleriyle celisirse reddedilir.
- Prompt injection benzeri ifade normal fixture'larda reddedilir.
- Injection senaryolarinda bu ifade yalnizca ozel olarak istenirse uretilir.

Ilk surumde validation kural tabanli olur. Baska bir LLM ile kalite puanlama veya otomatik yeniden yazma MVP kapsaminda degildir.

## Dil ve Ton

Fixture dili scenario taniminda acikca belirtilir. Ilk set icin teknik field degerleri ve comment'ler Ingilizce olabilir; audit agent output'u system prompt geregi Turkce kalir.

Varsayilan ton:

- Kisa ve profesyonel
- Teknik ama urun veya kurum ismi icermeyen
- Bir comment icin bir veya iki cumle
- Rationale veya evidence icin iki ila dort cumle

## Model ve Tekrarlanabilirlik

`gemma3:4b`, ilk local text authoring modeli olarak indirildi ve tek cumlelik JSON output smoke testini gecti. `qwen3:4b-instruct` audit sonucu karsilastirmalarindaki mevcut baseline olarak kalir; fixture metni yazma goreviyle ayni rolu ustlenmek zorunda degildir. Generator model adini, generation parametrelerini ve scenario seed'ini run metadata icinde saklar.

LLM uretimi tam anlamiyla deterministik kabul edilmez. Bu nedenle tekrar uretmek yerine kabul edilen ciktinin snapshot'i commitlenir. Yeni metin varyasyonlari mevcut fixture'i degistirmek yerine yeni fixture veya yeni revision olarak eklenir.

## Guvenlik ve Gizlilik

- Sadece sentetik brief ve sentetik field degerleri local modele gonderilir.
- Gercek Jira comment'i, kurum adi, kullanici adi veya gizli requirement metni kullanilmaz.
- Model ciktisi terminal loglarina tam metin olarak yazilmaz.
- URL veya attachment linki audit kaniti yerine uretilmez.

## Mevcut Uygulama Sınırı

Yerel Python araçlarının ilk uygulaması `AUD-015` large payload tanımına odaklanır. Summary ve verification evidence tanımdan deterministic olarak gelir. LLM yalnızca şu alanlara metin sağlar:

```text
iki comment body
ek aktif custom field degerleri
```

Araçlar `evaluation/local/` altında Git dışında tutulur. Kabul edilen fixture metni tekrar üretim sırasında değiştirilmez; yeni varyasyon gerekiyorsa ayrı fixture veya açık bir revision oluşturulur. Requirement change ve comment örnekleri için `AUD-013` ile `AUD-011` sabit referans olarak kullanılır.
