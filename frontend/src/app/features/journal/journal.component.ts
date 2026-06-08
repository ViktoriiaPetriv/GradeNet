import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { JournalService } from '../../core/services/journal.service';
import { UserService } from '../../core/services/user.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { OrgService } from '../../core/services/org.service';
import { CommissionService } from '../../core/services/commission.service';
import { ToastService } from '../../core/services/toast.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import {
  JournalSpecialtyDTO,
  JournalStudentStatus,
  JournalDisciplineStatus,
  JournalDisciplineDetail,
  JournalImportResult,
  DEGREE_OPTIONS,
  STUDY_FORM_OPTIONS,
} from '../../models/journal.model';
import { User } from '../../models/user.model';
import { Commission } from '../../models/commission.model';
import { Specialty, SpecialtyOffering, SpecialtyRequest, OrganizationShort, OrgType, Degree, EduType } from '../../models/org.model';

type Step = 'specialty' | 'students' | 'disciplines' | 'grade-view' | 'preview' | 'result';
type SpecialtyLinkStatus = 'checking' | 'linked' | 'no-offering' | 'no-specialty' | null;

interface StudentRow {
  student: JournalStudentStatus;
  selected: boolean;
}

interface DisciplineRow {
  discipline: JournalDisciplineStatus;
  selected: boolean;
  /** attempt number → internal professor user ID */
  professorByAttempt: Record<number, number | null>;
  /** attempt → studentExternalId → professorId (null = use discipline default) */
  studentProfessorOverrides: Record<number, Record<number, number | null>>;
  showStudentOverrides: boolean;
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
  private orgService = inject(OrgService);
  private commissionService = inject(CommissionService);
  private toastService = inject(ToastService);

  step = signal<Step>('specialty');

  // Step 1 – specialty list
  loadingSpecialties = signal(false);
  foundSpecialties = signal<JournalSpecialtyDTO[]>([]);
  specialtiesLoaded = signal(false);

  // Step 2 – specialty
  selectedJournalSpecialty = signal<JournalSpecialtyDTO | null>(null);
  specialtyLinkStatus = signal<SpecialtyLinkStatus>(null);
  matchedInternalSpecialty = signal<Specialty | null>(null);
  createOrgId = signal<number | null>(null);
  orgs = signal<OrganizationShort[]>([]);
  loadingOrgs = signal(false);
  loadingSpecialtyAction = signal(false);

  // Org autocomplete
  orgSearch = signal('');
  showOrgDropdown = signal(false);
  orgDropdownPosition = signal<'above' | 'below'>('below');

  // Step 3 – students
  selectedSpecialtyId = signal<number | null>(null);
  studentRows = signal<StudentRow[]>([]);
  loadingStudents = signal(false);
  studentsError = signal(false);

  // Step 4 – disciplines
  disciplineRows = signal<DisciplineRow[]>([]);
  loadingDisciplines = signal(false);
  disciplinesError = signal(false);
  professors = signal<User[]>([]);
  offerings = signal<SpecialtyOffering[]>([]);
  selectedInternalSpecialtyId = signal<number | null>(null);
  selectedOfferingId = signal<number | null>(null);
  importAcademicYear = signal('');
  loadingImport = signal(false);

  // Professor autocomplete
  profSearches = signal<Record<string, string>>({});
  showProfDropdown = signal<Record<string, boolean>>({});
  profDropdownPositions = signal<Record<string, 'above' | 'below'>>({});

  // Step 4 – grade view
  gradeViewDiscipline = signal<DisciplineRow | null>(null);
  disciplineDetail = signal<JournalDisciplineDetail | null>(null);
  loadingDiscipline = signal(false);

  // Step 5 – preview
  previewDetails = signal<JournalDisciplineDetail[]>([]);
  loadingPreview = signal(false);

  // Commission (for additional work types)
  commissions = signal<Commission[]>([]);
  selectedCommissionId = signal<number | null>(null);

