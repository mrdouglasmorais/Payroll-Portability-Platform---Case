export const portabilityKeys = {
  all: ['portability'] as const,
  status: () => [...portabilityKeys.all, 'status'] as const,
  agenda: (beneficiaryId: string) =>
    [...portabilityKeys.all, 'agenda', beneficiaryId] as const,
  operations: (beneficiaryId: string, page: number, size: number) =>
    [...portabilityKeys.all, 'operations', beneficiaryId, page, size] as const,
  operation: (operationId: string) =>
    [...portabilityKeys.all, 'operation', operationId] as const,
  events: (operationId: string) =>
    [...portabilityKeys.all, 'events', operationId] as const,
}
