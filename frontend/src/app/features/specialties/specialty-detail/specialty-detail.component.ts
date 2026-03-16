import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Specialty, OrgInfo } from '../../../models/org.model';
import { ToastService } from '../../../core/services/toast.service';
import { SpecialtyFormComponent } from '../specialty-form/specialty-form.component';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-specialty-detail',
  standalone: true,
  imports: [SpecialtyFormComponent],
  templateUrl: './specialty-detail.component.html',
  styleUrl: './specialty-detail.component.css',
})
export class SpecialtyDetailComponent implements OnInit {
  specialty = signal<Specialty | null>(null);
  orgInfo = signal<OrgInfo | null>(null);
  loading = signal(true);
  editModalOpen = signal(false);
  deleteModalOpen = signal(false);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toastService = inject(ToastService);

  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;

  constructor(private specialtyService: SpecialtyService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  load(id: number) {
    this.loading.set(true);
    this.specialtyService.getById(id).subscribe({
      next: (s) => {
        this.specialty.set(s);
        this.loading.set(false);
        this.specialtyService.getOrgInfo(id).subscribe((info) => this.orgInfo.set(info));
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/specialties']);
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
    const id = this.specialty()!.id;
    this.load(id);
  }

  goBack() {
    this.router.navigate(['/specialties']);
  }

  openDelete() {
    this.deleteModalOpen.set(true);
  }

  closeDelete() {
    this.deleteModalOpen.set(false);
  }

  confirmDelete() {
    this.specialtyService.delete(this.specialty()!.id).subscribe(() => {
      this.toastService.success('Спеціальність видалено');
      this.deleteModalOpen.set(false);
      this.router.navigate(['/specialties']);
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

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('uk-UA', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }
}