  // Step 6 – result
  importResult = signal<JournalImportResult | null>(null);
  tooltipText = signal('');
  tooltipX = signal(0);
  tooltipY = signal(0);
  tooltipVisible = signal(false);

  private readonly ADDITIONAL_WORK_TYPES = [21, 22, 32, 40];

  // Computed
  selectedStudentRows = computed(() => this.studentRows().filter((r) => r.selected));
  selectedDisciplineRows = computed(() => this.disciplineRows().filter((r) => r.selected));
  allStudentsSelected = computed(() => this.studentRows().every((r) => r.selected));

  hasAdditionalWorkDisciplines = computed(() =>
    this.previewDetails().some(d =>
      d.grades.some(g => this.ADDITIONAL_WORK_TYPES.includes(g.assessmentType))
    )
  );

  canProceedFromSpecialty = computed(() => {
    const status = this.specialtyLinkStatus();
    if (status === 'linked' || status === 'no-offering') return true;
    if (status === 'no-specialty') return this.createOrgId() !== null;
    return false;
  });

  selectedSpecialtyInfo = computed(() => this.matchedInternalSpecialty());

  filteredOrgs = computed(() => {
    const s = this.orgSearch().toLowerCase().trim();
    if (!s) return this.orgs();
    return this.orgs().filter((o) => o.name.toLowerCase().includes(s));
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

    const columns = discRows.flatMap((dr) =>
      (dr.discipline.attempts ?? [1]).map((attempt) => {
        const studentOverrides: Record<number, number | null> = dr.studentProfessorOverrides[attempt] ?? {};
        return {
          discipline: dr.discipline,
          attempt,
          detail: detailByExtId.get(dr.discipline.externalId) ?? null,
          professor: this.professors().find((p) => p.id === (dr.professorByAttempt[attempt] ?? null)) ?? null,
          studentOverrides,
          hasStudentOverrides: Object.values(studentOverrides).some((v) => v !== null),
        };
      })
    );

    return {
      columns,
      rows: studentRows.map((sr) => ({
        student: sr.student,
        cells: columns.map((col) => {
          const grade = gradeMap.get(col.discipline.externalId)?.get(col.attempt)?.get(sr.student.externalId) ?? null;
          const overrideProfId = col.studentOverrides[sr.student.externalId] ?? null;
          const overrideProf = overrideProfId
            ? (this.professors().find((p) => p.id === overrideProfId) ?? null)
            : null;
          return { grade, overrideProf };
        }),
      })),
    };
  });

  ngOnInit() {
    const y = new Date().getFullYear();
    this.importAcademicYear.set(`${y}/${y + 1}`);
    this.loadSpecialties();
    this.commissionService.getAll().subscribe(data => this.commissions.set(data));
  }

  // ── Step 1: Specialty ──────────────────────────────────────────────────────

  loadSpecialties() {
    this.loadingSpecialties.set(true);
    this.foundSpecialties.set([]);
    this.specialtiesLoaded.set(false);
    this.journalService.getSpecialties({}).subscribe({
      next: (specialties) => {
        this.loadingSpecialties.set(false);
        this.foundSpecialties.set(specialties);
        this.specialtiesLoaded.set(true);
        if (specialties.length === 1) {
          this.selectJournalSpecialty(specialties[0]);
        }
      },
      error: () => {
        this.toastService.error('Помилка завантаження спеціальностей');
        this.loadingSpecialties.set(false);
      },
    });
  }

  // ── Step 2: Specialty ──────────────────────────────────────────────────────

