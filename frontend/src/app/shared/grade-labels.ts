export const NATIONAL_GRADE_LABELS: Record<string, string> = {
  '5': 'Відмінно',
  '4': 'Добре',
  '3': 'Задовільно',
  '2': 'Незадовільно',
  'passed': 'Зараховано',
  'not_passed': 'Не зараховано',
};

export const WORK_NATIONAL_GRADE_LABELS: Record<string, string> = {
  FIVE: 'Відмінно',
  FOUR: 'Добре',
  THREE: 'Задовільно',
  TWO: 'Незадовільно',
  PASSED: 'Зараховано',
  NOT_PASSED: 'Не зараховано',
};

export function nationalGradeLabel(g: string | undefined | null): string {
  if (!g) return '—';
  return NATIONAL_GRADE_LABELS[g] ?? g;
}

export function workNationalGradeLabel(g: string | undefined | null): string {
  if (!g) return '—';
  return WORK_NATIONAL_GRADE_LABELS[g] ?? g;
}
