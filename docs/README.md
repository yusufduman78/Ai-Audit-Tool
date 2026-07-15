# Proje Dokümantasyonu

Bu klasör, Generic AI Audit Tool için mimari, entegrasyon, demo ve model değerlendirme belgelerinin giriş noktasıdır. Kodun mevcut davranışını anlamak için aşağıdaki okuma sırasını kullanın.

## Önerilen Okuma Sırası

1. Projeyi çalıştırmak ve genel amacı görmek için [ana README](../README.md).
2. Sistemin iç akışını anlamak için [Güncel Sistem Mimarisi](architecture/guncel_sistem_mimarisi.md).
3. Başka bir uygulamaya eklemek için [Kütüphane Entegrasyon Rehberi](integration/kutuphane_entegrasyonu.md).
4. Core ile yerel demonun neden ayrıldığını görmek için [Kütüphane ve Demo Ayrımı](architecture/library_demo_ayrimi.md).
5. Model kalitesini ve test senaryolarını incelemek için [Değerlendirme Belgeleri](evaluation/README.md).

## Belge Haritası

### Mimari

| Belge | Ne anlatır? | Durum |
| --- | --- | --- |
| [Güncel Sistem Mimarisi](architecture/guncel_sistem_mimarisi.md) | Core akışı, normalizasyon, prompt üretimi, transport sınırı ve demo ilişkisi | Güncel ana kaynak |
| [Kütüphane ve Demo Ayrımı](architecture/library_demo_ayrimi.md) | Maven modüllerinin sorumlulukları ve bağımlılık yönü | Güncel karar kaydı |
| [Comment Context Tasarımı](architecture/comment_context_design.md) | Yorumların neden ayrı modele taşındığı ve fallback yaklaşımı | Uygulanmış tasarım notu |
| [Mimari Belgeler Dizini](architecture/README.md) | Mimari belgelerin kapsamı ve bakım kuralları | Güncel dizin |
| [Diyagramlar](diagrams/README.md) | Draw.io ve görsel UML kaynaklarının kullanımı | Elle güncellenir |

### Entegrasyon

| Belge | Ne anlatır? |
| --- | --- |
| [Kütüphane Entegrasyon Rehberi](integration/kutuphane_entegrasyonu.md) | Maven bağımlılığı, `AuditInput`, `AgentTransport`, `AgentEndpoint`, facade örneği, test ve hata yönetimi |

### Değerlendirme ve Demo

| Belge | Ne anlatır? |
| --- | --- |
| [Değerlendirme Belgeleri Dizini](evaluation/README.md) | Senaryo, veri üretimi, demo ve model sonucu belgelerinin ilişkisi |
| [Değerlendirme Stratejisi](evaluation/evaluation_strategy.md) | LLM kalitesinin hangi ölçütlerle değerlendirildiği |
| [Senaryo Kataloğu](evaluation/scenario_catalog.md) | `AUD-*` senaryolarının amacı ve beklenen davranışı |
| [Demo Veri Tasarımı](evaluation/demo_data_design.md) | Gerçekçi ama sentetik Jira benzeri veri üretim ilkeleri |
| [Demo Akışı](evaluation/demo_walkthrough.md) | Web arayüzünde önerilen sunum ve test sırası |
| [Güncel Model Sonuçları](evaluation/current_results.md) | Manuel model karşılaştırmaları ve bilinen sınırlamalar |
| [Yerel LLM ile Metin Üretimi](evaluation/local_llm_text_generation.md) | Fixture metinlerinin kontrollü şekilde üretilmesi |

## Hangi Belge Esas Alınmalı?

Belgeler arasında çelişki görülürse şu öncelik sırası kullanılır:

1. Çalışan kod ve testler.
2. [Güncel Sistem Mimarisi](architecture/guncel_sistem_mimarisi.md).
3. [Kütüphane Entegrasyon Rehberi](integration/kutuphane_entegrasyonu.md).
4. Konuya özel tasarım ve değerlendirme belgeleri.
5. Git dışında tutulan kişisel çalışma notları.

`docs/local/` altındaki kişisel çalışma rehberleri Git tarafından izlenmez ve resmi proje dokümantasyonunun parçası sayılmaz.

## Bağlantı Kuralı

Doküman içi bağlantılar depo köküne göre değil, bağlantının bulunduğu Markdown dosyasına göre göreli yazılır. Böylece bağlantılar GitHub, IDE Markdown önizlemesi ve yerel dosya görünümünde birlikte çalışır.

Yeni bir ana belge eklendiğinde bu dizine ve ilgili alt klasör dizinine bağlantı eklenmelidir. Bir uygulama kararı değiştiğinde önce güncel mimari, ardından etkilenen entegrasyon veya değerlendirme belgesi güncellenmelidir.
