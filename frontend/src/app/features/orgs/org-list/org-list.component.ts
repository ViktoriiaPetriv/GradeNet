import { Component, OnInit, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OrgService } from '../../../core/services/org.service';
import { Organization, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-org-list',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './org-list.component.html',
  styleUrl: './org-list.component.css',
})
export class OrgListComponent implements OnInit {
  orgs = signal<Organization[]>([]);
  filtered = signal<Organization[]>([]);
  search = signal('');
  typeFilter = signal('');
  orgTypes = Object.values(OrgType);
  modalOpen = signal(false);
  isEdit = signal(false);
  editingId = signal<number | null>(null);
  form!: FormGroup;
  totalPages = signal(0);
  currentPage = signal(0);
  private toastService = inject(ToastService);

  constructor(
    private orgService: OrgService,
    private fb: FormBuilder,
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      orgType: ['', [Validators.required]],
      parentId: [null],
    });

    this.form.get('orgType')?.valueChanges.subscribe((type) => {
      const parentControl = this.form.get('parentId');
      if (type === OrgType.DEPARTMENT) {
        parentControl?.setValidators([Validators.required]);
      } else {
        parentControl?.clearValidators();
        parentControl?.setValue(null);
      }
      parentControl?.updateValueAndValidity();
    });

    this.load();
  }

  load() {
    this.orgService
      .getAll({
        orgType: this.typeFilter() || undefined,
        page: this.currentPage(),
      })
      .subscribe((r) => {
        this.orgs.set(r.content);
        this.totalPages.set(r.totalPages);
        this.applyFilter();
      });
  }

  applyFilter() {
    const s = this.search().toLowerCase();
    const t = this.typeFilter();
    this.filtered.set(
      this.orgs().filter(
        (o) => (!s || o.name.toLowerCase().includes(s)) && (!t || o.orgType === t),
      ),
    );
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
    this.applyFilter();
  }
  
  onTypeFilter(e: Event) {
    this.typeFilter.set((e.target as HTMLSelectElement).value);
    this.currentPage.set(0);
    this.load();
  }

  getFaculties(): Organization[] {
    return this.orgs().filter((o) => o.orgType === OrgType.FACULTY);
  }

  openCreate() {
    this.isEdit.set(false);
    this.editingId.set(null);
    this.form.reset();
    this.modalOpen.set(true);
  }

  openEdit(org: Organization) {
    this.isEdit.set(true);
    this.editingId.set(org.id);
    this.form.patchValue({ name: org.name, orgType: org.orgType, parentId: org.parentId ?? null });
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
    const request = { ...val, parentId: val.parentId || null };
    if (this.isEdit() && this.editingId()) {
      this.orgService.update(this.editingId()!, request).subscribe(() => {
        this.toastService.success('Організацію оновлено');
        this.load();
        this.closeModal();
      });
    } else {
      this.orgService.create(request).subscribe(() => {
        this.toastService.success('Організацію створено');
        this.load();
        this.closeModal();
      });
    }
  }

  delete(id: number) {
    if (!confirm('Видалити організацію?')) return;
    this.orgService.delete(id).subscribe(() => {
      this.toastService.success('Організацію видалено');
      this.load();
    });
  }

  isInvalid(f: string) {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  orgTypeLabel(t: string): string {
    return t === 'FACULTY' ? 'Факультет' : 'Кафедра';
  }

  getParentName(parentId?: number): string {
    if (!parentId) return '—';
    return this.orgs().find((o) => o.id === parentId)?.name ?? '—';
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  isDepartment(): boolean {
    return this.form.get('orgType')?.value === 'DEPARTMENT';
  }

  setPage(p: number) {
    this.currentPage.set(p);
    this.load();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}
