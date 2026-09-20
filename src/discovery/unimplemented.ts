import type { SkillSource } from "./skill-source.js";
import { SkillMcpError } from "../errors.js";

/** Documented extension points — not implemented as live sources in this build. */
export class UnimplementedSource implements SkillSource {
  constructor(readonly id: string) {}

  async search(): Promise<never> {
    throw new SkillMcpError("SOURCE_ERROR", `SkillSource '${this.id}' is not implemented in this build`);
  }
  async fetch(): Promise<never> {
    throw new SkillMcpError("SOURCE_ERROR", `SkillSource '${this.id}' is not implemented in this build`);
  }
  async pin(): Promise<never> {
    throw new SkillMcpError("SOURCE_ERROR", `SkillSource '${this.id}' is not implemented in this build`);
  }
}
