import { tool } from "@opencode-ai/plugin"
import path from "node:path"

const pathArgument = (description: string) =>
  tool.schema.string().min(1).max(1024).describe(description)

type AdapterEnvelope = {
  success?: boolean
  schemaVersion?: string
  auditContext?: string
  errorCode?: string
  message?: string
}

const MAX_ADAPTER_RESPONSE_BYTES = 8 * 1024 * 1024

export default tool({
  description:
    "Normalize project-local audit JSON files with the trusted Java adapter and return only the bounded audit context.",
  args: {
    issuePath: pathArgument("Project-relative path to the required Issue JSON file."),
    metadataPath: pathArgument("Project-relative path to the optional metadata JSON file.").optional(),
    fieldDescriptionsPath: pathArgument(
      "Project-relative path to the optional field descriptions JSON file.",
    ).optional(),
    checklistPath: pathArgument("Project-relative path to the optional checklist JSON file.").optional(),
  },
  async execute(args, context) {
    const jarPath = path.join(
      context.worktree,
      "opencode-adapter",
      "target",
      "audittool-opencode-adapter.jar",
    )
    if (!(await Bun.file(jarPath).exists())) {
      throw new Error(
        "ADAPTER_NOT_BUILT: Run 'mvn -q -pl opencode-adapter -am package -DskipTests' once outside the audit agent.",
      )
    }

    const command = ["java", "-jar", jarPath, "--issue", args.issuePath]
    appendOptional(command, "--metadata", args.metadataPath)
    appendOptional(command, "--field-descriptions", args.fieldDescriptionsPath)
    appendOptional(command, "--checklist", args.checklistPath)

    const child = Bun.spawn(command, {
      cwd: context.worktree,
      stdout: "pipe",
      stderr: "pipe",
    })

    const abortHandler = () => child.kill()
    context.abort.addEventListener("abort", abortHandler, { once: true })
    if (context.abort.aborted) {
      child.kill()
    }

    let timedOut = false
    const timeout = setTimeout(() => {
      timedOut = true
      child.kill()
    }, 30_000)

    let stdout = ""
    let stderr = ""
    let exitCode = -1
    try {
      [stdout, stderr, exitCode] = await Promise.all([
        new Response(child.stdout).text(),
        new Response(child.stderr).text(),
        child.exited,
      ])
    } finally {
      clearTimeout(timeout)
      context.abort.removeEventListener("abort", abortHandler)
    }

    if (timedOut) {
      throw new Error("ADAPTER_TIMEOUT: Normalization did not complete within 30 seconds.")
    }
    if (context.abort.aborted) {
      throw new Error("AUDIT_CANCELLED: Normalization was cancelled by the OpenCode session.")
    }
    if (
      utf8Size(stdout) > MAX_ADAPTER_RESPONSE_BYTES ||
      utf8Size(stderr) > MAX_ADAPTER_RESPONSE_BYTES
    ) {
      throw new Error("ADAPTER_OUTPUT_TOO_LARGE: Adapter response exceeded the 8 MiB limit.")
    }
    if (exitCode !== 0) {
      const failure = parseEnvelope(stderr)
      throw new Error(
        `${failure.errorCode ?? "ADAPTER_FAILED"}: ${failure.message ?? "Normalization failed."}`,
      )
    }

    const result = parseEnvelope(stdout)
    if (
      result.success !== true ||
      result.schemaVersion !== "1.0" ||
      typeof result.auditContext !== "string" ||
      result.auditContext.trim().length === 0
    ) {
      throw new Error("INVALID_ADAPTER_RESPONSE: Adapter returned an unsupported response envelope.")
    }

    return `BEGIN_AUDIT_CONTEXT\n${result.auditContext.trim()}\nEND_AUDIT_CONTEXT`
  },
})

function appendOptional(command: string[], option: string, value?: string) {
  if (value !== undefined) {
    command.push(option, value)
  }
}

function parseEnvelope(value: string): AdapterEnvelope {
  try {
    const parsed = JSON.parse(value.trim())
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {}
    }
    return parsed as AdapterEnvelope
  } catch {
    return {}
  }
}

function utf8Size(value: string) {
  return new TextEncoder().encode(value).byteLength
}
