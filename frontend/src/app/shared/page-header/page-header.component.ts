import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './page-header.component.html',
  styleUrls: ['./page-header.component.css']
})
export class PageHeaderComponent {
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() showBack = false;
  @Input() backLink: string | any[] | null = null; // для routerLink

  @Output() back = new EventEmitter<void>();

  onBack(event: Event): void {
    if (this.backLink == null) {
      event.preventDefault();
      this.back.emit();
    }
  }
}
