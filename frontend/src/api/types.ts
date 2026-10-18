import type { components } from './schema'

/** Short names for the API's schemas, so features import `Group` instead of components['schemas'][...] */
type Schemas = components['schemas']

export type User = Schemas['UserResponse']
export type CreateUserRequest = Schemas['CreateUserRequest']
export type LoginRequest = Schemas['LoginRequest']
export type Token = Schemas['TokenResponse']
export type Group = Schemas['GroupResponse']
export type Member = Schemas['MemberResponse']
export type Expense = Schemas['ExpenseResponse']
export type ExpenseRequest = Schemas['ExpenseRequest']
export type Split = Schemas['SplitResponse']
export type SplitType = NonNullable<ExpenseRequest['splitType']>
export type ExpensePage = Schemas['PageResponseExpenseResponse']
export type GroupBalances = Schemas['GroupBalancesResponse']
export type MemberBalance = Schemas['MemberBalanceResponse']
export type MyBalance = Schemas['MyBalanceResponse']
export type Settlement = Schemas['SettlementResponse']
export type SettlementRequest = Schemas['SettlementRequest']
export type SuggestedTransfer = Schemas['SuggestedTransferResponse']
