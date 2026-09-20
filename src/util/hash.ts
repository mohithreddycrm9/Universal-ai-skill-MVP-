import { createHash } from "node:crypto";

export function sha256(input: string | Buffer): string {
  return `sha256:${createHash("sha256").update(input).digest("hex")}`;
}

export function sha256RawHex(input: string | Buffer): string {
  return createHash("sha256").update(input).digest("hex");
}
