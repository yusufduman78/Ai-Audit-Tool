# Output Requirements

Return only the final audit report as readable Markdown. The first visible characters of the response must be `## Özet`. Do not announce that tools, prompts, or normalization succeeded. Use these headings in this order:

## Özet
## Bulgular
## Gözlemler ve Yetersiz Bağlam
## Önerilen Aksiyonlar

Write the report narrative in Turkish. Preserve evidence quotations, field names, paths, and source values in their original language.

Under `Bulgular`, explain each supported finding with its severity, concrete evidence, short rationale, and recommended action. If there is no supported finding, state that clearly.

Under `Gözlemler ve Yetersiz Bağlam`, include only supported observations or the specific information needed to resolve an established information gap. If neither exists, state that clearly. Do not add an observation merely to avoid an empty section and do not repeat findings.

Write both `Observation` and `Insufficient Context` classifications under `Gözlemler ve Yetersiz Bağlam`; distinguish them in the item label or opening sentence. Do not create an additional top-level section.

Return only the final report. Do not include JSON, code fences, drafts, notes, self-corrections, alternative answers, or chain-of-thought.
