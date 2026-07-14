# Demo Akisi

Bu akis, web arayuzunde projenin temel davranislarini gostermek icin secilmis kisa bir senaryo setidir. Temel demo icin `evaluation/demo-inputs/` altindaki ayri issue, metadata ve checklist dosyalari kullanilir. Eski tam fixture dosyalari da desteklenmeye devam eder.

## Calistirma

1. Ollama'nin calistigini dogrula.
2. Proje kok dizininde `mvn clean package -DskipTests` komutunu calistir.
3. `java -jar demo/target/audittool-demo-0.0.1-SNAPSHOT.jar` komutuyla demoyu baslat.
4. Tarayicidan `http://localhost:8080` adresini ac.
5. Model listesinden `qwen3:4b-instruct (onerilen)` secimini koru ve thinking modunu kapali birak.
6. `evaluation/demo-inputs/aud-002-missing-acceptance-criteria/` altindaki dort JSON dosyasini karsilik gelen alanlara yukle.
7. `Analiz Et` dugmesine bas ve sonucu ilgili `expected` dosyasiyla semantik olarak karsilastir. LLM'in baslik cumleleri bire bir ayni olmak zorunda degildir; kanit ve siniflandirma onemlidir.

Model seciminin calistigini gostermek icin ayni girdiyi `phi4-mini-reasoning:latest` ile tekrar calistirmak opsiyoneldir. Cikti Ingilizce olabilir ve siniflandirma varsayilan modelden farkli olabilir; bu davranis model karsilastirmasinin bir parcasidir.

## Onerilen Senaryo Sirasi

| Sira | Fixture | Gosterilen davranis | Beklenen sonuc |
| --- | --- | --- | --- |
| 1 | `evaluation/demo-inputs/aud-002-missing-acceptance-criteria/` | Ayri issue, metadata ve checklist dosyalariyla kabul kriteri eksikligi | Medium finding |
| 2 | `evaluation/scenarios/fixtures/aud-003-role-independence-risk.json` | Desteklenen ancak kanitlanmamis rol bagimsizligi riski | Observation |
| 3 | `evaluation/demo-inputs/aud-004-production-security-conflict/` | Checklist ve field degerleri arasinda dogrudan celiski | High finding |
| 4 | `evaluation/demo-inputs/aud-013-approved-change-missing-impact-analysis/` | Onaylanmis requirement change kaydinda impact analysis eksikligi | High finding |
| 5 | `evaluation/scenarios/fixtures/aud-010-context-injection.json` | Payload icindeki talimatin komut olarak uygulanmamasi | No supported finding |

Beklenti detaylari ayni adli `evaluation/scenarios/expected/` dosyalarinda bulunur.

Bu liste `qwen3:4b-instruct`, `temperature=0.2` ve `seed=42` ile 12 Temmuz 2026 tarihinde manuel olarak dogrulanan senaryolardan olusur. Model veya prompt degistiginde demo oncesi tekrar kontrol edilmelidir.

`AUD-001`, `AUD-007` ve `AUD-011` bilinen model sinirlamalarini gostermek icin evaluation setinde tutulur, ancak varsayilan demo akimina dahil edilmez. Ayrintilar `current_results.md` dosyasindadir.

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
