import { WorkflowEntrypoint, type WorkflowEvent, type WorkflowStep } from "cloudflare:workers";

export type TaskWorkflowParams = {
  taskId: string;
};

export class TaskWorkflow extends WorkflowEntrypoint<Env, TaskWorkflowParams> {
  override async run(
    event: WorkflowEvent<TaskWorkflowParams>,
    step: WorkflowStep,
  ): Promise<{ taskId: string; status: string; evidenceKey: string }> {
    const taskId = event.payload.taskId;
    const now = new Date().toISOString();

    await step.do("mark-task-running", async () => {
      await this.env.DB.prepare(
        "UPDATE tasks SET status = ?1, updated_at = ?2 WHERE id = ?3",
      )
        .bind("RUNNING", now, taskId)
        .run();
    });

    const evidence = await step.do("write-bootstrap-evidence", async () => {
      const key = `tasks/${taskId}/bootstrap.json`;
      const body = JSON.stringify({
        taskId,
        milestone: 2,
        result: "Provider execution intentionally deferred",
        costClass: "FREE",
        createdAt: now,
      });
      await this.env.EVIDENCE.put(key, body, {
        httpMetadata: { contentType: "application/json" },
      });
      return { key };
    });

    await step.do("request-human-decision", async () => {
      const decisionId = `decision:${taskId}`;
      await this.env.DB.batch([
        this.env.DB.prepare(
          "INSERT OR IGNORE INTO decisions (id, task_id, status, created_at) VALUES (?1, ?2, ?3, ?4)",
        ).bind(decisionId, taskId, "PENDING", now),
        this.env.DB.prepare(
          "UPDATE tasks SET status = ?1, updated_at = ?2 WHERE id = ?3",
        ).bind("WAITING_DECISION", now, taskId),
        this.env.DB.prepare(
          "INSERT OR REPLACE INTO task_runs (id, task_id, status, started_at, completed_at, evidence_key) VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        ).bind(`run:${taskId}`, taskId, "WAITING_DECISION", now, now, evidence.key),
      ]);
    });

    return { taskId, status: "WAITING_DECISION", evidenceKey: evidence.key };
  }
}