  selectJournalSpecialty(specialty: JournalSpecialtyDTO) {
    this.selectedJournalSpecialty.set(specialty);
    this.selectedSpecialtyId.set(specialty.externalId);
    this.specialtyLinkStatus.set('checking');
    this.matchedInternalSpecialty.set(null);
    this.selectedInternalSpecialtyId.set(null);
    this.selectedOfferingId.set(null);
    this.offerings.set([]);
    this.createOrgId.set(null);
    this.orgSearch.set('');
    this.showOrgDropdown.set(false);

    this.specialtyService.getOfferingByExternalId(specialty.externalId).subscribe({
      next: (offering) => {
        if (offering) {
          this.specialtyLinkStatus.set('linked');
          this.selectedOfferingId.set(offering.id);
          this.selectedInternalSpecialtyId.set(offering.specialtyId);
          this.specialtyService.getOfferings(offering.specialtyId).subscribe({
            next: (offs) => this.offerings.set(offs),
            error: () => {},
          });
        } else {
          this.specialtyService.getAll({ size: 200 }).subscribe({
            next: (page) => {
              const match = page.content.find((s) => s.code === specialty.code) ?? null;
              if (match) {
                this.matchedInternalSpecialty.set(match);
                this.selectedInternalSpecialtyId.set(match.id);
                this.specialtyLinkStatus.set('no-offering');
                this.specialtyService.getOfferings(match.id).subscribe({
                  next: (offs) => this.offerings.set(offs),
                  error: () => {},
                });
              } else {
                this.specialtyLinkStatus.set('no-specialty');
                this.loadingOrgs.set(true);
                this.orgService.getAllShort(OrgType.DEPARTMENT).subscribe({
                  next: (list) => { this.orgs.set(list); this.loadingOrgs.set(false); },
                  error: () => { this.toastService.error('Не вдалося завантажити підрозділи'); this.loadingOrgs.set(false); },
                });
              }
            },
            error: () => {
              this.specialtyLinkStatus.set('no-specialty');
              this.toastService.error('Не вдалося перевірити спеціальності');
            },
          });
        }
      },
      error: () => {
        this.specialtyLinkStatus.set(null);
        this.toastService.error('Помилка перевірки зв\'язку спеціальності');
      },
    });
  }

  onOrgSearchChange(value: string) {
    this.orgSearch.set(value);
    this.createOrgId.set(null);
    this.showOrgDropdown.set(true);
  }

  toggleOrgDropdown() {
    const isOpening = !this.showOrgDropdown();
    if (isOpening) {
      setTimeout(() => {
        const el = document.querySelector('[data-org-autocomplete]');
        if (!el) return;
        const rect = el.getBoundingClientRect();
        this.orgDropdownPosition.set(window.innerHeight - rect.bottom < 250 ? 'above' : 'below');
      }, 0);
    }
    this.showOrgDropdown.set(!this.showOrgDropdown());
  }

  closeOrgDropdown() {
    setTimeout(() => this.showOrgDropdown.set(false), 150);
  }

  assignOrg(org: OrganizationShort) {
    this.createOrgId.set(org.id);
    this.orgSearch.set(org.name);
    this.showOrgDropdown.set(false);
  }

  proceedFromSpecialty() {
    const status = this.specialtyLinkStatus();
    const js = this.selectedJournalSpecialty();
    if (!js) return;

    if (status === 'linked') {
      this.loadProfessors();
      this.goToStudents();
      return;
    }

    if (status === 'no-offering') {
      const matched = this.matchedInternalSpecialty();
      if (!matched) return;
      this.loadingSpecialtyAction.set(true);
      this.specialtyService.createOffering({
        specialtyId: matched.id,
        externalId: js.externalId,
        graduationYear: js.graduationYear,
      }).subscribe({
        next: (offering) => {
          this.selectedOfferingId.set(offering.id);
          this.specialtyLinkStatus.set('linked');
          this.loadingSpecialtyAction.set(false);
          this.loadProfessors();
          this.goToStudents();
        },
        error: () => {
          this.toastService.error('Не вдалося створити пропозицію спеціальності');
          this.loadingSpecialtyAction.set(false);
        },
      });
      return;
    }

    if (status === 'no-specialty') {
      const orgId = this.createOrgId();
      if (!orgId) return;
      this.loadingSpecialtyAction.set(true);
      this.specialtyService.create(this.buildSpecialtyRequest(js, orgId)).subscribe({
        next: (newSpec) => {
          this.matchedInternalSpecialty.set(newSpec);
          this.selectedInternalSpecialtyId.set(newSpec.id);
          this.specialtyService.createOffering({
            specialtyId: newSpec.id,
            externalId: js.externalId,
            graduationYear: js.graduationYear,
          }).subscribe({
            next: (offering) => {
              this.selectedOfferingId.set(offering.id);
              this.specialtyLinkStatus.set('linked');
              this.loadingSpecialtyAction.set(false);
              this.loadProfessors();
              this.goToStudents();
            },
            error: () => {
              this.toastService.error('Не вдалося створити пропозицію спеціальності');
              this.loadingSpecialtyAction.set(false);
            },
          });
        },
        error: () => {
          this.toastService.error('Не вдалося створити спеціальність');
          this.loadingSpecialtyAction.set(false);
        },
      });
    }
  }

