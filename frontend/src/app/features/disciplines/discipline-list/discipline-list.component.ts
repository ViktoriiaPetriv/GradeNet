import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { DisciplineService } from '../../../core/services/discipline.service';
import { DisciplineDTO } from '../../../models/discipline.model';
import { ToastService } from '../../../core/services/toast.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { DisciplineModalComponent } from '../discipline-modal/discipline-modal.component';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-discipline-list',
  standalone: true,
  imports: [
    PaginationComponent,
    DisciplineModalComponent,
    PageHeaderComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './discipline-list.component.html',
  styleUrl: './discipline-list.component.css',
})
export class DisciplineListComponent implements OnInit {
  disciplines = signal<DisciplineDTO[]>([]);
  filtered = signal<DisciplineDTO[]>([]);
  search = signal('');

  currentPage = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  editingDiscipline = signal<DisciplineDTO | null>(null);
  deleteTarget = signal<DisciplineDTO | null>(null);

  private disciplineService = inject(DisciplineService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;

  currentPageUi = computed(() => this.currentPage() + 1);

  totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.perPage())));

  paginatedItems = computed(() => {
    const start = this.currentPage() * this.perPage();
    return this.filtered().slice(start, start + this.perPage());
  });

  paginationInfo = computed(() => {
    if (this.filtered().length === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${this.totalPages()}`;
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.disciplineService.getAll().subscribe((items) => {
      this.disciplines.set(items);
      this.applyFilter();
    });
  }

  applyFilter() {
    const s = this.search().toLowerCase();
    this.filtered.set(this.disciplines().filter((d) => !s || d.name.toLowerCase().includes(s)));
    this.currentPage.set(0);
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
    this.applyFilter();
  }

  onPageChange(page: number) {
    this.currentPage.set(page - 1);
  }

  onPerPageChange(size: number) {
    this.perPage.set(size);
    this.currentPage.set(0);
  }

  openCreate() {
    this.editingDiscipline.set(null);
    this.modalOpen.set(true);
  }

  openEdit(d: DisciplineDTO) {
    this.editingDiscipline.set(d);
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  onSaved() {
    this.load();
    this.closeModal();
  }

  openDeleteModal(d: DisciplineDTO) {
    this.deleteTarget.set(d);
  }

  closeDeleteModal() {
    this.deleteTarget.set(null);
  }

  confirmDelete() {
    const d = this.deleteTarget();
    if (!d) return;
    this.disciplineService.delete(d.id).subscribe({
      next: () => {
        this.toastService.success('Дисципліну видалено');
        this.load();
        this.closeDeleteModal();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Помилка при видаленні дисципліни';
        this.toastService.error(msg);
        this.closeDeleteModal();
      },
    });
  }

  viewDiscipline(id: number) {
    this.router.navigate(['/disciplines', id]);
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }
}
