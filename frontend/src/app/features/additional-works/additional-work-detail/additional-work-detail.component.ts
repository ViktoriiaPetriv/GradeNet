import { Component, OnInit, OnDestroy, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AdditionalWorkService } from '../../../core/services/additional-work.service';
import { CommissionService } from '../../../core/services/commission.service';
import { UserService } from '../../../core/services/user.service';
import { BookService } from '../../../core/services/book.service';
import { AdditionalWork, WorkType, WorkState } from '../../../models/additional-work.model';
import { Commission } from '../../../models/commission.model';
import { BookNumber } from '../../../models/book.model';
import { User } from '../../../models/user.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { HeroCardComponent } from '../../../shared/hero-card/hero-card.component';
import { InfoCardComponent } from '../../../shared/info-card/info-card.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { AdditionalWorkModalComponent } from '../additional-work-modal/additional-work-modal.component';
import { workNationalGradeLabel, WORK_NATIONAL_GRADE_LABELS } from '../../../shared/grade-labels';

@Component({
  selector: 'app-additional-work-detail',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, HeroCardComponent, InfoCardComponent, ModalComponent, ConfirmDialogComponent, AdditionalWorkModalComponent, ReactiveFormsModule],
  templateUrl: './additional-work-detail.component.html',
  styleUrl: './additional-work-detail.component.css',
})
export class AdditionalWorkDetailComponent implements OnInit, OnDestroy {
  work = signal<AdditionalWork | null>(null);
  commissions = signal<Commission[]>([]);
  professors = signal<User[]>([]);
  bookInfo = signal<BookNumber | null>(null);

  editModalOpen = signal(false);
  deleteModalOpen = signal(false);
  gradeModalOpen = signal(false);

