import type { CostMetadata } from "./types.js";

/**
 * STRIX OSS is free; its runtime LLM may be chargeable.
 * Classify configured model/provider before any `strix --target` spawn.
 */

export type StrixLlmClass = "local_free" | "external" | "unknown";

export interface StrixLlmInspection {
  class: StrixLlmClass;
  model: string;
  provider: string;
  hasApiKey: boolean;
  explicitLocalFree: boolean;
  reason: string;
}

const LOCAL_MARKERS =
  /\b(ollama|lm[\s_-]?studio|localhost|127\.0\.0\.1|::1)\b|^(local)([/:_-]|$)|[/._-]local([/:_-]|$)/i;

const EXTERNAL_MARKERS =
  /\b(openai|anthropic|openrouter|google|gemini|bedrock|vertex|azure|claude|chatgpt|gpt-3|gpt-4|gpt-5|o1-|o3-|mistral\.ai|together\.ai|fireworks|groq|deepseek|cohere|perplexity|huggingface\.co|api\.openai|generativelanguage)\b/i;

export function truthyFlag(value: unknown): boolean {
  if (value === true || value === 1) {
    return true;
  }
  if (typeof value === "string") {
    const v = value.trim().toLowerCase();
    return v === "1" || v === "true" || v === "yes" || v === "on";
  }
  return false;
}

export function inspectStrixLlm(input: {
  env?: NodeJS.ProcessEnv;
  config?: Record<string, unknown>;
} = {}): StrixLlmInspection {
  const env = input.env ?? process.env;
  const cfg = input.config ?? {};

  const model = String(
    cfg.model ?? cfg.llmModel ?? env.STRIX_LLM ?? env.LLM_MODEL ?? "",
  ).trim();
  const provider = String(cfg.provider ?? cfg.llmProvider ?? "").trim();
  const hasApiKey = Boolean(
    String(env.LLM_API_KEY ?? "").trim() || String(env.OPENAI_API_KEY ?? "").trim(),
  );
  const explicitLocalFree =
    truthyFlag(cfg.llmIsLocalFree) || truthyFlag(env.SKILL_MCP_STRIX_LLM_IS_FREE);

  const haystack = `${provider} ${model}`.trim();

  // External/chargeable markers always win — env/config attestation cannot rebrand OpenAI/etc as free.
  if (haystack && EXTERNAL_MARKERS.test(haystack)) {
    return {
      class: "external",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: false,
      reason: `Model/provider looks like an external/chargeable LLM (${haystack}). Attestation/env flags cannot override this.`,
    };
  }

  // Proven local markers (ollama/lmstudio/localhost) — attestation not required.
  if (haystack && LOCAL_MARKERS.test(haystack)) {
    return {
      class: "local_free",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: false,
      reason: `Model/provider string looks local/free (${haystack}).`,
    };
  }

  // API key alone never implies free (with or without attestation).
  if (!haystack && hasApiKey) {
    return {
      class: "external",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: false,
      reason: "LLM API key present without a proven local/free model — treated as external/chargeable. API keys never imply free.",
    };
  }

  // Attestation is supporting evidence only for proven-local strings — never for empty, mystery, or external models.
  // Env/config flags alone cannot bypass ALLOW_FREE_ONLY.
  if (explicitLocalFree && haystack && LOCAL_MARKERS.test(haystack)) {
    return {
      class: "local_free",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: true,
      reason: "Explicit llmIsLocalFree / SKILL_MCP_STRIX_LLM_IS_FREE plus local markers attests local/free LLM.",
    };
  }
  if (explicitLocalFree) {
    return {
      class: "unknown",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: true,
      reason: "Free-LLM attestation present but model/provider not proven local (empty or unrecognized). Attestation alone cannot bypass ALLOW_FREE_ONLY.",
    };
  }

  if (!haystack && !hasApiKey) {
    return {
      class: "unknown",
      model,
      provider,
      hasApiKey,
      explicitLocalFree: false,
      reason: "No STRIX_LLM / LLM_MODEL / provider configured.",
    };
  }

  return {
    class: "unknown",
    model,
    provider,
    hasApiKey,
    explicitLocalFree: false,
    reason: `LLM model/provider not proven local/free (${haystack || "unset"}). Unknown cost is not assumed free.`,
  };
}

/** Runtime cost metadata for CostDetector — not the static OSS-CLI catalog row. */
export function costMetadataForStrixLlm(inspection: StrixLlmInspection): CostMetadata {
  if (inspection.class === "local_free") {
    return {
      provider: "usestrix/strix",
      service: "STRIX OSS CLI + local/free LLM",
      pricingModel: "free",
      freeTier: true,
      estimatedCost: "0",
      requiresApproval: false,
      purpose: "Local STRIX scan with proven local/free LLM",
      freeAlternative: "Built-in scanners (secret, injection, dependency) + Semgrep/Gitleaks/Trivy",
    };
  }
  if (inspection.class === "external") {
    return {
      provider: "usestrix/strix",
      service: "STRIX OSS CLI + external LLM",
      pricingModel: "usage_based",
      freeTier: false,
      estimatedCost: "unknown",
      requiresApproval: true,
      purpose: "STRIX scan that may call an external/chargeable LLM",
      freeAlternative: "Set STRIX_LLM to a local/ollama/lmstudio model, or SKILL_MCP_STRIX_LLM_IS_FREE=1",
    };
  }
  return {
    provider: "usestrix/strix",
    service: "STRIX OSS CLI + unverified LLM",
    pricingModel: "unknown",
    freeTier: false,
    estimatedCost: "unknown",
    requiresApproval: true,
    purpose: "STRIX scan with LLM cost not proven free",
    freeAlternative: "Configure a local/free model (ollama/lmstudio/localhost) or attest SKILL_MCP_STRIX_LLM_IS_FREE=1",
  };
}
