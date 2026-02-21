import { Component, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { OrgService } from '../../../core/services/org.service';
import { Specialty, Degree, EduType, Organization } from '../../../models/org.model';

@Component({
  selector: 'app-specialty-list',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './specialty-list.component.html',
  styleUrl: './specialty-list.component.css',
})
export class SpecialtyListComponent implements OnInit {
  specialties = signal<Specialty[]>([]);
  orgs = signal<Organization[]>([]);
  totalPages = signal(0);
  totalElements = signal(0);
  currentPage = signal(0);
  degreeFilter = signal('');
  eduTypeFilter = signal('');
  degrees = Object.values(Degree);
  eduTypes = Object.values(EduType);
  modalOpen = signal(false);
  isEdit = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;

  constructor(
    private specialtyService: SpecialtyService,
    private orgService: OrgService,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(10)]],
      nameUA: ['', Validators.required],
      nameEN: ['', Validators.required],
      studyProgramUA: ['', Validators.required],
      studyProgramEN: ['', Validators.required],
      eduProgramUA: ['', Validators.required],
      eduProgramEN: ['', Validators.required],
      orgId: ['', Validators.required],
      degree: ['', Validators.required],
      eduType: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: [''],
    });
    this.orgService.getAll().subscribe((o) => this.orgs.set(o));
    this.load();
  }

  load() {
    this.specialtyService
      .getAll({
        degree: this.degreeFilter() || undefined,
        eduType: this.eduTypeFilter() || undefined,
        page: this.currentPage(),
      })
      .subscribe((r) => {
        this.specialties.set(r.content);
        this.totalPages.set(r.totalPages);
        this.totalElements.set(r.totalElements);
      });
  }

  onDegreeFilter(e: Event) {
    this.degreeFilter.set((e.target as HTMLSelectElement).value);
    this.currentPage.set(0);
    this.load();
  }
  onEduTypeFilter(e: Event) {
    this.eduTypeFilter.set((e.target as HTMLSelectElement).value);
    this.currentPage.set(0);
    this.load();
  }
  setPage(p: number) {
    this.currentPage.set(p);
    this.load();
  }
  get pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i + 1);
  }

  openCreate() {
    this.isEdit.set(false);
    this.editingId.set(null);
    this.form.reset();
    this.modalOpen.set(true);
  }

  openEdit(s: Specialty) {
    this.isEdit.set(true);
    this.editingId.set(s.id);
    this.form.patchValue({
      ...s,
      startDate: this.toDateInput(s.startDate),
      endDate: s.endDate ? this.toDateInput(s.endDate) : '',
    });
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
    this.form.reset();
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const val = this.form.value;
    const request = {
      ...val,
      startDate: new Date(val.startDate).toISOString(),
      endDate: val.endDate ? new Date(val.endDate).toISOString() : null,
    };
    if (this.isEdit() && this.editingId()) {
      this.specialtyService.update(this.editingId()!, request).subscribe(() => {
        this.load();
        this.closeModal();
      });
    } else {
      this.specialtyService.create(request).subscribe(() => {
        this.load();
        this.closeModal();
      });
    }
  }

  delete(id: number) {
    if (!confirm('Видалити спеціальність?')) return;
    this.specialtyService.delete(id).subscribe(() => this.load());
  }

  isInvalid(f: string) {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  getOrgName(orgId: number): string {
    return this.orgs().find((o) => o.id === orgId)?.name ?? '—';
  }

  degreeLabel(d: string): string {
    const map: Record<string, string> = {
      BACHELOR: 'Бакалавр',
      MASTER: 'Магістр',
      DOCTOR: 'Доктор',
      SPECIALIST: 'Спеціаліст',
    };
    return map[d] || d;
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
    return map[e] || e;
  }

  toDateInput(iso: string): string {
    return iso ? iso.split('T')[0] : '';
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }
}
