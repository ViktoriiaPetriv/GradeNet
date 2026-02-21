import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  isOpen = signal(false);

  toggle() {
    this.isOpen.update((v) => !v);
  }
  close() {
    this.isOpen.set(false);
  }
}
