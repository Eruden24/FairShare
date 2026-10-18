/**
 * Formatting in the user's locale. Amounts come from the API as decimals in the group's currency
 * (12.5 for 12.50 EUR); Intl knows how many decimals each currency shows.
 */
const moneyFormatters = new Map<string, Intl.NumberFormat>()

export function formatMoney(amount: number, currency: string, options: { signed?: boolean } = {}) {
  const key = `${currency}:${options.signed ? 'signed' : 'plain'}`
  let formatter = moneyFormatters.get(key)
  if (!formatter) {
    formatter = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      signDisplay: options.signed ? 'exceptZero' : 'auto',
    })
    moneyFormatters.set(key, formatter)
  }
  return formatter.format(amount)
}

const dateFormatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

/** "2026-10-01" (a date without time from the API) -> "Oct 1, 2026", without timezone shifts */
export function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return dateFormatter.format(new Date(year!, month! - 1, day!))
}

/** Today as "2026-10-18" in the user's timezone, for date inputs */
export function todayIso() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
