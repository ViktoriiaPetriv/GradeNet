import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  @Input() title: string = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showClose = true;
  @Input() closeOnBackdrop = true;

  @Output() close = new EventEmitter<void>();

  onBackdropClick(): void {
    if (this.closeOnBackdrop) {
      this.close.emit();
    }
  }
}