  private buildSpecialtyRequest(js: JournalSpecialtyDTO, orgId: number): SpecialtyRequest {
    const degreeMap: Record<string, Degree> = {
      bachelor: Degree.BACHELOR,
      master: Degree.MASTER,
      specialist: Degree.SPECIALIST,
      doctor: Degree.DOCTOR,
    };
    const eduTypeMap: Record<string, EduType> = {
      full_time: EduType.FULL_TIME,
      part_time: EduType.PART_TIME,
    };
    const studyLengthMap: Record<string, number> = {
      bachelor: 4, master: 2, specialist: 5, doctor: 3,
    };
    const degree = degreeMap[js.degree?.toLowerCase()] ?? Degree.BACHELOR;
    const eduType = eduTypeMap[js.studyForm?.toLowerCase()] ?? EduType.FULL_TIME;
    const length = studyLengthMap[js.degree?.toLowerCase()] ?? 4;
    const startYear = js.graduationYear - length;
    return {
      code: js.code,
      nameUA: js.name,
      nameEN: js.name,
      studyProgramUA: js.name,
      studyProgramEN: js.name,
      eduProgramUA: js.name,
      eduProgramEN: js.name,
      orgId,
      degree,
      eduType,
      startDate: `${startYear}-09-01T00:00:00Z`,
      endDate: null,
    };
  }

  // ── Step 3: Students ───────────────────────────────────────────────────────

