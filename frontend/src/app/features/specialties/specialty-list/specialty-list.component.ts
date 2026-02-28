import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { OrgService } from '../../../core/services/org.service';
import { Specialty, Degree, EduType, OrganizationShort, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';
import { SpecialtyFormComponent } from '../specialty-form/specialty-form.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

@Component({
  selector: 'app-specialty-list',
  standalone: true,
  imports: [SpecialtyFormComponent, PaginationComponent],
  templateUrl: './specialty-list.component.html',
  styleUrl: './specialty-list.component.css',
})
export class SpecialtyListComponent implements OnInit {
  specialties = signal<Specialty[]>([]);
  totalPages = signal(0);
  totalElements = signal(0);
  currentPage = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];
  degreeFilter = signal('');
  eduTypeFilter = signal('');
  degrees = Object.values(Degree);
  eduTypes = Object.values(EduType);
  modalOpen = signal(false);
  deleteModalOpen = signal(false);
  specialtyToDelete = signal<Specialty | null>(null);
  editingSpecialty = signal<Specialty | null>(null);
  faculties = signal<OrganizationShort[]>([]);
  departments = signal<OrganizationShort[]>([]);
  selectedFacultyId = signal<number | null>(null);
  selectedDeptId = signal<number | null>(null);
  loading = signal(false);

  private toastService = inject(ToastService);
  private router = inject(Router);

  constructor(
    private specialtyService: SpecialtyService,
    private orgService: OrgService,
  ) {}

  currentPageUi = computed(() => this.currentPage() + 1);

  paginationInfo = computed(() => {
    const total = this.totalPages();
    if (total === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${total} · Всього: ${this.totalElements()}`;
  });

  onPageChange(page: number) {
    this.currentPage.set(page - 1);
    this.load();
  }

  onPerPageChange(size: number) {
    this.perPage.set(size);
    this.currentPage.set(0);
    this.load();
  }

  ngOnInit() {
    this.orgService.getAllShort(OrgType.FACULTY).subscribe((f) => this.faculties.set(f));
    this.orgService.getAllShort(OrgType.DEPARTMENT).subscribe((d) => this.departments.set(d));
    this.load();
  }

  load() {
    this.loading.set(true);
    const deptId = this.selectedDeptId();
    const facultyId = this.selectedFacultyId();
    const orgId = deptId ?? facultyId ?? null;

    const params = {
      degree: this.degreeFilter() || undefined,
      eduType: this.eduTypeFilter() || undefined,
      page: this.currentPage(),
      size: this.perPage(),
    };

    const obs = orgId
      ? this.specialtyService.getByOrg(orgId, params)
      : this.specialtyService.getAll(params);

    obs.subscribe((r) => {
      this.specialties.set(r.content);
      this.totalPages.set(r.totalPages);
      this.totalElements.set(r.totalElements);
      this.loading.set(false);
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

  onFacultyFilter(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    this.selectedFacultyId.set(val ? +val : null);
    this.selectedDeptId.set(null);
    this.currentPage.set(0);
    this.load();
  }

  onDeptFilter(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    this.selectedDeptId.set(val ? +val : null);
    this.currentPage.set(0);
    this.load();
  }

  openCreate() {
    this.editingSpecialty.set(null);
    this.modalOpen.set(true);
  }

  openEdit(s: Specialty) {
    this.editingSpecialty.set(s);
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
    this.editingSpecialty.set(null);
  }

  onFormSaved() {
    this.closeModal();
    this.load();
  }

  viewDetail(id: number) {
    this.router.navigate(['/specialties', id]);
  }

  openDelete(s: Specialty) {
    this.specialtyToDelete.set(s);
    this.deleteModalOpen.set(true);
  }

  closeDelete() {
    this.deleteModalOpen.set(false);
  }

  confirmDelete() {
    const s = this.specialtyToDelete();
    if (!s) return;

    this.specialtyService.delete(s.id).subscribe(() => {
      this.toastService.success('Спеціальність видалено');
      this.deleteModalOpen.set(false);
      this.specialtyToDelete.set(null);
      this.load();
    });
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

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  getDeptName(orgId: number): string {
    return this.departments().find((d) => d.id === orgId)?.name ?? '—';
  }
}
