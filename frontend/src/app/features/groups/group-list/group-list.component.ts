import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { GroupService } from '../../../core/services/group.service';
import { StudentGroup } from '../../../models/group.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { GroupModalComponent } from '../group-modal/group-modal.component';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [
    PaginationComponent,
    PageHeaderComponent,
    ModalComponent,
    ConfirmDialogComponent,
    GroupModalComponent,
  ],
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.css',
})
export class GroupListComponent implements OnInit {
  groups = signal<StudentGroup[]>([]);
  search = signal('');

  currentPage = signal(0);
  totalPages = signal(0);
  perPage = signal(10);
  perPageOptions = [5, 10, 25, 50];

  modalOpen = signal(false);
  isEdit = signal(false);
  editingGroup = signal<StudentGroup | null>(null);

  deleteModalOpen = signal(false);
  groupToDelete = signal<StudentGroup | null>(null);

  private groupService = inject(GroupService);
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
    this.groupService
      .getAll({
        name: this.search() || undefined,
        page: this.currentPage(),
        size: this.perPage(),
      })
      .subscribe((r) => {
        this.groups.set(r.content);
        this.totalPages.set(r.totalPages);
      });
  }

  onSearch(e: Event) {
    this.search.set((e.target as HTMLInputElement).value);
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
    this.editingGroup.set(null);
    this.isEdit.set(false);
    this.modalOpen.set(true);
  }

  openEdit(group: StudentGroup) {
    this.editingGroup.set(group);
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

  openDeleteModal(group: StudentGroup) {
    this.groupToDelete.set(group);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.groupToDelete.set(null);
  }

  confirmDelete() {
    const group = this.groupToDelete();
    if (!group) return;
    this.groupService.delete(group.id).subscribe(() => {
      this.toastService.success('Групу видалено');
      this.load();
      this.closeDeleteModal();
    });
  }

  viewGroup(id: number) {
    this.router.navigate(['/groups', id]);
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }
}
