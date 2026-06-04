import {
  Component,
  input,
  output,
  signal,
  computed,
  inject,
  OnInit,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { switchMap, of, Observable } from 'rxjs';
import { BookService } from '../../../core/services/book.service';
import { BookNumber } from '../../../models/book.model';
import { ToastService } from '../../../core/services/toast.service';
import { UserService } from '../../../core/services/user.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { OrgService } from '../../../core/services/org.service';
import { User } from '../../../models/user.model';
import { Specialty, SpecialtyOffering, Degree, EduType, OrganizationShort, OrgType } from '../../../models/org.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-book-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './book-modal.component.html',
  styleUrl: './book-modal.component.css',
})
export class BookModalComponent implements OnInit, OnChanges {
  book = input<BookNumber | null>(null);
  mode = input<'create' | 'edit'>('create');

  saved = output<void>();
  cancelled = output<void>();

  form!: FormGroup;

  students = signal<User[]>([]);
  studentSearch = signal('');
  showStudentDropdown = signal(false);

  filteredStudents = computed(() => {
    const search = this.studentSearch().toLowerCase().trim();
    if (!search) return this.students();
    return this.students().filter((s) =>
      this.getStudentLabel(s).toLowerCase().includes(search)
    );
  });

  faculties = signal<OrganizationShort[]>([]);
  facultySearch = signal('');
  showFacultyDropdown = signal(false);

  filteredFaculties = computed(() => {
    const q = this.facultySearch().toLowerCase().trim();
    if (!q) return this.faculties();
    return this.faculties().filter((f) => f.name.toLowerCase().includes(q));
  });

  specialties = signal<Specialty[]>([]);
  specialtySearch = signal('');
  showSpecialtyDropdown = signal(false);

