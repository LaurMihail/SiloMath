export interface SafeLogEvent {
  event: string;
  correlationId: string;
  outcome?: string;
  durationMs?: number;
}

export type LogSink = (event: SafeLogEvent) => void;

export function safeLog(
  sink: LogSink,
  event: SafeLogEvent,
): void {
  sink({
    event: event.event,
    correlationId: event.correlationId,
    ...(event.outcome === undefined ? {} : { outcome: event.outcome }),
    ...(event.durationMs === undefined ? {} : { durationMs: event.durationMs }),
  });
}
