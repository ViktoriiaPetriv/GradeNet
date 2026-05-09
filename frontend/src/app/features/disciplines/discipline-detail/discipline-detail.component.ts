import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DisciplineService } from '../../../core/services/discipline.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { DisciplineDTO, SpecialtyDisciplineDTO, HoursDTO } from '../../../models/discipline.model';
import { Specialty } from '../../../models/org.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { HeroCardComponent } from '../../../shared/hero-card/hero-card.component';
import { InfoCardComponent } from '../../../shared/info-card/info-card.component';
import { DisciplineModalComponent } from '../discipline-modal/discipline-modal.component';
import { SpecialtyDisciplineModalComponent } from '../specialty-discipline-modal/specialty-discipline-modal.component';
import { AddHoursModalComponent } from '../add-hours-modal/add-hours-modal.component';

@Component({
  selector: 'app-discipline-detail',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, HeroCardComponent, InfoCardComponent, DisciplineModalComponent, SpecialtyDisciplineModalComponent, AddHoursModalComponent],
  templateUrl: './discipline-detail.component.html',
  styleUrl: './discipline-detail.component.css',
})
export class DisciplineDetailComponent implements OnInit {
  discipline = signal<DisciplineDTO | null>(null);
  specialtyDisciplines = signal<SpecialtyDisciplineDTO[]>([]);
  specialtyMap = signal<Map<number, Specialty>>(new Map());
  loading = signal(true);
  editModalOpen = signal(false);
  addSdModalOpen = signal(false);
  addHoursTargetSdId = signal<number | null>(null);
  expandedSdIds = signal<Set<number>>(new Set());

  existingSpecialtyIds = () => this.specialtyDisciplines().map((sd) => sd.specialtyId);

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
    return sd ? this.getSpecialtyName(sd.specialtyId) : '';
  }

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private disciplineService = inject(DisciplineService);
  private specialtyService = inject(SpecialtyService);
  private authState = inject(AuthStateService);
  isAdmin = this.authState.isAdmin;

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
        const map = new Map<number, Specialty>();
        specialties.content.forEach((s) => map.set(s.id, s));
        this.specialtyMap.set(map);
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

  getAvatarColor(id: number): string {
    const colors = ['#5B6AF0', '#0D9E6E', '#D97706', '#7C3AED', '#E53E3E', '#0891B2'];
    return colors[id % colors.length];
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
