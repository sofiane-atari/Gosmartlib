export type LocalDate = string & { readonly __brand: 'LocalDate' };

export function toLocalDate(date: Date): LocalDate {
  return date.toISOString().split('T')[0] as LocalDate;
}

