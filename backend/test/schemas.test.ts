import { describe, expect, it } from "vitest";
import {
  createTaskSchema,
  decisionSchema,
  deviceRegistrationSchema,
  radarFindingSchema,
  resourceLedgerSchema,
} from "../src/schemas";
import { parseJson } from "../src/http";

describe("Milestone 2 API schemas", () => {
  it("defaults every new task to the Hassan project without adding cost state", () => {
    const value = createTaskSchema.parse({ title: "Build candidate" });
    expect(value.projectId).toBe("hassan-ai");
    expect(value.source).toBe("APP");
    expect(value.payload).toBe("");
  });

  it("rejects unknown task fields", () => {
    expect(() =>
      createTaskSchema.parse({ title: "Spend", automaticPayment: true }),
    ).toThrow();
  });

  it("accepts only stable and candidate device channels", () => {
    expect(
      deviceRegistrationSchema.safeParse({
        deviceId: "device-12345",
        displayName: "S25 Ultra",
        publicKeySpkiBase64: "A".repeat(64),
        buildChannel: "candidate",
      }).success,
    ).toBe(true);
    expect(
      deviceRegistrationSchema.safeParse({
        deviceId: "device-12345",
        displayName: "S25 Ultra",
        publicKeySpkiBase64: "A".repeat(64),
        buildChannel: "nightly",
      }).success,
    ).toBe(false);
  });

  it("requires a signed approve or reject decision", () => {
    expect(
      decisionSchema.safeParse({
        action: "APPROVE",
        deviceId: "device-12345",
        signedPayload: "{}",
        signatureBase64: "A".repeat(32),
      }).success,
    ).toBe(true);
  });

  it("enforces zero cost and refuses metered resources", () => {
    const free = {
      providerId: "radar",
      displayName: "Radar",
      costClass: "FREE",
      actualCostCents: 0,
      quota: "public",
      sourceEvidence: "https://example.com/evidence",
      cardRequired: false,
      humanGated: false,
    };
    expect(resourceLedgerSchema.safeParse(free).success).toBe(true);
    expect(resourceLedgerSchema.safeParse({ ...free, costClass: "METERED" }).success).toBe(false);
    expect(resourceLedgerSchema.safeParse({ ...free, actualCostCents: 1 }).success).toBe(false);
    expect(resourceLedgerSchema.safeParse({ ...free, cardRequired: true }).success).toBe(false);
  });

  it("accepts only free sourced radar findings", () => {
    expect(
      radarFindingSchema.safeParse({
        id: "ollama:v1",
        sourceId: "ollama",
        title: "Release",
        sourceUrl: "https://github.com/ollama/ollama/releases/tag/v1",
        version: "v1",
        costClass: "FREE",
        license: "MIT",
        sourceEvidence: "https://api.github.com/repos/ollama/ollama/releases/latest",
      }).success,
    ).toBe(true);
  });

  it("enforces the JSON limit even without a content-length header", async () => {
    const request = new Request("https://local.test/tasks", {
      method: "POST",
      body: JSON.stringify({ title: "A".repeat(70_000) }),
    });
    await expect(parseJson(request, createTaskSchema)).rejects.toMatchObject({
      status: 413,
      code: "PAYLOAD_TOO_LARGE",
    });
  });
});
