import type { SkillRegistry } from "../registry/skill-registry.js";
import { newId } from "../util/ids.js";
import { redactDeep } from "../util/redact.js";
import type { Clock } from "../util/clock.js";
import { iso } from "../util/clock.js";

export class AuditLog {
  constructor(
    private readonly registry: SkillRegistry,
    private readonly clock: Clock,
  ) {}

  record(input: {
    requestId: string;
    actor: string;
    action: string;
    skillId?: string;
    fingerprint?: string;
    detail?: unknown;
  }): void {
    this.registry.insertAudit({
      id: newId("aud"),
      requestId: input.requestId,
      actor: input.actor,
      action: input.action,
      skillId: input.skillId,
      fingerprint: input.fingerprint,
      detail: redactDeep(input.detail ?? {}),
      createdAt: iso(this.clock),
    });
  }
}