  filteredSpecialties = computed(() => {
    const q = this.specialtySearch().toLowerCase().trim();
    if (!q) return this.specialties();
    return this.specialties().filter(
      (s) => s.nameUA.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)
    );
  });

  offerings = signal<SpecialtyOffering[]>([]);

  readonly degrees = Object.values(Degree);
  readonly eduTypes = Object.values(EduType);

  private fb = inject(FormBuilder);
  private bookService = inject(BookService);
  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private orgService = inject(OrgService);
  private toastService = inject(ToastService);

  get isEdit(): boolean {
    return this.mode() === 'edit';
  }

  constructor() {
    this.initForm();
  }

  ngOnInit() {
    this.userService.findAll('STUDENT').subscribe((students) => {
      this.students.set(students);
      const currentId = this.form.get('studentId')?.value;
      if (currentId) {
        const s = students.find((u) => u.id === currentId);
        if (s) this.studentSearch.set(this.getStudentLabel(s));
      }
    });

    this.orgService.getAllShort(OrgType.FACULTY).subscribe((f) => {
      this.faculties.set(f);
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!this.form) return;

    if (changes['mode'] || changes['book']) {
      const b = this.book();

      if (this.isEdit && b) {
        const offering$: Observable<SpecialtyOffering | null> = b.specialtyOfferingId
          ? this.specialtyService.getOfferingById(b.specialtyOfferingId)
          : of(null);

        offering$.pipe(
          switchMap((offering) => {
            if (!offering) return of({ orgInfo: null, specialty: null, offering: null });
            return forkJoin({
              orgInfo: this.specialtyService.getOrgInfo(offering.specialtyId),
              specialty: this.specialtyService.getById(offering.specialtyId),
            }).pipe(switchMap(({ orgInfo, specialty }) => of({ orgInfo, specialty, offering })));
          }),
        ).subscribe(({ orgInfo, specialty, offering }) => {
          this.form.patchValue(
            {
              number: b.number,
              studentId: b.studentId,
              facultyId: orgInfo?.facultyId ?? null,
              degree: specialty?.degree ?? null,
              eduType: specialty?.eduType ?? null,
              specialtyId: offering?.specialtyId ?? null,
              specialtyOfferingId: b.specialtyOfferingId ?? null,
            },
            { emitEvent: false },
          );

          const student = this.students().find((s) => s.id === b.studentId);
          if (student) this.studentSearch.set(this.getStudentLabel(student));

          const faculty = this.faculties().find((f) => f.id === orgInfo?.facultyId);
          if (faculty) this.facultySearch.set(faculty.name);

          if (specialty) {
            this.specialtySearch.set(`${specialty.code} — ${specialty.nameUA}`);
          }

          if (orgInfo && specialty) {
            this.specialtyService
              .getByOrg(orgInfo.facultyId, { degree: specialty.degree, eduType: specialty.eduType, page: 0 })
              .subscribe((page) => {
                this.specialties.set(page.content);
                if (offering) {
                  this.specialtyService.getOfferings(offering.specialtyId).subscribe((offs) => {
                    this.offerings.set(offs);
                  });
                }
              });
          }
        });
      } else {
        this.form.reset();
        this.studentSearch.set('');
        this.facultySearch.set('');
        this.specialtySearch.set('');
        this.specialties.set([]);
        this.offerings.set([]);
      }
    }
  }

  initForm() {
    this.form = this.fb.group({
      number: ['', [Validators.required, Validators.maxLength(20)]],
      studentId: [null, Validators.required],
      facultyId: [null, Validators.required],
      degree: [null, Validators.required],
      eduType: [null, Validators.required],
      specialtyId: [null, Validators.required],
      specialtyOfferingId: [null, Validators.required],
    });
  }

  onStudentSearchInput(value: string) {
    this.studentSearch.set(value);
    this.form.patchValue({ studentId: null });
    this.showStudentDropdown.set(true);
  }

  onStudentInputFocus() {
    this.showStudentDropdown.set(true);
  }

  selectStudent(student: User) {
    this.form.patchValue({ studentId: student.id });
    this.form.get('studentId')!.markAsTouched();
    this.studentSearch.set(this.getStudentLabel(student));
    this.showStudentDropdown.set(false);
  }

  closeStudentDropdown() {
    setTimeout(() => this.showStudentDropdown.set(false), 150);
  }

  onFacultySearchInput(value: string) {
    this.facultySearch.set(value);
    this.form.patchValue({ facultyId: null, degree: null, eduType: null, specialtyId: null, specialtyOfferingId: null });
    this.specialties.set([]);
    this.offerings.set([]);
    this.specialtySearch.set('');
    this.showFacultyDropdown.set(true);
  }

  onFacultyInputFocus() {
    this.showFacultyDropdown.set(true);
  }

  closeFacultyDropdown() {
    setTimeout(() => this.showFacultyDropdown.set(false), 150);
  }

  selectFaculty(f: OrganizationShort) {
    this.form.patchValue({ facultyId: f.id });
    this.form.get('facultyId')!.markAsTouched();
    this.facultySearch.set(f.name);
    this.showFacultyDropdown.set(false);
  }

  onFacultyChange() {
    this.form.patchValue({ degree: null, eduType: null, specialtyId: null, specialtyOfferingId: null });
    this.specialties.set([]);
    this.offerings.set([]);
    this.specialtySearch.set('');
  }

  onSpecialtySearchInput(value: string) {
    this.specialtySearch.set(value);
    this.form.patchValue({ specialtyId: null, specialtyOfferingId: null });
    this.offerings.set([]);
    this.showSpecialtyDropdown.set(true);
  }

  onSpecialtyInputFocus() {
    this.showSpecialtyDropdown.set(true);
  }

  closeSpecialtyDropdown() {
    setTimeout(() => this.showSpecialtyDropdown.set(false), 150);
  }

  selectSpecialty(s: Specialty) {
    this.form.patchValue({ specialtyId: s.id });
    this.form.get('specialtyId')!.markAsTouched();
    this.specialtySearch.set(`${s.code} — ${s.nameUA}`);
    this.showSpecialtyDropdown.set(false);
    this.onSpecialtyChange();
  }

  onSpecialtyFilterChange() {
    const facultyId: number | null = this.form.get('facultyId')!.value;
    const degree: string | null = this.form.get('degree')!.value;
    const eduType: string | null = this.form.get('eduType')!.value;

    this.form.patchValue({ specialtyId: null, specialtyOfferingId: null });
    this.specialtySearch.set('');
    this.offerings.set([]);

    if (facultyId && degree && eduType) {
      this.specialtyService.getByOrg(facultyId, { degree, eduType, page: 0 }).subscribe((page) => {
        this.specialties.set(page.content);
      });
    } else {
      this.specialties.set([]);
    }
  }

  onSpecialtyChange() {
    const specialtyId: number | null = this.form.get('specialtyId')!.value;
    this.form.patchValue({ specialtyOfferingId: null });
    if (specialtyId) {
      this.specialtyService.getOfferings(specialtyId).subscribe((offs) => this.offerings.set(offs));
    } else {
      this.offerings.set([]);
    }
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { facultyId, degree, eduType, specialtyId, ...request } = this.form.value;

    const action$ = this.isEdit
      ? this.bookService.update(this.book()!.id, request)
      : this.bookService.create(request);

    action$.subscribe(() => {
      this.toastService.success(this.isEdit ? 'Заліковку оновлено' : 'Заліковку зареєстровано');
      this.saved.emit();
    });
  }

  cancel() {
    this.cancelled.emit();
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }

  getStudentLabel(u: User): string {
    const name = [u.lastName, u.firstName].filter(Boolean).join(' ');
    return name ? `${name} (${u.email})` : u.email;
  }

  degreeLabel(d: string): string {
    const map: Record<string, string> = {
      BACHELOR: 'Бакалавр',
      MASTER: 'Магістр',
      DOCTOR: 'Доктор',
      SPECIALIST: 'Спеціаліст',
    };
    return map[d] ?? d;
  }

  eduTypeLabel(e: string): string {
    const map: Record<string, string> = {
      FULL_TIME: 'Денна',
      PART_TIME: 'Вечірня',
      CORRESPONDENCE: 'Заочна',
      DISTANCE: 'Дистанційна',
      BLENDED: 'Змішана',
      EXTERN: 'Екстернат',
    };
    return map[e] ?? e;
  }
}
