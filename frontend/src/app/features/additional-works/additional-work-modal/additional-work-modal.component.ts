import { Component, OnInit, OnDestroy, input, output, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AdditionalWorkService } from '../../../core/services/additional-work.service';
import { UserService } from '../../../core/services/user.service';
import { BookService } from '../../../core/services/book.service';
import { AdditionalWork, WorkType } from '../../../models/additional-work.model';
import { Commission } from '../../../models/commission.model';
import { User, StudentInfo } from '../../../models/user.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-additional-work-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalComponent],
  templateUrl: './additional-work-modal.component.html',
  styleUrl: './additional-work-modal.component.css',
})
export class AdditionalWorkModalComponent implements OnInit, OnDestroy {
  work        = input<AdditionalWork | null>(null);
  mode        = input<'create' | 'edit'>('create');
  commissions = input<Commission[]>([]);

  saved     = output<void>();
  cancelled = output<void>();

  // Forms
  baseForm!:          FormGroup;
  courseForm!:        FormGroup;
  practiceForm!:      FormGroup;
  qualificationForm!: FormGroup;

  // Student search
  studentSearch  = new FormControl('');
  searchResults  = signal<User[]>([]);
  selectedStudent = signal<User | null>(null);
  studentBooks   = signal<StudentInfo[]>([]);
  searching      = signal(false);
  changingStudent = signal(false);

  // Commission picker
  commissionPickerOpen    = signal(false);
  selectedCommissionId    = signal<number | null>(null);
  showInactiveCommissions = signal(false);

  // Professors for detail forms
  professors = signal<User[]>([]);

  // Supervisor search
  supervisorSearch   = new FormControl('');
  supervisorResults  = signal<User[]>([]);
  selectedSupervisor = signal<User | null>(null);

  private destroy$ = new Subject<void>();
  private workService = inject(AdditionalWorkService);
  private userService = inject(UserService);
  private bookService = inject(BookService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);

  readonly required = Validators.required;

  readonly workTypes: { value: WorkType; label: string }[] = [
    { value: 'COURSE_WORK',          label: 'Курсова робота' },
    { value: 'EDUCATIONAL_PRACTICE', label: 'Навчальна практика' },
    { value: 'PRODUCTION_PRACTICE',  label: 'Виробнича практика' },
    { value: 'QUALIFICATION',        label: 'Кваліфікаційна робота' },
    { value: 'COMPREHENSIVE_EXAM',   label: 'Комплексний екзамен' },
  ];
  readonly stateOptions = [
    { value: 'IN_PROGRESS', label: 'В процесі' },
    { value: 'COMPLETED',   label: 'Завершено' },
    { value: 'FAILED',      label: 'Не зараховано' },
  ];

  get isEdit()      { return this.mode() === 'edit'; }
  get selectedType(): WorkType { return this.baseForm?.get('type')?.value; }

  selectedCommission = computed(() =>
    this.commissions().find(c => c.id === this.selectedCommissionId()) ?? null
  );

  activeCommissions = computed(() =>
    this.commissions().filter(c => this.isCommissionActive(c))
  );

  inactiveCommissions = computed(() =>
    this.commissions().filter(c => !this.isCommissionActive(c))
  );

