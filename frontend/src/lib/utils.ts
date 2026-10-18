import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Joins class names and lets later Tailwind classes win: cn('px-2', isWide && 'px-4') -> 'px-4' */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** "Anna Smith" -> "AS", for avatars */
export function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}
