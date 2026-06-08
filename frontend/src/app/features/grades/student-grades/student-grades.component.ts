import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GradeService } from '../../../core/services/grade.service';
import { BookService } from '../../../core/services/book.service';
import { AdditionalWorkService } from '../../../core/services/additional-work.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { UserService } from '../../../core/services/user.service';
import { StudentDisciplineDTO, GradeDTO } from '../../../models/grade.model';
import { BookNumber } from '../../../models/book.model';
import { AdditionalWork, WorkType, WorkState } from '../../../models/additional-work.model';
import { nationalGradeLabel, workNationalGradeLabel } from '../../../shared/grade-labels';

interface DisplayRow {
  kind: 'discipline' | 'work';
  name: string;
  workType?: WorkType;
  academicYear: string;
  semester: number | null;
  attempt: number | null;
  ectsCredits: number | null;
  totalHours: number | null;
  universityGrade: number | null;
  ectsGrade: string | null;
  nationalGradeLabel: string;
  statusLabel: string;
  statusCss: string;
  result: string | null;
  resultLabel: string;
  resultCss: string;
  navId: number;
}

@Component({
  selector: 'app-student-grades',
  standalone: true,
  imports: [],
  templateUrl: './student-grades.component.html',
  styleUrl: './student-grades.component.css',
})
export class StudentGradesComponent implements OnInit {
  bookNumberId = signal(0);
  book = signal<BookNumber | null>(null);
  disciplines = signal<StudentDisciplineDTO[]>([]);
  additionalWorks = signal<AdditionalWork[]>([]);
  selectedSemester = signal<number | null>(null);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gradeService = inject(GradeService);
  private bookService = inject(BookService);
  private additionalWorkService = inject(AdditionalWorkService);
  private authState = inject(AuthStateService);
  private userService = inject(UserService);

  isStudent = this.authState.isStudent;

  allSemesters = computed(() => {
    const semesters = new Set<number>();
    this.disciplines().forEach(d => { if (d.semester !== null) semesters.add(d.semester); });
    this.additionalWorks().forEach(w => { if (w.courseWorkDetails?.semester) semesters.add(w.courseWorkDetails.semester); });
    return [...semesters].sort((a, b) => a - b);
  });

  combinedRows = computed((): DisplayRow[] => {
    const sem = this.selectedSemester();

    const disciplineRows: DisplayRow[] = this.disciplines()
      .filter(d => sem === null || d.semester === sem)
      .map(d => {
        const g = this.latestGrade(d);
        return {
          kind: 'discipline',
          name: d.disciplineName,
          academicYear: d.academicYear,
          semester: d.semester,
          attempt: d.attempt,
          ectsCredits: this.ectsCreditsFor(d),
          totalHours: this.totalHoursFor(d),
          universityGrade: g?.universityGrade ?? null,
          ectsGrade: g?.ectsGrade ?? null,
          nationalGradeLabel: this.nationalLabel(g?.nationalGrade),
          statusLabel: d.status === 'IN_PROGRESS' ? 'Відкрито' : 'Закрито',
          statusCss: `status-${d.status}`,
          result: d.result,
          resultLabel: d.result === 'PASSED' ? 'Зараховано' : d.result === 'FAILED' ? 'Не зараховано' : '—',
          resultCss: d.result ? `result-${d.result}` : '',
          navId: d.entryId,
        };
      });

    const stateLabels: Record<WorkState, string> = { IN_PROGRESS: 'В процесі', COMPLETED: 'Завершено', FAILED: 'Не зараховано' };
    const stateCss: Record<WorkState, string> = { IN_PROGRESS: 'state-progress', COMPLETED: 'state-completed', FAILED: 'state-failed' };
    const resultFromState: Partial<Record<WorkState, string>> = { COMPLETED: 'PASSED', FAILED: 'FAILED' };

    const filteredWorks = this.additionalWorks().filter(w => {
      if (sem === null) return true;
      const wSem = w.courseWorkDetails?.semester ?? null;
      return wSem === sem;
    });
    const attemptCounters = new Map<WorkType, number>();
    const workRows: DisplayRow[] = filteredWorks
      .map(w => {
        const state: WorkState | undefined = w.courseWorkDetails?.state ?? w.qualificationDetails?.state;
        const result = state ? (resultFromState[state] ?? null) : null;
        const needsAttempt = w.type === 'COURSE_WORK' || w.type === 'EDUCATIONAL_PRACTICE' || w.type === 'PRODUCTION_PRACTICE';
        const attempt = needsAttempt
          ? (attemptCounters.set(w.type, (attemptCounters.get(w.type) ?? 0) + 1), attemptCounters.get(w.type)!)
          : null;
        return {
          kind: 'work',
          name: w.title,
          workType: w.type,
          academicYear: '—',
          semester: w.courseWorkDetails?.semester ?? null,
          attempt,
          ectsCredits: w.practiceDetails?.ectsCredits ?? w.courseWorkDetails?.ectsCredits ?? null,
          totalHours: w.courseWorkDetails?.totalHours ?? w.practiceDetails?.totalHours ?? null,
          universityGrade: w.universityGrade ?? null,
          ectsGrade: w.ectsGrade ?? null,
          nationalGradeLabel: this.workNationalLabel(w.nationalGrade),
          statusLabel: state ? stateLabels[state] : '—',
          statusCss: state ? stateCss[state] : '',
          result,
          resultLabel: result === 'PASSED' ? 'Зараховано' : result === 'FAILED' ? 'Не зараховано' : '—',
          resultCss: result ? `result-${result}` : '',
          navId: w.id,
        };
      });

    return [...disciplineRows, ...workRows].sort((a, b) => (a.semester ?? 999) - (b.semester ?? 999));
  });

