export class Metrics {
  private readonly counters = new Map<string, number>();

  inc(name: string, amount = 1): void {
    this.counters.set(name, (this.counters.get(name) ?? 0) + amount);
  }

  snapshot(): Record<string, number> {
    return Object.fromEntries(this.counters.entries());
  }

  prometheus(): string {
    return Object.entries(this.snapshot())
      .map(([name, value]) => `skill_mcp_${name} ${value}`)
      .join("\n");
  }
}
