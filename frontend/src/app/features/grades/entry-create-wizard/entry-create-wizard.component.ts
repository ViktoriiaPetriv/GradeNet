import { Component, OnInit, Output, EventEmitter, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { GradeService } from '../../../core/services/grade.service';
import { DisciplineService } from '../../../core/services/discipline.service';
import { GroupService } from '../../../core/services/group.service';
import { UserService } from '../../../core/services/user.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ToastService } from '../../../core/services/toast.service';
import { SpecialtyDisciplineDTO } from '../../../models/discipline.model';
import { StudentGroup, StudentGroupMember } from '../../../models/group.model';
import { User } from '../../../models/user.model';
import { Specialty, SpecialtyOffering } from '../../../models/org.model';

interface StudentRow {
  bookNumberId: number;
  studentId?: number;
  studentName?: string;
  studentEmail?: string;
  groupName: string;
  excluded: boolean;
}

@Component({
  selector: 'app-entry-create-wizard',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './entry-create-wizard.component.html',
  styleUrl: './entry-create-wizard.component.css',
})
export class EntryCreateWizardComponent implements OnInit {
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  step = signal<1 | 2>(1);
  loading = signal(false);
  groupsLoading = signal(false);

  specialties = signal<Specialty[]>([]);
  offerings = signal<SpecialtyOffering[]>([]);
  allSpecialtyDisciplines = signal<SpecialtyDisciplineDTO[]>([]);
  professors = signal<User[]>([]);
  groups = signal<StudentGroup[]>([]);

  selectedSpecialtyId = signal<number | null>(null);
  selectedOfferingId = signal<number | null>(null);
  selectedDisciplineId = signal<number | null>(null);
  selectedProfessorId = signal<number | null>(null);
  academicYear = signal('');
  semester = signal<number>(1);
  reportDate = signal('');
  selectedGroupIds = signal<Set<number>>(new Set());

  students = signal<StudentRow[]>([]);

  filteredDisciplines = computed(() => {
    const offeringId = this.selectedOfferingId();
    const all = this.allSpecialtyDisciplines();
    if (!offeringId) return all;
    return all.filter((sd) => sd.specialtyOfferingId === offeringId);
  });

  private gradeService = inject(GradeService);
  private disciplineService = inject(DisciplineService);
  private groupService = inject(GroupService);
  private userService = inject(UserService);
  private specialtyService = inject(SpecialtyService);
  private toastService = inject(ToastService);

  ngOnInit() {
    this.loading.set(true);
    forkJoin({
      specialties: this.specialtyService.getAll({ size: 200 }),
      disciplines: this.disciplineService.getAllSpecialtyDisciplines(),
      users: this.userService.findAll(),
      groups: this.groupService.getAll({ size: 200 }),
    }).subscribe({
      next: ({ specialties, disciplines, users, groups }) => {
        this.specialties.set(specialties.content ?? []);
        this.allSpecialtyDisciplines.set(disciplines);
        this.professors.set(users.filter((u) => u.role === 'PROFESSOR'));
        this.groups.set(groups.content ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.toastService.error('Помилка завантаження даних');
        this.loading.set(false);
      },
    });
  }

  onSpecialtyChange(id: number | null) {
    this.selectedSpecialtyId.set(id);
    this.selectedOfferingId.set(null);
    this.selectedDisciplineId.set(null);
    this.selectedGroupIds.set(new Set());
    this.offerings.set([]);
    if (id) {
      this.specialtyService.getOfferings(id).subscribe((offs) => this.offerings.set(offs));
    }
    this.reloadGroups(null);
  }

  onOfferingChange(id: number | null) {
    this.selectedOfferingId.set(id);
    this.selectedDisciplineId.set(null);
    this.selectedGroupIds.set(new Set());
    this.reloadGroups(id);
  }

  private reloadGroups(specialtyOfferingId: number | null) {
    this.groupsLoading.set(true);
    this.groupService.getAll({ size: 200, specialtyOfferingId: specialtyOfferingId ?? undefined }).subscribe({
      next: (page) => {
        this.groups.set(page.content ?? []);
        this.groupsLoading.set(false);
      },
      error: () => this.groupsLoading.set(false),
    });
  }

  toggleGroup(id: number) {
    const set = new Set(this.selectedGroupIds());
    if (set.has(id)) set.delete(id);
    else set.add(id);
    this.selectedGroupIds.set(set);
  }

  isGroupSelected(id: number): boolean {
    return this.selectedGroupIds().has(id);
  }

  goToStep2() {
    if (!this.selectedDisciplineId()) {
      this.toastService.error('Виберіть дисципліну');
      return;
    }
    if (!this.selectedProfessorId()) {
      this.toastService.error('Виберіть викладача');
      return;
    }
    if (!this.academicYear().trim()) {
      this.toastService.error('Вкажіть навчальний рік');
      return;
    }
    const groupIds = [...this.selectedGroupIds()];
    if (groupIds.length === 0) {
      this.toastService.error('Виберіть хоча б одну групу');
      return;
    }

    this.loading.set(true);
    forkJoin(groupIds.map((id) => this.groupService.getMembers(id))).subscribe({
      next: (results) => {
        const groupMap = new Map(this.groups().map((g) => [g.id, g.name]));
        const seen = new Set<number>();
        const rows: StudentRow[] = [];

        results.forEach((members, idx) => {
          const groupId = groupIds[idx];
          const groupName = groupMap.get(groupId) ?? `Група ${groupId}`;
          members.forEach((m: StudentGroupMember) => {
            if (!seen.has(m.bookNumberId)) {
              seen.add(m.bookNumberId);
              rows.push({
                bookNumberId: m.bookNumberId,
                studentId: m.studentId,
                studentName: m.studentName,
                studentEmail: m.studentEmail,
                groupName,
                excluded: false,
              });
            }
          });
        });

        this.students.set(rows);
        this.loading.set(false);
        this.step.set(2);
      },
      error: () => {
        this.toastService.error('Помилка завантаження студентів');
        this.loading.set(false);
      },
    });
  }

  toggleExclude(row: StudentRow) {
    row.excluded = !row.excluded;
    this.students.set([...this.students()]);
  }

  activeStudents() {
    return this.students().filter((s) => !s.excluded);
  }

  confirm() {
    const active = this.activeStudents();
    if (active.length === 0) {
      this.toastService.error("Немає студентів для прив'язки");
      return;
    }

    const req = {
      specialtyDisciplineId: this.selectedDisciplineId()!,
      professorId: this.selectedProfessorId()!,
      academicYear: this.academicYear().trim(),
      bookNumberIds: active.map((s) => s.bookNumberId),
      reportDate: this.reportDate() || undefined,
      semester: this.semester(),
    };

    this.loading.set(true);
    this.gradeService.createEntries(req).subscribe({
      next: () => {
        this.toastService.success(`Створено записи для ${active.length} студентів`);
        this.saved.emit();
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Помилка створення записів');
        this.loading.set(false);
      },
    });
  }

  back() {
    this.step.set(1);
  }

  specialtyLabel(s: Specialty): string {
    return `${s.code} ${s.nameUA}`;
  }

  offeringLabel(o: SpecialtyOffering): string {
    return `Рік випуску: ${o.graduationYear}`;
  }

  disciplineLabel(sd: SpecialtyDisciplineDTO): string {
    return sd.discipline.name;
  }

  professorLabel(u: User): string {
    return [u.lastName, u.firstName, u.patronymic].filter(Boolean).join(' ');
  }
}
