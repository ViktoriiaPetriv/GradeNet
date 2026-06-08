import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommissionService } from '../../../core/services/commission.service';
import { UserService } from '../../../core/services/user.service';
import { Commission, CommissionMember } from '../../../models/commission.model';
import { User } from '../../../models/user.model';
import { ToastService } from '../../../core/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { HeroCardComponent } from '../../../shared/hero-card/hero-card.component';
import { InfoCardComponent } from '../../../shared/info-card/info-card.component';
import { ModalComponent } from '../../../shared/modal/modal.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-commission-detail',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, HeroCardComponent, InfoCardComponent, ModalComponent, ConfirmDialogComponent, ReactiveFormsModule],
  templateUrl: './commission-detail.component.html',
  styleUrl: './commission-detail.component.css',
})
export class CommissionDetailComponent implements OnInit {
  commission = signal<Commission | null>(null);
  professors = signal<User[]>([]);

  addMemberModalOpen = signal(false);
  memberToRemove = signal<CommissionMember | null>(null);
  removeModalOpen = signal(false);

  addForm!: FormGroup;

  private route = inject(ActivatedRoute);
  private commissionService = inject(CommissionService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);
  private authState = inject(AuthStateService);

  isAdminOrManager = this.authState.isAdminOrManager;

  professorMap = computed(() => {
    const map: Record<number, User> = {};
    this.professors().forEach(p => (map[p.id] = p));
    return map;
  });

  availableProfessors = computed(() => {
    const memberIds = new Set((this.commission()?.members ?? []).map(m => m.professorId));
    return this.professors().filter(p => !memberIds.has(p.id));
  });

  hasHead = computed(() => (this.commission()?.members ?? []).some(m => m.isHead));

  ngOnInit() {
    this.addForm = this.fb.group({
      professorId: [null, Validators.required],
      isHead: [false],
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
    this.userService.getProfessors().subscribe(data => this.professors.set(data));
  }

  load(id: number) {
    this.commissionService.getById(id).subscribe(data => this.commission.set(data));
  }

  openAddMember() {
    this.addForm.reset({ professorId: null, isHead: false });
    this.addMemberModalOpen.set(true);
  }

  closeAddMember() {
    this.addMemberModalOpen.set(false);
  }

  submitAddMember() {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const c = this.commission();
    if (!c) return;
    this.commissionService.addMember(c.id, this.addForm.value).subscribe(() => {
      this.toastService.success('Члена комісії додано');
      this.load(c.id);
      this.closeAddMember();
    });
  }

  openRemoveMember(member: CommissionMember) {
    this.memberToRemove.set(member);
    this.removeModalOpen.set(true);
  }

  closeRemoveMember() {
    this.removeModalOpen.set(false);
    this.memberToRemove.set(null);
  }

  confirmRemoveMember() {
    const c = this.commission();
    const m = this.memberToRemove();
    if (!c || !m) return;
    this.commissionService.removeMember(c.id, m.id).subscribe(() => {
      this.toastService.success('Члена комісії видалено');
      this.load(c.id);
      this.closeRemoveMember();
    });
  }

  getProfessorName(professorId: number): string {
    const p = this.professorMap()[professorId];
    if (!p) return `ID: ${professorId}`;
    return `${p.lastName} ${p.firstName}${p.patronymic ? ' ' + p.patronymic : ''}`;
  }

  isActive(c: Commission): boolean {
    if (!c.endDate) return true;
    return new Date(c.endDate) >= new Date(new Date().toDateString());
  }

  formatDate(date: string): string {
    if (!date) return '—';
    const [y, m, d] = date.split('-');
    return `${d}.${m}.${y}`;
  }

  isInvalid(f: string): boolean {
    const c = this.addForm.get(f);
    return !!(c?.invalid && c?.touched);
  }
}
