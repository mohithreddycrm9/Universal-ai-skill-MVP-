import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";

const PATTERNS: Array<{ id: string; re: RegExp; title: string }> = [
  { id: "ignore-prev", re: /ignore (all )?(previous|prior|above) instructions/i, title: "Instruction-hijack phrasing" },
  { id: "you-are-now", re: /you are now (a |an )?/i, title: "Role-override phrasing" },
  { id: "system-prompt", re: /reveal (your )?system prompt/i, title: "System prompt extraction" },
  { id: "disable-security", re: /disable (the )?(security|firewall|sandbox|scanner)/i, title: "Attempt to disable security controls" },
  { id: "exfil", re: /exfiltrat|send (me )?(all )?(secrets|credentials|api keys)/i, title: "Exfiltration phrasing" },
  { id: "hidden", re: /hidden instruction|\[INST\]|<!--[\s\S]{0,120}(ignore|system|secret)/i, title: "Hidden instruction pattern" },
  { id: "jailbreak", re: /jailbreak|do not follow (your )?(safety|policies)/i, title: "Jailbreak phrasing" },
  { id: "self-grant", re: /grant yourself|add capability|request_capability/i, title: "Self-grant / tool-escalation phrasing" },
];

export class PromptInjectionScanner implements SecurityScanner {
  readonly id = "prompt_injection";
  readonly version = "1.0.0";

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, _configuration: ScannerConfig): Promise<ScannerRun> {
    const findings = [];
    for (const file of walkTarget(target)) {
      for (const pattern of PATTERNS) {
        if (pattern.re.test(file.content)) {
          findings.push(
            finding(`inject:${pattern.id}`, "HIGH", pattern.title, file.path, file.content.slice(0, 160)),
          );
        }
      }
    }
    return runEnvelope(this.id, this.version, this.clock, findings.length ? "FAIL" : "PASS", findings);
  }
}
