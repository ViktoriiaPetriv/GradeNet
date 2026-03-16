import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { OrgService } from '../../../core/services/org.service';
import { Organization, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { OrgModalComponent } from '../org-modal/org-modal.component';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-org-list',
  standalone: true,
  imports: [PaginationComponent, OrgModalComponent],
  templateUrl: './org-list.component.html',
  styleUrl: './org-list.component.css',
})
export class OrgListComponent implements OnInit {
  orgs = signal<Organization[]>([]);
  filtered = signal<Organization[]>([]);
  search = signal('');
  typeFilter = signal<OrgType | ''>('');
  orgTypes = Object.values(OrgType);

  currentPage = signal(0);
  totalPages = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  isEdit = signal(false);
  editingOrg = signal<Organization | null>(null);

  deleteModalOpen = signal(false);
  orgToDelete = signal<Organization | null>(null);

  private orgService = inject(OrgService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;

  currentPageUi = computed(() => this.currentPage() + 1);

  paginationInfo = computed(() => {
    const total = this.totalPages();
    if (total === 0) return 'Немає записів';
    return `Сторінка ${this.currentPageUi()} з ${total}`;
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.orgService
      .getAll({
        orgType: this.typeFilter() || undefined,
        page: this.currentPage(),
        size: this.perPage(),
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
    this.typeFilter.set((e.target as HTMLSelectElement).value as OrgType | '');
    this.currentPage.set(0);
    this.load();
  }

  onPageChange(page: number) {
    this.currentPage.set(page - 1);
    this.load();
  }

  onPerPageChange(size: number) {
    this.perPage.set(size);
    this.currentPage.set(0);
    this.load();
  }

  openCreate() {
    this.editingOrg.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(org: Organization) {
    this.editingOrg.set(org);
    this.isEdit.set(true);
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  onSaved() {
    this.load();
    this.closeModal();
  }

  openDeleteModal(org: Organization) {
    this.orgToDelete.set(org);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.orgToDelete.set(null);
  }

  confirmDelete() {
    const org = this.orgToDelete();
    if (!org) return;
    this.orgService.delete(org.id).subscribe(() => {
      this.toastService.success('Організацію видалено');
      this.load();
      this.closeDeleteModal();
    });
  }

  viewOrg(id: number) {
    this.router.navigate(['/orgs', id]);
  }

  orgTypeLabel(t: string): string {
    return t === 'FACULTY' ? 'Факультет' : 'Кафедра';
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  getParentName(parentId: number | null | undefined): string {
    if (!parentId) return '—';
    return this.orgs().find((o) => o.id === parentId)?.name ?? '—';
  }
}
