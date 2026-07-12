# Evaluation Fixtures

Bu klasor, LLM davranisini degerlendirmek icin kullanilan request fixture'larini ve expected-result sozlesmelerini icerir.

- `scenarios/fixtures/`: `/api/normalize` ve `/api/analyze` endpointlerine gonderilebilen `AnalyzeRequest` JSON dosyalari.
- `scenarios/definitions/`: Generator'un okuyacagi buyuk veya varyasyonlu senaryo tanimlari.
- `scenarios/expected/`: Modelin gormedigi, beklenen audit davranisini tanimlayan sozlesmeler.
- `scenarios/generated/`: Python generator eklendiginde olusacak varyasyonlar icin ayrilmistir.
- `demo-inputs/`: Web arayuzune ayri ayri yuklenebilen `issue.json`, `metadata.json` ve `checklist.json` paketleri.

Fixture requestleri production uygulamasinin runtime bagimliligi degildir. Fixture ve expected dosyalari, model davranisini ayni audit sozlesmesine gore karsilastirmak icin referans saglar.

Guncel manuel model sonuclari `docs/evaluation/current_results.md`, web arayuzu demo sirasi ise `docs/evaluation/demo_walkthrough.md` icindedir.

Manuel normalize testi:

```bash
curl -X POST http://localhost:8080/api/normalize \
  -H "Content-Type: application/json" \
  --data @evaluation/scenarios/fixtures/aud-001-done-without-evidence.json
```

Manuel analiz testi icin ayni istekte endpointi `/api/analyze` olarak degistir.

Web arayuzu testi icin `demo-inputs/` altindaki bir senaryo klasorunden issue, metadata, field descriptions ve checklist dosyalarini ilgili alanlara yukle. Arayuz bu dosyalari tek `AnalyzeRequest` icinde birlestirir; backend'e ayri HTTP istekleri gonderilmez.