  gradeForm!: FormGroup;
  private destroy$ = new Subject<void>();

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private workService = inject(AdditionalWorkService);
  private commissionService = inject(CommissionService);
  private userService = inject(UserService);
  private bookService = inject(BookService);
  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);
  private fb = inject(FormBuilder);

  isAdminOrManager = this.authState.isAdminOrManager;
  isProfessor = this.authState.isProfessor;

  canGrade = computed(() => {
    const w = this.work();
    if (!w) return false;
    if (this.authState.isAdmin()) return true;
    const commission = this.commissions().find(c => c.id === w.commissionId);
    if (!commission) return false;
    if (this.authState.isManager()) {
      return commission.orgId != null && commission.orgId === this.authState.managerFacultyId();
    }
    if (this.authState.isProfessor()) {
      const uid = this.authState.currentUserId();
      return commission.members.some(m => m.professorId === uid);
    }
    return false;
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
    this.commissionService.getAll().subscribe(data => this.commissions.set(data));
    this.userService.getProfessors().subscribe(data => this.professors.set(data));
  }

  load(id: number) {
    this.workService.getById(id).subscribe({
      next: data => {
        this.work.set(data);
        this.bookService.findById(data.bookNumberId).subscribe(b => this.bookInfo.set(b));
      },
      error: () => this.router.navigate(['/additional-works']),
    });
  }

  openEdit() { this.editModalOpen.set(true); }
  closeEdit() { this.editModalOpen.set(false); }

  onSaved() {
    this.closeEdit();
    this.load(this.work()!.id);
  }

  openDelete() { this.deleteModalOpen.set(true); }
  closeDelete() { this.deleteModalOpen.set(false); }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openGrade() {
    const w = this.work()!;
    this.gradeForm = this.fb.group({
      universityGrade: [w.universityGrade ?? null],
      ectsGrade:       [w.ectsGrade ?? null],
      nationalGrade:   [w.nationalGrade ?? null],
    });
    this.gradeForm.get('universityGrade')!.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(score => {
        const c = this.convertGrade(score, this.work()!.type);
        this.gradeForm.patchValue({ ectsGrade: c.ects, nationalGrade: c.national }, { emitEvent: false });
      });
    this.gradeModalOpen.set(true);
  }

  closeGrade() { this.gradeModalOpen.set(false); }

  submitGrade() {
    const w = this.work()!;
    const { universityGrade, ectsGrade, nationalGrade } = this.gradeForm.value;
    this.workService.grade(w.id, universityGrade, ectsGrade, nationalGrade).subscribe({
      next: () => {
        this.toastService.success('Оцінку виставлено');
        this.load(w.id);
        this.closeGrade();
      },
      error: (err) => this.toastService.error(err?.error?.message || 'Доступ заборонено'),
    });
  }

  convertGrade(score: number | null | '', type: WorkType): { ects: string | null; national: string | null } {
    if (score == null || score === '' || isNaN(Number(score))) return { ects: null, national: null };
    const s = Number(score);

    let ects: string;
    if (s >= 90)      ects = 'A';
    else if (s >= 80) ects = 'B';
    else if (s >= 70) ects = 'C';
    else if (s >= 60) ects = 'D';
    else if (s >= 50) ects = 'E';
    else if (s >= 26) ects = 'FE';
    else              ects = 'F';

    let national: string;
    if (type === 'EDUCATIONAL_PRACTICE' || type === 'PRODUCTION_PRACTICE') {
      national = s >= 50 ? 'PASSED' : 'NOT_PASSED';
    } else {
      if (s >= 90)      national = 'FIVE';
      else if (s >= 70) national = 'FOUR';
      else if (s >= 50) national = 'THREE';
      else              national = 'TWO';
    }

    return { ects, national };
  }

  get derivedEcts():     string | null { return this.gradeForm?.get('ectsGrade')?.value ?? null; }
  get derivedNational(): string | null { return this.gradeForm?.get('nationalGrade')?.value ?? null; }

  readonly nationalLabels = WORK_NATIONAL_GRADE_LABELS;

  confirmDelete() {
    const id = this.work()?.id;
    if (!id) return;
    this.workService.delete(id).subscribe(() => {
      this.toastService.success('Роботу видалено');
      this.router.navigate(['/additional-works']);
    });
  }

  currentState(): WorkState | undefined {
    const w = this.work();
    if (!w) return undefined;
    return w.courseWorkDetails?.state ?? w.qualificationDetails?.state;
  }

  typeAvatarColor(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: '#3B82F6',
      EDUCATIONAL_PRACTICE: '#0D9E6E',
      PRODUCTION_PRACTICE: '#0B7A56',
      COMPREHENSIVE_EXAM: '#7C3AED',
      QUALIFICATION: '#7C3AED',
    };
    return map[type] ?? '#5B6AF0';
  }

  typeChipClass(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'chip-course',
      EDUCATIONAL_PRACTICE: 'chip-practice',
      PRODUCTION_PRACTICE: 'chip-practice',
      COMPREHENSIVE_EXAM: 'chip-qualification',
      QUALIFICATION: 'chip-qualification',
    };
    return map[type] ?? '';
  }

  typeIcon(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'pi pi-book',
      EDUCATIONAL_PRACTICE: 'pi pi-briefcase',
      PRODUCTION_PRACTICE: 'pi pi-briefcase',
      COMPREHENSIVE_EXAM: 'pi pi-star',
      QUALIFICATION: 'pi pi-star',
    };
    return map[type] ?? 'pi pi-file';
  }

  stateChipClass(state: WorkState | undefined): string {
    if (!state) return '';
    const map: Record<WorkState, string> = { IN_PROGRESS: 'chip-progress', COMPLETED: 'chip-completed', FAILED: 'chip-failed' };
    return map[state] ?? '';
  }

  typeLabel(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'Курсова робота',
      EDUCATIONAL_PRACTICE: 'Навчальна практика',
      PRODUCTION_PRACTICE: 'Виробнича практика',
      COMPREHENSIVE_EXAM: 'Комплексний екзамен',
      QUALIFICATION: 'Кваліфікаційна робота',
    };
    return map[type] ?? type;
  }

  typeClass(type: WorkType): string {
    const map: Record<WorkType, string> = {
      COURSE_WORK: 'badge-course',
      EDUCATIONAL_PRACTICE: 'badge-practice',
      PRODUCTION_PRACTICE: 'badge-practice',
      COMPREHENSIVE_EXAM: 'badge-qualification',
      QUALIFICATION: 'badge-qualification',
    };
    return map[type] ?? '';
  }

  stateLabel(state: WorkState | undefined): string {
    if (!state) return '';
    const map: Record<WorkState, string> = {
      IN_PROGRESS: 'В процесі',
      COMPLETED: 'Завершено',
      FAILED: 'Не зараховано',
    };
    return map[state] ?? state;
  }

  stateClass(state: WorkState): string {
    const map: Record<WorkState, string> = {
      IN_PROGRESS: 'state-progress',
      COMPLETED: 'state-completed',
      FAILED: 'state-failed',
    };
    return map[state] ?? '';
  }

  professorName(id: number): string {
    const p = this.professors().find(p => p.id === id);
    if (!p) return `ID: ${id}`;
    return `${p.lastName} ${p.firstName}${p.patronymic ? ' ' + p.patronymic : ''}`;
  }

  commissionLabel(commissionId: number): string {
    const c = this.commissions().find(c => c.id === commissionId);
    if (!c) return `#${commissionId}`;
    return `${this.formatDate(c.startDate)} — ${this.formatDate(c.endDate)}`;
  }

  isCommissionActive(commissionId: number): boolean {
    const c = this.commissions().find(c => c.id === commissionId);
    if (!c) return false;
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  formatDate(date: string | undefined): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }

  nationalLabel = workNationalGradeLabel;
}