  studentName = computed(() => {
    const b = this.book();
    if (!b) return '';
    return [b.studentLastName, b.studentFirstName].filter(Boolean).join(' ');
  });

  passedCount = computed(() => this.combinedRows().filter(r => r.result === 'PASSED').length);
  failedCount = computed(() => this.combinedRows().filter(r => r.result === 'FAILED').length);
  inProgressCount = computed(() => this.combinedRows().filter(r => r.statusCss.includes('IN_PROGRESS') || r.statusCss === 'state-progress').length);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('bookNumberId'));
    this.bookNumberId.set(id);

    if (this.isStudent()) {
      const user = this.authState.currentUser();
      this.userService.getMyProfile().subscribe({
        next: (p) => {
          const b = p.books?.find((b) => b.bookId === id);
          if (b) {
            this.book.set({
              id: b.bookId,
              number: b.bookNumber,
              studentId: user?.id ?? 0,
              studentFirstName: user?.firstName,
              studentLastName: user?.lastName,
              status: b.bookNumberStatus as BookNumber['status'],
              regStartDate: b.startDate,
            });
          }
        },
        error: () => {},
      });
    } else {
      this.bookService.findById(id).subscribe((book) => this.book.set(book));
    }

    this.gradeService.getStudentDisciplines(id).subscribe((d) => this.disciplines.set(d));
    this.additionalWorkService.getByBookNumberId(id).subscribe({
      next: (w) => this.additionalWorks.set(w),
      error: () => {},
    });
  }

  toggleSemester(semester: number) {
    this.selectedSemester.set(this.selectedSemester() === semester ? null : semester);
  }

  isSemesterSelected(semester: number): boolean {
    return this.selectedSemester() === semester;
  }

  navigateTo(row: DisplayRow) {
    if (this.isStudent()) return;
    if (row.kind === 'discipline') {
      this.router.navigate(['/grades', row.navId]);
    } else {
      this.router.navigate(['/additional-works', row.navId]);
    }
  }

  goBack() {
    if (this.isStudent()) {
      this.router.navigate(['/profile', this.authState.currentUserId()]);
    } else {
      this.router.navigate(['/books', this.bookNumberId()]);
    }
  }

  goToBook() {
    this.router.navigate(['/books', this.bookNumberId()]);
  }

  openReport() {
    this.router.navigate(['/grades/student', this.bookNumberId(), 'report']);
  }

  private latestGrade(d: StudentDisciplineDTO): GradeDTO | null {
    if (!d.grades?.length) return null;
    return d.grades[d.grades.length - 1];
  }

  private nationalLabel = nationalGradeLabel;
  private workNationalLabel = workNationalGradeLabel;

  private ectsCreditsFor(d: StudentDisciplineDTO): number | null {
    const h = d.hours.find(hr => hr.academicYear === d.academicYear);
    return h?.ectsCredits ?? null;
  }

  private totalHoursFor(d: StudentDisciplineDTO): number | null {
    const h = d.hours.find(hr => hr.academicYear === d.academicYear);
    return h?.totalHours ?? null;
  }

  workTypeLabel(type: WorkType): string {
    const map: Record<WorkType, string> = { COURSE_WORK: 'Курсова робота', EDUCATIONAL_PRACTICE: 'Навчальна практика', PRODUCTION_PRACTICE: 'Виробнича практика', QUALIFICATION: 'Кваліфікаційна робота', COMPREHENSIVE_EXAM: 'Комплексний екзамен' };
    return map[type] ?? type;
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('uk-UA');
  }
}
