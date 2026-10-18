/**
 * All TanStack Query cache keys in one place. Keys are arrays from general to specific, so
 * invalidating ['groups'] also refreshes ['groups', id] and ['groups', id, 'members'].
 */
export const queryKeys = {
  me: ['me'] as const,
  myBalances: ['me', 'balances'] as const,
  groups: ['groups'] as const,
  group: (groupId: string) => ['groups', groupId] as const,
}
