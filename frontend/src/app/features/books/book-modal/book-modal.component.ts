import {
  Component,
  input,
  output,
  signal,
  inject,
  OnInit,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { switchMap, of } from 'rxjs';
import { BookService } from '../../../core/services/book.service';
import { BookNumber } from '../../../models/book.model';
import { ToastService } from '../../../core/services/toast.service';
import { UserService } from '../../../core/services/user.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { OrgService } from '../../../core/services/org.service';
import { User } from '../../../models/user.model';
import { Specialty, Degree, EduType, OrganizationShort, OrgType } from '../../../models/org.model';
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
  faculties = signal<OrganizationShort[]>([]);
  specialties = signal<Specialty[]>([]);

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
    this.userService.findAll().subscribe((users) => {
      this.students.set(users.filter((u) => u.role === 'STUDENT'));
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
        const base$ = forkJoin({
          orgInfo: b.specialtyId ? this.specialtyService.getOrgInfo(b.specialtyId) : of(null),
          specialty: b.specialtyId ? this.specialtyService.getById(b.specialtyId) : of(null),
        });

        base$.subscribe(({ orgInfo, specialty }) => {
          this.form.patchValue(
            {
              number: b.number,
              studentId: b.studentId,
              facultyId: orgInfo?.facultyId ?? null,
              degree: specialty?.degree ?? null,
              eduType: specialty?.eduType ?? null,
              specialtyId: b.specialtyId ?? null,
            },
            { emitEvent: false },
          );

          if (orgInfo && specialty) {
            this.specialtyService
              .getByOrg(orgInfo.facultyId, {
                degree: specialty.degree,
                eduType: specialty.eduType,
                page: 0,
              })
              .subscribe((page) => {
                this.specialties.set(page.content);
              });
          }
        });
      } else {
        this.form.reset();
        this.specialties.set([]);
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
    });
  }

  onFacultyChange() {
    this.form.patchValue({ degree: null, eduType: null, specialtyId: null });
    this.specialties.set([]);
  }

  onSpecialtyFilterChange() {
    const facultyId: number | null = this.form.get('facultyId')!.value;
    const degree: string | null = this.form.get('degree')!.value;
    const eduType: string | null = this.form.get('eduType')!.value;

    this.form.patchValue({ specialtyId: null });

    if (facultyId && degree && eduType) {
      this.specialtyService.getByOrg(facultyId, { degree, eduType, page: 0 }).subscribe((page) => {
        this.specialties.set(page.content);
      });
    } else {
      this.specialties.set([]);
    }
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { facultyId, degree, eduType, ...request } = this.form.value;

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
