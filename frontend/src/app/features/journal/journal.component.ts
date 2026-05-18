import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';
import { JournalService } from '../../core/services/journal.service';
import { UserService } from '../../core/services/user.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { ToastService } from '../../core/services/toast.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import {
  JournalStudentStatus,
  JournalDisciplineStatus,
  JournalDisciplineDetail,
  JournalImportResult,
  DEGREE_OPTIONS,
  STUDY_FORM_OPTIONS,
} from '../../models/journal.model';
import { User } from '../../models/user.model';
import { Specialty, SpecialtyOffering } from '../../models/org.model';

type Step = 'search' | 'students' | 'disciplines' | 'grade-view' | 'preview' | 'result';

interface StudentRow {
  student: JournalStudentStatus;
  selected: boolean;
}

interface DisciplineRow {
  discipline: JournalDisciplineStatus;
  selected: boolean;
  /** attempt number → internal professor user ID */
  professorByAttempt: Record<number, number | null>;
}

@Component({
  selector: 'app-journal',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './journal.component.html',
  styleUrl: './journal.component.css',
})
export class JournalComponent implements OnInit {
  private journalService = inject(JournalService);
  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);

  readonly degreeOptions = DEGREE_OPTIONS;
  readonly studyFormOptions = STUDY_FORM_OPTIONS;

  step = signal<Step>('search');

  // Step 1 – search filters
  filterDegree = signal('');
  filterYear = signal<number | null>(null);
  filterStudyForm = signal('');
  filterCode = signal('');
  loadingSpecialties = signal(false);
  foundIds = signal<number[]>([]);
  searched = signal(false);

  // Selected specialty
  selectedSpecialtyId = signal<number | null>(null);

  // Step 2 – students
  studentRows = signal<StudentRow[]>([]);
  loadingStudents = signal(false);

  // Step 3 – disciplines
  disciplineRows = signal<DisciplineRow[]>([]);
  loadingDisciplines = signal(false);
  professors = signal<User[]>([]);
  internalSpecialties = signal<Specialty[]>([]);
  offerings = signal<SpecialtyOffering[]>([]);
  selectedInternalSpecialtyId = signal<number | null>(null);
  selectedOfferingId = signal<number | null>(null);
  importAcademicYear = signal('');
  importSpecialtySearch = signal('');
  loadingImport = signal(false);

  // Step 4 – grade view
  gradeViewDiscipline = signal<DisciplineRow | null>(null);
  disciplineDetail = signal<JournalDisciplineDetail | null>(null);
  loadingDiscipline = signal(false);

  // Step 5 – preview
  previewDetails = signal<JournalDisciplineDetail[]>([]);
  loadingPreview = signal(false);

  // Step 6 – result
  importResult = signal<JournalImportResult | null>(null);

  // Computed
  selectedStudentRows = computed(() => this.studentRows().filter((r) => r.selected));
  selectedDisciplineRows = computed(() => this.disciplineRows().filter((r) => r.selected));
  allStudentsSelected = computed(() => this.studentRows().every((r) => r.selected));

  filteredInternalSpecialties = computed(() => {
    const q = this.importSpecialtySearch().toLowerCase().trim();
    return q
      ? this.internalSpecialties().filter((s) => s.nameUA.toLowerCase().includes(q))
      : this.internalSpecialties();
  });

  canImport = computed(
    () =>
      this.selectedOfferingId() !== null &&
      this.importAcademicYear().trim().length > 0 &&
      this.selectedDisciplineRows().length > 0 &&
      this.selectedDisciplineRows().every((r) =>
        Object.values(r.professorByAttempt).every((v) => v !== null)
      )
  );

  gradeRows = computed(() => {
    const detail = this.disciplineDetail();
    if (!detail) return [];
    const selectedIds = new Set(this.selectedStudentRows().map((r) => r.student.externalId));
    return detail.grades
      .filter((g) => selectedIds.size === 0 || selectedIds.has(g.studentExternalId))
      .map((g) => ({
        student: this.studentRows().find((r) => r.student.externalId === g.studentExternalId)?.student ?? null,
        grade: g,
      }));
  });

  previewTable = computed(() => {
    const details = this.previewDetails();
    const discRows = this.selectedDisciplineRows();
    const studentRows = this.selectedStudentRows();
    if (!details.length || !discRows.length) return null;

    const detailByExtId = new Map(details.map((d) => [d.externalId, d]));

    // gradeMap: disciplineId → attempt → studentExternalId → grade
    const gradeMap = new Map<number, Map<number, Map<number, (typeof details)[0]['grades'][0]>>>();
    for (const d of details) {
      const byAttempt = new Map<number, Map<number, (typeof d.grades)[0]>>();
      for (const g of d.grades) {
        const att = g.attempt ?? 1;
        if (!byAttempt.has(att)) byAttempt.set(att, new Map());
        byAttempt.get(att)!.set(g.studentExternalId, g);
      }
      gradeMap.set(d.externalId, byAttempt);
    }

    // one column per (discipline, attempt)
    const columns = discRows.flatMap((dr) =>
      (dr.discipline.attempts ?? [1]).map((attempt) => ({
        discipline: dr.discipline,
        attempt,
        detail: detailByExtId.get(dr.discipline.externalId) ?? null,
        professor: this.professors().find((p) => p.id === (dr.professorByAttempt[attempt] ?? null)) ?? null,
      }))
    );

    return {
      columns,
      rows: studentRows.map((sr) => ({
        student: sr.student,
        cells: columns.map((col) =>
          gradeMap.get(col.discipline.externalId)?.get(col.attempt)?.get(sr.student.externalId) ?? null
        ),
      })),
    };
  });

  ngOnInit() {
    const y = new Date().getFullYear();
    this.importAcademicYear.set(`${y}/${y + 1}`);
  }

  // ── Step 1: Search ─────────────────────────────────────────────────────────

  searchSpecialties() {
    this.loadingSpecialties.set(true);
    this.foundIds.set([]);
    this.searched.set(false);
    this.journalService
      .getSpecialties({
        degree: this.filterDegree() || undefined,
        graduationYear: this.filterYear() ?? undefined,
        studyForm: this.filterStudyForm() || undefined,
        code: this.filterCode() || undefined,
      })
      .subscribe({
        next: (ids) => {
          this.loadingSpecialties.set(false);
          if (ids.length === 1) {
            this.loadStudents(ids[0]);
          } else {
            this.foundIds.set(ids);
            this.searched.set(true);
          }
        },
        error: () => {
          this.toastService.error('Помилка пошуку спеціальності');
          this.loadingSpecialties.set(false);
        },
      });
  }

  // ── Step 2: Students ───────────────────────────────────────────────────────

  loadStudents(id: number) {
    this.selectedSpecialtyId.set(id);
    this.studentRows.set([]);
    this.loadingStudents.set(true);
    this.step.set('students');
    this.journalService.getStudentsWithStatus(id).subscribe({
      next: (students) => {
        this.studentRows.set(students.map((s) => ({ student: s, selected: true })));
        this.loadingStudents.set(false);
      },
      error: () => {
        this.toastService.error('Помилка завантаження студентів');
        this.loadingStudents.set(false);
      },
    });
  }

  toggleStudent(row: StudentRow) {
    this.studentRows.update((rows) =>
      rows.map((r) => (r === row ? { ...r, selected: !r.selected } : r))
    );
  }

  toggleAllStudents(checked: boolean) {
    this.studentRows.update((rows) => rows.map((r) => ({ ...r, selected: checked })));
  }

  // ── Step 3: Disciplines ────────────────────────────────────────────────────

  goToDisciplines() {
    const id = this.selectedSpecialtyId();
    if (!id) return;
    this.disciplineRows.set([]);
    this.loadingDisciplines.set(true);
    this.step.set('disciplines');
    if (this.professors().length === 0) this.loadProfessors();
    if (this.internalSpecialties().length === 0) this.loadInternalSpecialties();

    // Auto-identify offering by journal externalId
    this.specialtyService.getOfferingByExternalId(id).pipe(
      switchMap((offering) => {
        if (offering) {
          this.selectedOfferingId.set(offering.id);
          this.selectedInternalSpecialtyId.set(offering.specialtyId);
          return this.specialtyService.getOfferings(offering.specialtyId);
        }
        return of([]);
      })
    ).subscribe({
      next: (offs) => { if (offs.length) this.offerings.set(offs); },
      error: () => {},
    });

    this.journalService.getDisciplinesWithStatus(id).subscribe({
      next: (disciplines) => {
        this.disciplineRows.set(
          disciplines.map((d) => ({
            discipline: d,
            selected: true,
            professorByAttempt: Object.fromEntries((d.attempts ?? [1]).map((a) => [a, null])),
          }))
        );
        const yearFromJournal = disciplines.find((d) => d.academicYear)?.academicYear;
        if (yearFromJournal) this.importAcademicYear.set(yearFromJournal);
        this.loadingDisciplines.set(false);
      },
      error: () => {
        this.toastService.error('Помилка завантаження дисциплін');
        this.loadingDisciplines.set(false);
      },
    });
  }

  private loadProfessors() {
    this.userService.getProfessors().subscribe({
      next: (list) => this.professors.set(list),
      error: () => this.toastService.error('Не вдалося завантажити список викладачів'),
    });
  }

  private loadInternalSpecialties() {
    this.specialtyService.getAll({ size: 200 }).subscribe({
      next: (page) => this.internalSpecialties.set(page.content),
      error: () => this.toastService.error('Не вдалося завантажити спеціальності'),
    });
  }

  onInternalSpecialtyChange(specialtyId: number | null) {
    this.selectedInternalSpecialtyId.set(specialtyId);
    this.selectedOfferingId.set(null);
    this.offerings.set([]);
    if (!specialtyId) return;
    this.specialtyService.getOfferings(specialtyId).subscribe({
      next: (list) => this.offerings.set(list),
      error: () => this.toastService.error('Не вдалося завантажити пропозиції спеціальності'),
    });
  }

  toggleDiscipline(row: DisciplineRow) {
    this.disciplineRows.update((rows) =>
      rows.map((r) => (r === row ? { ...r, selected: !r.selected } : r))
    );
  }

  setProfessor(row: DisciplineRow, attempt: number, professorId: number | null) {
    this.disciplineRows.update((rows) =>
      rows.map((r) =>
        r === row
          ? { ...r, professorByAttempt: { ...r.professorByAttempt, [attempt]: professorId } }
          : r
      )
    );
  }

  // ── Step 4: Grade view ─────────────────────────────────────────────────────

  viewDisciplineGrades(row: DisciplineRow) {
    this.gradeViewDiscipline.set(row);
    this.disciplineDetail.set(null);
    this.loadingDiscipline.set(true);
    this.step.set('grade-view');
    this.journalService.getDisciplineDetail(row.discipline.externalId).subscribe({
      next: (detail) => {
        this.disciplineDetail.set(detail);
        this.loadingDiscipline.set(false);
      },
      error: () => {
        this.toastService.error('Помилка завантаження оцінок');
        this.loadingDiscipline.set(false);
      },
    });
  }

  // ── Step 5: Preview ────────────────────────────────────────────────────────

  goToPreview() {
    const selected = this.selectedDisciplineRows();
    if (!selected.length) return;
    this.previewDetails.set([]);
    this.loadingPreview.set(true);
    this.step.set('preview');
    forkJoin(selected.map((r) => this.journalService.getDisciplineDetail(r.discipline.externalId))).subscribe({
      next: (details) => {
        this.previewDetails.set(details);
        this.loadingPreview.set(false);
      },
      error: () => {
        this.toastService.error('Помилка завантаження деталей дисциплін');
        this.loadingPreview.set(false);
      },
    });
  }

  // ── Import ─────────────────────────────────────────────────────────────────

  executeImport() {
    const specialtyId = this.selectedSpecialtyId();
    const offeringId = this.selectedOfferingId();
    if (!specialtyId || !offeringId) return;

    const professorByDisciplineId: Record<number, Record<number, number>> = {};
    for (const row of this.selectedDisciplineRows()) {
      const byAttempt: Record<number, number> = {};
      for (const [attempt, profId] of Object.entries(row.professorByAttempt)) {
        if (profId !== null) byAttempt[+attempt] = profId;
      }
      professorByDisciplineId[row.discipline.externalId] = byAttempt;
    }

    const selectedStudentExternalIds = this.selectedStudentRows().map((r) => r.student.externalId);

    this.loadingImport.set(true);
    this.journalService
      .importFromJournal({
        journalSpecialtyId: specialtyId,
        specialtyOfferingId: offeringId,
        academicYear: this.importAcademicYear(),
        professorByDisciplineId,
        selectedStudentExternalIds,
      })
      .subscribe({
        next: (result) => {
          this.importResult.set(result);
          this.loadingImport.set(false);
          this.step.set('result');
        },
        error: () => {
          this.toastService.error('Помилка під час імпорту');
          this.loadingImport.set(false);
        },
      });
  }

  // ── Navigation ─────────────────────────────────────────────────────────────

  goToSearch() {
    this.step.set('search');
    this.foundIds.set([]);
    this.searched.set(false);
  }

  goToStudents() {
    this.step.set('students');
  }

  goToDisciplinesStep() {
    this.step.set('disciplines');
  }

  reset() {
    this.step.set('search');
    this.foundIds.set([]);
    this.searched.set(false);
    this.selectedSpecialtyId.set(null);
    this.studentRows.set([]);
    this.disciplineRows.set([]);
    this.selectedInternalSpecialtyId.set(null);
    this.selectedOfferingId.set(null);
    this.offerings.set([]);
    this.importResult.set(null);
    this.gradeViewDiscipline.set(null);
    this.disciplineDetail.set(null);
    this.previewDetails.set([]);
    const y = new Date().getFullYear();
    this.importAcademicYear.set(`${y}/${y + 1}`);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  fullName(s: { firstName: string; lastName: string; patronymic: string }): string {
    return `${s.lastName} ${s.firstName} ${s.patronymic}`;
  }

  professorFullName(u: User): string {
    return `${u.lastName} ${u.firstName}${u.patronymic ? ' ' + u.patronymic : ''}`;
  }

  yearOptions(): number[] {
    const y = new Date().getFullYear();
    return Array.from({ length: 8 }, (_, i) => y - 2 + i);
  }

  degreeLabel(value: string) {
    return DEGREE_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  studyFormLabel(value: string) {
    return STUDY_FORM_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  gradeClass(grade: number | null): string {
    if (grade === null) return '';
    if (grade >= 90) return 'grade-a';
    if (grade >= 75) return 'grade-b';
    if (grade >= 60) return 'grade-c';
    if (grade >= 35) return 'grade-d';
    return 'grade-f';
  }
}
