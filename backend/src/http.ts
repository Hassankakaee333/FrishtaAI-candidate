import { ZodError, type ZodType } from "zod";

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

export function json(data: unknown, status = 200): Response {
  return Response.json(data, { status, headers: JSON_HEADERS });
}

export function problem(status: number, code: string, message: string): Response {
  return json({ error: { code, message } }, status);
}

export async function parseJson<T>(request: Request, schema: ZodType<T>): Promise<T> {
  const body = await readBodyWithLimit(request, 64_000);
  let value: unknown;
  try {
    value = JSON.parse(body);
  } catch {
    throw new RequestError(400, "INVALID_JSON", "Request body must be valid JSON.");
  }
  try {
    return schema.parse(value);
  } catch (error) {
    if (error instanceof ZodError) {
      throw new RequestError(422, "VALIDATION_FAILED", error.issues[0]?.message ?? "Invalid request.");
    }
    throw error;
  }
}

async function readBodyWithLimit(request: Request, limit: number): Promise<string> {
  const declaredLength = Number(request.headers.get("content-length") ?? "0");
  if (declaredLength > limit) {
    throw new RequestError(413, "PAYLOAD_TOO_LARGE", "Request body exceeds 64 KB.");
  }
  if (!request.body) {
    throw new RequestError(400, "INVALID_JSON", "Request body must be valid JSON.");
  }
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > limit) {
      await reader.cancel("payload too large");
      throw new RequestError(413, "PAYLOAD_TOO_LARGE", "Request body exceeds 64 KB.");
    }
    chunks.push(value);
  }
  const combined = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    combined.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder().decode(combined);
}

export class RequestError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}
