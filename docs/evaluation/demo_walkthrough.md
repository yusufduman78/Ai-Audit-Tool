# Demo Akisi

Bu akis, web arayuzunde projenin temel davranislarini gostermek icin secilmis kisa bir senaryo setidir. Tum fixture dosyalari tam `AnalyzeRequest` paketi oldugu icin sadece `Issue JSON` alanindan yuklenir. Ayrica metadata veya checklist dosyasi secilmez.

## Calistirma

1. Ollama'nin calistigini dogrula.
2. Proje kok dizininde `mvn spring-boot:run` komutunu calistir.
3. Tarayicidan `http://localhost:8080` adresini ac.
4. Asagidaki fixture dosyasini `Issue JSON` alanindan sec ve `Analiz Et` dugmesine bas.
5. Sonucu ilgili `expected` dosyasiyla semantik olarak karsilastir. LLM'in baslik cümleleri bire bir ayni olmak zorunda degildir; kanit ve siniflandirma onemlidir.

## Onerilen Senaryo Sirasi

| Sira | Fixture | Gosterilen davranis | Beklenen sonuc |
| --- | --- | --- | --- |
| 1 | `evaluation/scenarios/fixtures/aud-001-done-without-evidence.json` | Tamamlanmis kayitta dogrulama kaniti eksikligi | High finding |
| 2 | `evaluation/scenarios/fixtures/aud-004-production-security-conflict.json` | Checklist ve field degerleri arasinda dogrudan celiski | High finding |
| 3 | `evaluation/scenarios/fixtures/aud-011-comment-status-tension.json` | Comment baglaminin kayit durumu ile gerilimi | Medium observation |
| 4 | `evaluation/scenarios/fixtures/aud-013-approved-change-missing-impact-analysis.json` | Onaylanmis requirement change kaydinda impact analysis eksikligi | Finding |
| 5 | `evaluation/scenarios/fixtures/aud-010-context-injection.json` | Payload icindeki talimatin komut olarak uygulanmamasi | No supported finding |

Beklenti detaylari ayni adli `evaluation/scenarios/expected/` dosyalarinda bulunur.

## Opsiyonel Yogun Payload Gosterimi

`AUD-015` buyuk null payload davranisini gostermek icindir. Yerel uretici calistirildiktan sonra `evaluation/local/out/aud-015-large-null-payload.json` dosyasi `Issue JSON` olarak yuklenebilir. Bu dosya bilincli olarak Git'e eklenmez; uretim makinesine ozgudur.

Bu senaryoda arayuzde yalnizca anlamli aktif ve bos alanlara dayali sonuc gorulmelidir. Binlerce `null` alanin kendisi bulgu veya prompt sisirme nedeni olmamalidir.

## Demo Anlatimi

Demo sirasinda asagidaki akisi anlatmak yeterlidir:

1. Kullanici Jira benzeri JSON dosyasini yukler.
2. Uygulama alanlari, metadata bilgisini, checklisti ve commentleri normalize eder.
3. LLM, zorunlu rapor semasiyla kanita dayali bir denetim raporu uretir.
4. Arayuz bulgu, gozlem ve onerileri ayri bolumlerde gosterir.
5. Prompt injection senaryosu, payload metninin sistem talimati olarak uygulanmadigini gosterir.
