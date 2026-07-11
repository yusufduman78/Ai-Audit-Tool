# Evaluation Fixtures

Bu klasor, LLM davranisini degerlendirmek icin kullanilan request fixture'larini ve expected-result sozlesmelerini icerir.

- `scenarios/fixtures/`: `/api/normalize` ve `/api/analyze` endpointlerine gonderilebilen `AnalyzeRequest` JSON dosyalari.
- `scenarios/definitions/`: Generator'un okuyacagi buyuk veya varyasyonlu senaryo tanimlari.
- `scenarios/expected/`: Modelin gormedigi, beklenen audit davranisini tanimlayan sozlesmeler.
- `scenarios/generated/`: Python generator eklendiginde olusacak varyasyonlar icin ayrilmistir.

Fixture requestleri production uygulamasinin runtime bagimliligi degildir. Ilk iki fixture elle yazilmistir; bunlar sonraki generator ve benchmark calismasi icin referans davranis saglar.

Manuel normalize testi:

```bash
curl -X POST http://localhost:8080/api/normalize \
  -H "Content-Type: application/json" \
  --data @evaluation/scenarios/fixtures/aud-001-done-without-evidence.json
```

Manuel analiz testi icin ayni istekte endpointi `/api/analyze` olarak degistir.
