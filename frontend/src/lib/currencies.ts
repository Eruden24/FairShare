/** Currencies offered in pickers. The backend accepts any ISO 4217 code; these are the common ones. */
export const CURRENCIES = [
  'EUR',
  'USD',
  'GBP',
  'CHF',
  'SEK',
  'NOK',
  'DKK',
  'PLN',
  'CZK',
  'JPY',
  'CAD',
  'AUD',
] as const

// Currency names are words, so they follow the UI's language (<html lang>), not the browser's.
// Amounts and dates (lib/format.ts) do follow the user's regional format, e.g. 12,50 € in Greece.
const names = new Intl.DisplayNames(document.documentElement.lang || 'en', { type: 'currency' })

/** "EUR — Euro" */
export function currencyLabel(code: string) {
  return `${code} — ${names.of(code) ?? code}`
}
