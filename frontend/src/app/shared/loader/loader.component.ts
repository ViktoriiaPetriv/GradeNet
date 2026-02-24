import { Component, inject } from '@angular/core';
import { LoaderService } from '../../core/services/loader.service';

@Component({
  selector: 'app-loader',
  standalone: true,
  template: `
    @if (loaderService.isLoading()) {
      <div class="loader-overlay">
        <div class="loader-spinner"></div>
      </div>
    }
  `,
  styleUrl: './loader.component.css',
})
export class LoaderComponent {
  loaderService = inject(LoaderService);
}
