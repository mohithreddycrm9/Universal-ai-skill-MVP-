import type { BuildSuggestionsResult } from "../agent/build-suggestions.js";

export function formatSuggestionsForTerminal(data: {
  suggestions: BuildSuggestionsResult["suggestions"];
  emptyReason?: string;
  buildContextUsed?: BuildSuggestionsResult["buildContextUsed"];
}): string {
  const lines: string[] = [];
  const width = 70;

  if (!data.suggestions.length) {
    lines.push("No build hints to show.");
    if (data.emptyReason) {
      lines.push("");
      lines.push(data.emptyReason);
    }
    return lines.join("\n");
  }

  lines.push("┌─ Build hints " + "─".repeat(width - 15) + "┐");
  for (const s of data.suggestions) {
    const chip = ` ${s.label} `;
    lines.push(`│${chip.padEnd(width + 2)}│`);
  }
  lines.push("├" + "─".repeat(width + 2) + "┤");
  lines.push("│ (copy a prompt below into Cursor Agent)".padEnd(width + 2) + " │");
  lines.push("└" + "─".repeat(width + 2) + "┘");
  lines.push("");

  data.suggestions.forEach((s, i) => {
    lines.push(`${i + 1}. ${s.label}`);
    lines.push(`   because: ${s.because ?? "—"}  |  action: ${s.action}`);
    if (s.prompt) {
      lines.push(`   prompt: ${s.prompt}`);
    }
    if (s.toolName) {
      lines.push(`   tool: ${s.toolName} ${JSON.stringify(s.toolArgs ?? {})}`);
    }
    lines.push("");
  });

  if (data.buildContextUsed) {
    lines.push("Context: " + JSON.stringify(data.buildContextUsed));
  }

  return lines.join("\n");
}
