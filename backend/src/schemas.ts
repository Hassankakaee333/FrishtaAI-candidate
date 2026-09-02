import { z } from "zod";

export const deviceRegistrationSchema = z
  .object({
    deviceId: z.string().min(8).max(128),
    displayName: z.string().min(1).max(80),
    publicKeySpkiBase64: z.string().min(32).max(4096),
    buildChannel: z.enum(["stable", "candidate"]),
  })
  .strict();

export const createTaskSchema = z
  .object({
    projectId: z.string().min(1).max(128).default("hassan-ai"),
    title: z.string().trim().min(1).max(240),
    payload: z.string().max(64_000).default(""),
    source: z.enum(["APP", "SHARE_SHEET", "LOCAL_TEST"]).default("APP"),
  })
  .strict();

export const decisionSchema = z
  .object({
    action: z.enum(["APPROVE", "REJECT"]),
    deviceId: z.string().min(8).max(128),
    signedPayload: z.string().min(2).max(16_000),
    signatureBase64: z.string().min(16).max(4096),
  })
  .strict();

export const resourceLedgerSchema = z
  .object({
    providerId: z.string().min(1).max(128),
    displayName: z.string().min(1).max(160),
    costClass: z.enum(["FREE", "PREPAID"]),
    actualCostCents: z.literal(0),
    quota: z.string().max(240),
    sourceEvidence: z.string().url().max(2048),
    cardRequired: z.literal(false),
    humanGated: z.boolean(),
  })
  .strict();

export const radarFindingSchema = z
  .object({
    id: z.string().min(1).max(200),
    sourceId: z.string().min(1).max(128),
    title: z.string().min(1).max(300),
    sourceUrl: z.string().url().max(2048),
    version: z.string().max(128),
    costClass: z.literal("FREE"),
    license: z.string().min(1).max(160),
    sourceEvidence: z.string().url().max(2048),
  })
  .strict();

export type CreateTaskInput = z.infer<typeof createTaskSchema>;
