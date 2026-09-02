import {
  createTaskSchema,
  decisionSchema,
  deviceRegistrationSchema,
  radarFindingSchema,
  resourceLedgerSchema,
} from "./schemas";
import { json, parseJson, problem, RequestError } from "./http";
export { TaskWorkflow } from "./workflow";

type TaskRow = {
  id: string;
  project_id: string;
  title: string;
  payload: string;
  source: string;
  status: string;
  created_at: string;
  updated_at: string;
};

type ProjectRow = {
  id: string;
  name: string;
  description: string;
  created_at: string;
};

type ProviderRow = {
  id: string;
  name: string;
  cost_class: string;
  state: string;
  enabled: number;
  notes: string;
};

type ResourceLedgerRow = {
  provider_id: string;
  display_name: string;
  cost_class: "FREE" | "PREPAID";
  actual_cost_cents: number;
  quota: string;
  source_evidence: string;
  card_required: number;
  human_gated: number;
  last_verified_at: string;
};

type RadarResourceRow = {
  id: string;
  source_id: string;
  title: string;
  source_url: string;
  version: string;
  cost_class: "FREE";
  license: string;
  source_evidence: string;
  verified_at: string;
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    console.log(JSON.stringify({ message: "request", method: request.method, path: url.pathname }));

    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json({
          ok: true,
          service: "hassan-ai-control-plane",
          milestone: 2,
          freeOnly: true,
          additionalSpendLimit: 0,
        });
      }

      if (request.method === "POST" && url.pathname === "/device/register") {
        const input = await parseJson(request, deviceRegistrationSchema);
        const now = new Date().toISOString();
        await env.DB.prepare(
          `INSERT INTO devices (id, display_name, public_key_spki_base64, build_channel, created_at, last_seen_at)
           VALUES (?1, ?2, ?3, ?4, ?5, ?5)
           ON CONFLICT(id) DO UPDATE SET
             display_name = excluded.display_name,
             public_key_spki_base64 = excluded.public_key_spki_base64,
             build_channel = excluded.build_channel,
             last_seen_at = excluded.last_seen_at`,
        )
          .bind(
            input.deviceId,
            input.displayName,
            input.publicKeySpkiBase64,
            input.buildChannel,
            now,
          )
          .run();
        return json({ registered: true, deviceId: input.deviceId }, 201);
      }

      if (request.method === "POST" && url.pathname === "/tasks") {
        const input = await parseJson(request, createTaskSchema);
        const id = crypto.randomUUID();
        const now = new Date().toISOString();
        await env.DB.prepare(
          `INSERT INTO tasks (id, project_id, title, payload, source, status, created_at, updated_at)
           VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?7)`,
        )
          .bind(id, input.projectId, input.title, input.payload, input.source, "QUEUED", now)
          .run();
        await env.TASK_WORKFLOW.create({
          id: `task-${id}`,
          params: { taskId: id },
        });
        return json({ id, status: "QUEUED" }, 202);
      }

      if (request.method === "GET" && url.pathname === "/tasks") {
        const result = await env.DB.prepare(
          "SELECT * FROM tasks ORDER BY updated_at DESC LIMIT 100",
        ).run<TaskRow>();
        return json({ tasks: result.results });
      }

      const taskMatch = url.pathname.match(/^\/tasks\/([0-9a-f-]+)$/);
      if (request.method === "GET" && taskMatch?.[1]) {
        const task = await env.DB.prepare("SELECT * FROM tasks WHERE id = ?1")
          .bind(taskMatch[1])
          .first<TaskRow>();
        return task ? json({ task }) : problem(404, "NOT_FOUND", "Task not found.");
      }

      const decisionMatch = url.pathname.match(/^\/tasks\/([0-9a-f-]+)\/decision$/);
      if (request.method === "POST" && decisionMatch?.[1]) {
        const input = await parseJson(request, decisionSchema);
        const taskId = decisionMatch[1];
        const decisionId = `decision:${taskId}`;
        const now = new Date().toISOString();
        const nextStatus = input.action === "APPROVE" ? "APPROVED" : "REJECTED";
        const result = await env.DB.batch([
          env.DB.prepare(
            `UPDATE decisions
             SET action = ?1, status = ?2, signature_base64 = ?3, signed_payload = ?4, decided_at = ?5
             WHERE id = ?6`,
          ).bind(
            input.action,
            nextStatus,
            input.signatureBase64,
            input.signedPayload,
            now,
            decisionId,
          ),
          env.DB.prepare(
            "UPDATE tasks SET status = ?1, updated_at = ?2 WHERE id = ?3",
          ).bind(nextStatus, now, taskId),
        ]);
        return json({ taskId, status: nextStatus, changed: result[1]?.meta.changes ?? 0 });
      }

      if (request.method === "GET" && url.pathname === "/projects") {
        const result = await env.DB.prepare(
          "SELECT * FROM projects ORDER BY created_at",
        ).run<ProjectRow>();
        return json({ projects: result.results });
      }

      if (request.method === "GET" && url.pathname === "/providers") {
        const result = await env.DB.prepare(
          "SELECT * FROM providers ORDER BY cost_class, name",
        ).run<ProviderRow>();
        return json({ providers: result.results });
      }

      if (request.method === "GET" && url.pathname === "/resources") {
        const result = await env.DB.prepare(
          "SELECT * FROM resource_ledger ORDER BY cost_class, display_name",
        ).run<ResourceLedgerRow>();
        return json({ resources: result.results, actualCostCents: 0 });
      }

      if (request.method === "PUT" && url.pathname === "/resources") {
        const input = await parseJson(request, resourceLedgerSchema);
        const now = new Date().toISOString();
        await env.DB.prepare(
          `INSERT INTO resource_ledger
           (provider_id, display_name, cost_class, actual_cost_cents, quota, source_evidence, card_required, human_gated, last_verified_at)
           VALUES (?1, ?2, ?3, 0, ?4, ?5, 0, ?6, ?7)
           ON CONFLICT(provider_id) DO UPDATE SET
             display_name = excluded.display_name,
             cost_class = excluded.cost_class,
             actual_cost_cents = 0,
             quota = excluded.quota,
             source_evidence = excluded.source_evidence,
             card_required = 0,
             human_gated = excluded.human_gated,
             last_verified_at = excluded.last_verified_at`,
        )
          .bind(
            input.providerId,
            input.displayName,
            input.costClass,
            input.quota,
            input.sourceEvidence,
            input.humanGated ? 1 : 0,
            now,
          )
          .run();
        return json({ providerId: input.providerId, actualCostCents: 0 }, 200);
      }

      if (request.method === "GET" && url.pathname === "/radar/findings") {
        const result = await env.DB.prepare(
          "SELECT * FROM radar_resources ORDER BY verified_at DESC LIMIT 100",
        ).run<RadarResourceRow>();
        return json({ findings: result.results });
      }

      if (request.method === "POST" && url.pathname === "/radar/findings") {
        const input = await parseJson(request, radarFindingSchema);
        const now = new Date().toISOString();
        await env.DB.prepare(
          `INSERT INTO radar_resources
           (id, source_id, title, source_url, version, cost_class, license, source_evidence, verified_at)
           VALUES (?1, ?2, ?3, ?4, ?5, 'FREE', ?6, ?7, ?8)
           ON CONFLICT(id) DO UPDATE SET
             title = excluded.title,
             source_url = excluded.source_url,
             version = excluded.version,
             license = excluded.license,
             source_evidence = excluded.source_evidence,
             verified_at = excluded.verified_at`,
        )
          .bind(
            input.id,
            input.sourceId,
            input.title,
            input.sourceUrl,
            input.version,
            input.license,
            input.sourceEvidence,
            now,
          )
          .run();
        return json({ id: input.id, persisted: true, actualCostCents: 0 }, 201);
      }

      return problem(404, "NOT_FOUND", "Route not found.");
    } catch (error) {
      if (error instanceof RequestError) {
        return problem(error.status, error.code, error.message);
      }
      const message = error instanceof Error ? error.message : "Unknown error";
      console.error(JSON.stringify({ message: "request failed", path: url.pathname, error: message }));
      return problem(500, "INTERNAL_ERROR", "Internal server error.");
    }
  },
} satisfies ExportedHandler<Env>;
