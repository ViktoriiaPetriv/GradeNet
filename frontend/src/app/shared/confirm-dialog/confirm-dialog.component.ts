import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [ModalComponent, NgClass],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css']
})
export class ConfirmDialogComponent {
  @Input() title: string = 'Підтвердження';
  @Input() message: string = '';
  @Input() subtext: string = '';
  @Input() confirmLabel: string = 'Підтвердити';
  @Input() cancelLabel: string = 'Скасувати';
  @Input() iconType: 'danger' | 'warning' | 'purple' = 'danger';
  @Input() confirmButtonClass: string = 'btn-danger'; // можна 'btn-confirm-warning' або 'btn-confirm-purple'

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  get iconClass(): string {
    switch (this.iconType) {
      case 'warning': return 'confirm-warning';
      case 'purple': return 'confirm-purple';
      default: return 'confirm-danger';
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }

  onConfirm(): void {
    this.confirm.emit();
  }
}
