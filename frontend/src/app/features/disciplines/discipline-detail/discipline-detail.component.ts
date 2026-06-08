import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DisciplineService } from '../../../core/services/discipline.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { DisciplineDTO, SpecialtyDisciplineDTO, HoursDTO } from '../../../models/discipline.model';
import { ToastService } from '../../../core/services/toast.service';
import { Specialty, SpecialtyOffering } from '../../../models/org.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { HeroCardComponent } from '../../../shared/hero-card/hero-card.component';
import { InfoCardComponent } from '../../../shared/info-card/info-card.component';
import { DisciplineModalComponent } from '../discipline-modal/discipline-modal.component';
import { SpecialtyDisciplineModalComponent } from '../specialty-discipline-modal/specialty-discipline-modal.component';
import { AddHoursModalComponent } from '../add-hours-modal/add-hours-modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-discipline-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, HeroCardComponent, InfoCardComponent, DisciplineModalComponent, SpecialtyDisciplineModalComponent, AddHoursModalComponent, ConfirmDialogComponent],
  templateUrl: './discipline-detail.component.html',
  styleUrl: './discipline-detail.component.css',
})
export class DisciplineDetailComponent implements OnInit {
  discipline = signal<DisciplineDTO | null>(null);
  specialtyDisciplines = signal<SpecialtyDisciplineDTO[]>([]);
  offeringMap = signal<Map<number, SpecialtyOffering>>(new Map());
  specialtyMap = signal<Map<number, Specialty>>(new Map());
  loading = signal(true);
  editModalOpen = signal(false);
  addSdModalOpen = signal(false);
  addHoursTargetSdId = signal<number | null>(null);
  editHoursTarget = signal<HoursDTO | null>(null);
  deleteHoursTarget = signal<HoursDTO | null>(null);
  deletingHours = signal(false);
  expandedSdIds = signal<Set<number>>(new Set());

  existingSpecialtyOfferingIds = () => this.specialtyDisciplines().map((sd) => sd.specialtyOfferingId);

  toggleHoursExpanded(sdId: number) {
    const current = new Set(this.expandedSdIds());
    if (current.has(sdId)) {
      current.delete(sdId);
    } else {
      current.add(sdId);
    }
    this.expandedSdIds.set(current);
  }

  isHoursExpanded(sdId: number): boolean {
    return this.expandedSdIds().has(sdId);
  }

  getSpecialtyNameForSd(sdId: number): string {
    const sd = this.specialtyDisciplines().find((s) => s.id === sdId);
    if (!sd) return '';
    const offering = this.offeringMap().get(sd.specialtyOfferingId);
    if (!offering) return `Набір #${sd.specialtyOfferingId}`;
    return this.getSpecialtyName(offering.specialtyId) + ` (${offering.graduationYear})`;
  }

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private disciplineService = inject(DisciplineService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);
  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;
  isAdminOrManager = this.authState.isAdminOrManager;

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  load(id: number) {
    this.loading.set(true);
    forkJoin({
      discipline: this.disciplineService.getById(id),
      sdList: this.disciplineService.getSpecialtyDisciplines(id),
      specialties: this.specialtyService.getAll({ size: 200 }),
    }).subscribe({
      next: ({ discipline, sdList, specialties }) => {
        this.discipline.set(discipline);
        this.specialtyDisciplines.set(sdList);

        const specMap = new Map<number, Specialty>();
        specialties.content.forEach((s) => specMap.set(s.id, s));
        this.specialtyMap.set(specMap);

        const offeringIds = [...new Set(sdList.map((sd) => sd.specialtyOfferingId))];
        if (offeringIds.length > 0) {
          forkJoin(offeringIds.map((oid) => this.specialtyService.getOfferingById(oid))).subscribe((offs) => {
            const offMap = new Map<number, SpecialtyOffering>();
            offs.forEach((o) => offMap.set(o.id, o));
            this.offeringMap.set(offMap);
          });
        }

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/disciplines']);
      },
    });
  }

  onSaved() {
    this.editModalOpen.set(false);
    this.load(this.discipline()!.id);
  }

  onSdAdded() {
    this.addSdModalOpen.set(false);
    this.load(this.discipline()!.id);
  }

  onHoursAdded() {
    this.addHoursTargetSdId.set(null);
    this.load(this.discipline()!.id);
  }

  onHoursEdited() {
    this.editHoursTarget.set(null);
    this.load(this.discipline()!.id);
  }

  confirmDeleteHours() {
    const hours = this.deleteHoursTarget();
    if (!hours) return;
    this.deletingHours.set(true);
    this.disciplineService.deleteHours(hours.id).subscribe({
      next: () => {
        this.toastService.success('Години видалено');
        this.deletingHours.set(false);
        this.deleteHoursTarget.set(null);
        this.load(this.discipline()!.id);
      },
      error: () => {
        this.deletingHours.set(false);
      },
    });
  }

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
  }

  getSpecialtyIdForSd(sdId: number): number | null {
    const sd = this.specialtyDisciplines().find((s) => s.id === sdId);
    if (!sd) return null;
    return this.offeringMap().get(sd.specialtyOfferingId)?.specialtyId ?? null;
  }

  getSpecialtyName(id: number): string {
    const s = this.specialtyMap().get(id);
    return s ? `${s.code} — ${s.nameUA}` : `#${id}`;
  }

  sortedHours(hours: HoursDTO[]): HoursDTO[] {
    return [...hours].sort((a, b) => a.academicYear.localeCompare(b.academicYear));
  }

  totalEcts(): number {
    return this.specialtyDisciplines().reduce((sum, sd) => {
      const maxEcts = Math.max(0, ...sd.hours.map((h) => h.ectsCredits ?? 0));
      return sum + maxEcts;
    }, 0);
  }
}
