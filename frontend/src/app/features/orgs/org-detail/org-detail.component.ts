import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { OrgService } from '../../../core/services/org.service';
import { Organization, OrgType } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';
import { OrgModalComponent } from '../org-modal/org-modal.component';

@Component({
  selector: 'app-org-detail',
  standalone: true,
  imports: [CommonModule, OrgModalComponent],
  templateUrl: './org-detail.component.html',
  styleUrl: './org-detail.component.css',
})
export class OrgDetailComponent implements OnInit {
  org = signal<Organization | null>(null);
  parentOrg = signal<Organization | null>(null);
  departments = signal<Organization[]>([]);
  loading = signal(true);
  editModalOpen = signal(false);
  deleteModalOpen = signal(false);

  isFaculty = computed(() => this.org()?.orgType === OrgType.FACULTY);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orgService = inject(OrgService);
  private toastService = inject(ToastService);

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (id) {
        this.load(id);
      }
    });
  }

  load(id: number) {
    this.loading.set(true);
    this.orgService.getById(id).subscribe({
      next: (o) => {
        this.org.set(o);
        this.loading.set(false);

        if (o.parentId) {
          this.orgService.getById(o.parentId).subscribe((p) => this.parentOrg.set(p));
        }

        if (o.orgType === OrgType.FACULTY) {
          this.orgService.getAll({ orgType: OrgType.DEPARTMENT, size: 100 }).subscribe((r) => {
            this.departments.set(r.content.filter((d) => d.parentId === o.id));
          });
        }
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/orgs']);
      },
    });
  }

  openEdit() {
    this.editModalOpen.set(true);
  }
  closeEdit() {
    this.editModalOpen.set(false);
  }

  onSaved() {
    this.closeEdit();
    this.load(this.org()!.id);
  }

  openDeleteModal() {
    this.deleteModalOpen.set(true);
  }
  closeDeleteModal() {
    this.deleteModalOpen.set(false);
  }

  confirmDelete() {
    const id = this.org()?.id;
    if (!id) return;
    this.orgService.delete(id).subscribe(() => {
      this.toastService.success('Організацію видалено');
      this.router.navigate(['/orgs']);
    });
  }

  goBack() {
    this.router.navigate(['/orgs']);
  }

  orgTypeLabel(t: string): string {
    return t === 'FACULTY' ? 'Факультет' : 'Кафедра';
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  goToOrg(id: number) {
    this.router.navigate(['/orgs', id]);
  }
}
