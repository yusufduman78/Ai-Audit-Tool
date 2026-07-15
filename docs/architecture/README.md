# Mimari Belgeler

Bu klasör, sistemin çalışan mimarisini ve uygulanmış tasarım kararlarını açıklar. Projenin ana ürünü `core` kütüphanesidir; Spring Boot, Ollama ve web arayüzü `demo` modülünde kalır.

## Belgeler

| Belge | Kapsam | Ne zaman okunmalı? |
| --- | --- | --- |
| [Güncel Sistem Mimarisi](guncel_sistem_mimarisi.md) | Public API, normalizasyon, metadata, comment, prompt ve transport akışı | Sistemi uçtan uca anlamak için ilk belge |
| [Kütüphane ve Demo Ayrımı](library_demo_ayrimi.md) | Modül sınırları, bağımlılık yönü ve demo adapterleri | Dağıtım ve paket sorumluluklarını anlamak için |
| [Comment Context Tasarımı](comment_context_design.md) | Comment extraction, coverage ve güvenlik kararları | Yorum işleme davranışını değiştirmeden önce |
| [Kütüphane Entegrasyon Rehberi](../integration/kutuphane_entegrasyonu.md) | Dış projeden kullanım ve `AgentTransport` implementasyonu | Kurum entegrasyonu yapılırken |
| [Diyagram Kaynakları](../diagrams/README.md) | Draw.io kaynağı ve görsel UML | Elle diyagram güncellenirken |

## Mimari Kaynak Sırası

Çalışan kod ve testler birinci kaynaktır. Metinsel ana kaynak [Güncel Sistem Mimarisi](guncel_sistem_mimarisi.md), dış projeden kullanımın ana kaynağı ise [Kütüphane Entegrasyon Rehberi](../integration/kutuphane_entegrasyonu.md) belgesidir.

## Bakım Kuralı

- Public API değişirse güncel mimari ve entegrasyon rehberi birlikte güncellenir.
- Normalizasyon davranışı değişirse güncel mimari ile ilgili tasarım notu birlikte güncellenir.
- Yalnızca demo davranışı değişirse core akışı değiştirilmiş gibi anlatılmaz.
- UML görseli elle güncellenene kadar metinsel mimari belge esas alınır.

[Dokümantasyon merkezine dön](../README.md).