  ngOnInit() {
    const w = this.work();

    this.selectedCommissionId.set(w?.commissionId ?? null);

    this.baseForm = this.fb.group({
      bookNumberId: [w?.bookNumberId ?? null, Validators.required],
      commissionId: [w?.commissionId ?? null, Validators.required],
      type:         [w?.type ?? 'COURSE_WORK', Validators.required],
      title:        [w?.title ?? '', Validators.required],
      eventDate:    [w?.eventDate ?? ''],
    });

    const cwd = w?.courseWorkDetails;
    this.courseForm = this.fb.group({
      semester:    [cwd?.semester ?? null, Validators.required],
      state:       [cwd?.state ?? 'IN_PROGRESS', Validators.required],
      ectsCredits: [cwd?.ectsCredits ?? null],
      totalHours:  [cwd?.totalHours ?? null],
    });

    const pd = w?.practiceDetails;
    this.practiceForm = this.fb.group({
      organization:    [pd?.organization ?? '', Validators.required],
      course:          [pd?.course ?? null, Validators.required],
      startDate:       [pd?.startDate ?? '', Validators.required],
      endDate:         [pd?.endDate ?? ''],
      workDescription: [pd?.workDescription ?? ''],
      ectsCredits:     [pd?.ectsCredits ?? null, Validators.required],
      totalHours:      [pd?.totalHours ?? null],
      supervisorId:    [pd?.supervisorId ?? null, Validators.required],
    });

    const qd = w?.qualificationDetails;
    this.qualificationForm = this.fb.group({
      supervisorId: [qd?.supervisorId ?? null, Validators.required],
      state:        [qd?.state ?? 'IN_PROGRESS', Validators.required],
    });

    this.userService.getProfessors().subscribe(data => {
      this.professors.set(data);
      const supervisorId = w?.practiceDetails?.supervisorId ?? w?.qualificationDetails?.supervisorId ?? null;
      if (supervisorId) {
        const found = data.find(p => p.id === supervisorId) ?? null;
        this.selectedSupervisor.set(found);
        if (found) this.supervisorSearch.setValue(this.fullName(found), { emitEvent: false });
      }
    });


    this.baseForm.get('type')!.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((type: WorkType) => {
      const practiceLabels: Partial<Record<WorkType, string>> = {
        EDUCATIONAL_PRACTICE: 'Навчальна практика',
        PRODUCTION_PRACTICE:  'Виробнича практика',
      };
      if (practiceLabels[type]) {
        this.baseForm.get('title')!.setValue(practiceLabels[type], { emitEvent: false });
      }
    });

    this.supervisorSearch.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(query => {
      if (!query || query.trim().length < 2) { this.supervisorResults.set([]); return; }
      const q = query.toLowerCase();
      this.supervisorResults.set(
        this.professors().filter(p => this.fullName(p).toLowerCase().includes(q))
      );
    });

    this.studentSearch.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(query => {
      if (query && query.trim().length >= 2) {
        this.searching.set(true);
        this.userService.searchStudents(query).subscribe({
          next: results => { this.searchResults.set(results); this.searching.set(false); },
          error: ()      => { this.searchResults.set([]); this.searching.set(false); },
        });
      } else {
        this.searchResults.set([]);
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Student selection
  selectStudent(student: User) {
    this.selectedStudent.set(student);
    this.searchResults.set([]);
    this.studentSearch.reset('');
    this.changingStudent.set(false);
    this.baseForm.patchValue({ bookNumberId: null });
    this.bookService.findByStudentId(student.id).subscribe(books => {
      const infos: StudentInfo[] = books.map(b => ({
        bookId: b.id,
        bookNumber: b.number,
        bookNumberStatus: b.status,
        startDate: b.regStartDate,
        endDate: b.regEndDate,
        specialtyOfferingId: b.specialtyOfferingId ?? 0,
        orgId: 0,
      }));
      this.studentBooks.set(infos);
      if (books.length === 1) {
        this.baseForm.patchValue({ bookNumberId: books[0].id });
      }
    });
  }

  clearStudent() {
    this.selectedStudent.set(null);
    this.studentBooks.set([]);
    this.baseForm.patchValue({ bookNumberId: null });
    this.changingStudent.set(false);
    this.studentSearch.reset('');
  }

  startChangingStudent() {
    this.changingStudent.set(true);
    this.selectedStudent.set(null);
    this.studentBooks.set([]);
    this.baseForm.patchValue({ bookNumberId: null });
    this.studentSearch.reset('');
  }

  onBookSelect(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    this.baseForm.patchValue({ bookNumberId: val ? Number(val) : null });
  }

  // Commission picker
  openCommissionPicker()  { this.commissionPickerOpen.set(true); }
  closeCommissionPicker() { this.commissionPickerOpen.set(false); }

  selectCommission(c: Commission) {
    this.selectedCommissionId.set(c.id);
    this.baseForm.patchValue({ commissionId: c.id });
    this.commissionPickerOpen.set(false);
  }

  isCommissionActive(c: Commission): boolean {
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  commissionDateLabel(c: Commission): string {
    const end = c.endDate ? this.formatDate(c.endDate) : '...';
    return `${this.formatDate(c.startDate)} — ${end}`;
  }

  // Supervisor
  selectSupervisor(p: User) {
    this.selectedSupervisor.set(p);
    this.supervisorSearch.setValue(this.fullName(p), { emitEvent: false });
    this.supervisorResults.set([]);
    this.practiceForm.patchValue({ supervisorId: p.id });
    this.qualificationForm.patchValue({ supervisorId: p.id });
  }

  clearSupervisor() {
    this.selectedSupervisor.set(null);
    this.supervisorSearch.reset('');
    this.supervisorResults.set([]);
    this.practiceForm.patchValue({ supervisorId: null });
    this.qualificationForm.patchValue({ supervisorId: null });
  }

  fullName(p: User): string {
    return `${p.lastName} ${p.firstName}${p.patronymic ? ' ' + p.patronymic : ''}`;
  }

  professorName(id: number): string {
    const p = this.professors().find(p => p.id === id);
    return p ? `${p.lastName} ${p.firstName}` : `#${id}`;
  }

  // Submit
  submit() {
    this.baseForm.markAllAsTouched();
    const detailsForm = this.detailsFormForType(this.selectedType);
    if (detailsForm) detailsForm.markAllAsTouched();

    if (this.baseForm.invalid || detailsForm?.invalid) return;

    const request = { ...this.baseForm.value };

    if (this.isEdit) {
      const w = this.work()!;
      const editRequest = {
        ...request,
        universityGrade: w.universityGrade,
        ectsGrade: w.ectsGrade,
        nationalGrade: w.nationalGrade,
      };
      this.workService.update(w.id, editRequest).subscribe(updated => this.saveDetails(updated.id, w));
    } else {
      this.workService.create(request).subscribe(created => this.saveDetails(created.id, null));
    }
  }

  private saveDetails(workId: number, existing: AdditionalWork | null) {
    const type = this.selectedType;

    if (type === 'COURSE_WORK') {
      const obs = existing?.courseWorkDetails
        ? this.workService.updateCourseWorkDetails(workId, this.courseForm.value)
        : this.workService.createCourseWorkDetails(workId, this.courseForm.value);
      obs.subscribe(() => this.onSuccess());
    } else if (type === 'EDUCATIONAL_PRACTICE' || type === 'PRODUCTION_PRACTICE') {
      const obs = existing?.practiceDetails
        ? this.workService.updatePracticeDetails(workId, this.practiceForm.value)
        : this.workService.createPracticeDetails(workId, this.practiceForm.value);
      obs.subscribe(() => this.onSuccess());
    } else if (type === 'QUALIFICATION') {
      const obs = existing?.qualificationDetails
        ? this.workService.updateQualificationDetails(workId, this.qualificationForm.value)
        : this.workService.createQualificationDetails(workId, this.qualificationForm.value);
      obs.subscribe(() => this.onSuccess());
    } else {
      this.onSuccess();
    }
  }

  private onSuccess() {
    this.toastService.success(this.isEdit ? 'Роботу оновлено' : 'Роботу створено');
    this.saved.emit();
  }

  private detailsFormForType(type: WorkType): FormGroup | null {
    if (type === 'COURSE_WORK')   return this.courseForm;
    if (type === 'EDUCATIONAL_PRACTICE' || type === 'PRODUCTION_PRACTICE') return this.practiceForm;
    if (type === 'QUALIFICATION') return this.qualificationForm;
    return null;
  }

  isInvalid(form: FormGroup, f: string): boolean {
    const c = form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  formatDate(date: string): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }
}