  goToStudents() {
    const id = this.selectedSpecialtyId();
    if (!id) return;
    this.studentRows.set([]);
    this.studentsError.set(false);
    this.loadingStudents.set(true);
    this.step.set('students');
    this.journalService.getStudentsWithStatus(id).subscribe({
      next: (students) => {
        this.studentRows.set(students.map((s) => ({ student: s, selected: true })));
        this.loadingStudents.set(false);
      },
      error: () => {
        this.studentsError.set(true);
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

  // ── Step 4: Disciplines ────────────────────────────────────────────────────

  goToDisciplines() {
    const id = this.selectedSpecialtyId();
    if (!id) return;
    this.disciplineRows.set([]);
    this.disciplinesError.set(false);
    this.loadingDisciplines.set(true);
    this.step.set('disciplines');

    this.journalService.getDisciplinesWithStatus(id).subscribe({
      next: (disciplines) => {
        this.disciplineRows.set(
          disciplines.map((d) => ({
            discipline: d,
            selected: true,
            professorByAttempt: Object.fromEntries((d.attempts ?? [1]).map((a) => [a, null])),
            studentProfessorOverrides: {},
            showStudentOverrides: false,
          }))
        );
        const yearFromJournal = disciplines.find((d) => d.academicYear)?.academicYear;
        if (yearFromJournal) this.importAcademicYear.set(yearFromJournal);
        this.loadingDisciplines.set(false);
      },
      error: () => {
        this.disciplinesError.set(true);
        this.loadingDisciplines.set(false);
      },
    });
  }

  private loadProfessors() {
    if (this.professors().length > 0) return;
    this.userService.getProfessors().subscribe({
      next: (list) => this.professors.set(list),
      error: () => this.toastService.error('Не вдалося завантажити список викладачів'),
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

  profKey(externalId: number, attempt: number): string {
    return `${externalId}_${attempt}`;
  }

  getFilteredProfessors(search: string): User[] {
    const s = search.toLowerCase().trim();
    if (!s) return this.professors();
    return this.professors().filter((p) =>
      this.professorFullName(p).toLowerCase().includes(s)
    );
  }

  onProfSearchChange(key: string, value: string) {
    this.profSearches.update((m) => ({ ...m, [key]: value }));
    this.showProfDropdown.update((m) => ({ ...m, [key]: true }));
  }

  toggleProfDropdown(key: string) {
    const isOpening = !this.showProfDropdown()[key];
    if (isOpening) {
      setTimeout(() => this.calculateProfDropdownPosition(key), 0);
    }
    this.showProfDropdown.update((m) => ({ ...m, [key]: !m[key] }));
  }

  closeProfDropdown(key: string) {
    setTimeout(() => this.showProfDropdown.update((m) => ({ ...m, [key]: false })), 150);
  }

  assignProfessor(row: DisciplineRow, attempt: number, professorId: number | null) {
    this.setProfessor(row, attempt, professorId);
    const key = this.profKey(row.discipline.externalId, attempt);
    const prof = professorId ? this.professors().find((p) => p.id === professorId) : null;
    this.profSearches.update((m) => ({ ...m, [key]: prof ? this.professorFullName(prof) : '' }));
    this.showProfDropdown.update((m) => ({ ...m, [key]: false }));
  }

  studentProfKey(disciplineExtId: number, attempt: number, studentExtId: number): string {
    return `s_${disciplineExtId}_${attempt}_${studentExtId}`;
  }

  assignStudentProfessor(disciplineExtId: number, attempt: number, studentExtId: number, professorId: number | null) {
    this.disciplineRows.update((rows) =>
      rows.map((r) =>
        r.discipline.externalId === disciplineExtId
          ? {
              ...r,
              studentProfessorOverrides: {
                ...r.studentProfessorOverrides,
                [attempt]: { ...(r.studentProfessorOverrides[attempt] ?? {}), [studentExtId]: professorId },
              },
            }
          : r
      )
    );
    const key = this.studentProfKey(disciplineExtId, attempt, studentExtId);
    const prof = professorId ? this.professors().find((p) => p.id === professorId) : null;
    this.profSearches.update((m) => ({ ...m, [key]: prof ? this.professorFullName(prof) : '' }));
    this.showProfDropdown.update((m) => ({ ...m, [key]: false }));
  }

  toggleStudentOverrides(externalId: number) {
    this.disciplineRows.update((rows) =>
      rows.map((r) =>
        r.discipline.externalId === externalId
          ? { ...r, showStudentOverrides: !r.showStudentOverrides }
          : r
      )
    );
  }

  getStudentOverrideProfessor(disciplineExtId: number, attempt: number, studentExtId: number): number | null {
    const row = this.disciplineRows().find((r) => r.discipline.externalId === disciplineExtId);
    return row?.studentProfessorOverrides[attempt]?.[studentExtId] ?? null;
  }

  private calculateProfDropdownPosition(key: string) {
    const element = document.querySelector(`[data-prof-key="${key}"]`);
    if (!element) return;
    const rect = element.getBoundingClientRect();
    const position = window.innerHeight - rect.bottom < 250 ? 'above' : 'below';
    this.profDropdownPositions.update((m) => ({ ...m, [key]: position }));
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
    const professorOverridesByStudent: Record<number, Record<number, Record<number, number>>> = {};
    for (const row of this.selectedDisciplineRows()) {
      const byAttempt: Record<number, number> = {};
      for (const [attempt, profId] of Object.entries(row.professorByAttempt)) {
        if (profId !== null) byAttempt[+attempt] = profId;
      }
      professorByDisciplineId[row.discipline.externalId] = byAttempt;

      for (const [attempt, studentMap] of Object.entries(row.studentProfessorOverrides)) {
        for (const [studentId, profId] of Object.entries(studentMap)) {
          if (profId !== null) {
            if (!professorOverridesByStudent[row.discipline.externalId])
              professorOverridesByStudent[row.discipline.externalId] = {};
            if (!professorOverridesByStudent[row.discipline.externalId][+attempt])
              professorOverridesByStudent[row.discipline.externalId][+attempt] = {};
            professorOverridesByStudent[row.discipline.externalId][+attempt][+studentId] = profId;
          }
        }
      }
    }

    const selectedStudentExternalIds = this.selectedStudentRows().map((r) => r.student.externalId);

    this.loadingImport.set(true);
    const commissionId = this.selectedCommissionId();
    this.journalService
      .importFromJournal({
        journalSpecialtyId: specialtyId,
        specialtyOfferingId: offeringId,
        academicYear: this.importAcademicYear(),
        professorByDisciplineId,
        professorOverridesByStudent: Object.keys(professorOverridesByStudent).length
          ? professorOverridesByStudent : undefined,
        selectedStudentExternalIds,
        commissionId: commissionId ?? undefined,
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
    this.step.set('specialty');
    this.selectedJournalSpecialty.set(null);
    this.specialtyLinkStatus.set(null);
    this.matchedInternalSpecialty.set(null);
    this.createOrgId.set(null);
    this.orgSearch.set('');
    this.showOrgDropdown.set(false);
  }

  goToDisciplinesStep() {
    this.step.set('disciplines');
  }

  reset() {
    this.step.set('specialty');
    this.foundSpecialties.set([]);
    this.specialtiesLoaded.set(false);
    this.selectedJournalSpecialty.set(null);
    this.specialtyLinkStatus.set(null);
    this.matchedInternalSpecialty.set(null);
    this.createOrgId.set(null);
    this.orgSearch.set('');
    this.showOrgDropdown.set(false);
    this.orgs.set([]);
    this.selectedSpecialtyId.set(null);
    this.studentRows.set([]);
    this.studentsError.set(false);
    this.disciplineRows.set([]);
    this.disciplinesError.set(false);
    this.selectedInternalSpecialtyId.set(null);
    this.selectedOfferingId.set(null);
    this.offerings.set([]);
    this.importResult.set(null);
    this.gradeViewDiscipline.set(null);
    this.disciplineDetail.set(null);
    this.previewDetails.set([]);
    const y = new Date().getFullYear();
    this.importAcademicYear.set(`${y}/${y + 1}`);
    this.loadSpecialties();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  fullName(s: { firstName: string; lastName: string; patronymic: string }): string {
    return `${s.lastName} ${s.firstName} ${s.patronymic}`;
  }

  professorFullName(u: User): string {
    return `${u.lastName} ${u.firstName}${u.patronymic ? ' ' + u.patronymic : ''}`;
  }

  formatCommissionDate(date: string): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }

  degreeLabel(value: string) {
    return DEGREE_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  studyFormLabel(value: string) {
    return STUDY_FORM_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  cleanReason(reason: string): string {
    // strip leading "400 BAD_REQUEST: " prefix
    let s = reason.replace(/^\d{3,}[^:]*:\s*/, '').trim();
    // strip surrounding escaped or plain quotes: \"...\", "..."
    s = s.replace(/^\\?"(.*?)\\?"$/, '$1').trim();
    // if JSON object, extract "message" field
    if (s.startsWith('{')) {
      try {
        const obj = JSON.parse(s);
        s = obj.message ?? obj.error ?? s;
      } catch {}
    }
    return s;
  }

  showErrorTooltip(event: MouseEvent, reason: string) {
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    this.tooltipText.set(this.cleanReason(reason));
    this.tooltipX.set(rect.left + rect.width / 2);
    this.tooltipY.set(rect.top - 8);
    this.tooltipVisible.set(true);
  }

  hideErrorTooltip() {
    this.tooltipVisible.set(false);
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
