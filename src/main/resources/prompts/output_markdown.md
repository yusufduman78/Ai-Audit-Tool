# Output Requirements

Return only the final audit report as readable Markdown. Use these headings in this order:

## Özet
## Bulgular
## Gözlemler ve Yetersiz Bağlam
## Önerilen Aksiyonlar

Write the report narrative in Turkish. Preserve evidence quotations, field names, paths, and source values in their original language.

Under `Bulgular`, explain each supported finding with its severity, concrete evidence, short rationale, and recommended action. If there is no supported finding, state that clearly.

Under `Gozlemler ve Yetersiz Baglam`, include only supported observations or the specific information needed to resolve an established information gap. Do not repeat findings.

Return only the final report. Do not include JSON, code fences, drafts, notes, self-corrections, alternative answers, or chain-of-thought.
