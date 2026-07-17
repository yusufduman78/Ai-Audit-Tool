# Output Requirements

Return only a valid JSON object. Do not use Markdown, code fences, explanatory text, or fields outside this contract:

{
  "summary": "Turkish summary",
  "findings": [{"title": "", "category": "", "severity": "High|Medium|Low", "evidence": [""], "rationale": "", "recommendedAction": ""}],
  "observations": [{"type": "Observation|Insufficient Context", "description": "", "evidence": [""]}],
  "recommendation": "Turkish recommendation"
}

Use empty arrays when there are no findings or observations. Preserve field names and values as they appear in the context. Do not repeat the same issue in multiple sections.

The JSON contract has two collections: put `Finding` items in `findings`; put both `Observation` and `Insufficient Context` items in `observations`, distinguished by the exact `type` value. Do not create a third collection.

Write `summary`, finding `title`, `category`, `rationale`, `recommendedAction`, observation `description`, and `recommendation` in Turkish. Keep evidence quotations, field names, paths, and source values in their original language.

Choose the report classification before writing. The JSON schema provided by the runtime is mandatory: use its exact field names and do not create synonyms such as `short_justification`. Return only the final report; do not include drafts, notes, self-corrections, alternative answers, or explanations of how the report should be rewritten.

# Final Self-Check

Before returning the final JSON, silently verify that:

- the object uses only the exact schema fields;
- `findings` and `observations` are present as arrays;
- every finding has non-empty `title`, `category`, `severity`, `evidence`, `rationale`, and `recommendedAction` values;
- observations contain only `type`, `description`, and `evidence`, without a severity field;
- all narrative fields are in Turkish while source evidence remains in its original language;
- the same issue is not repeated in both arrays or duplicated within an array.

Return only the checked final JSON object.
