import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { AuthApiService } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class SetupService {
  private readonly authApi = inject(AuthApiService);
  private cached$: Observable<boolean> | null = null;

  checkSetupRequired(): Observable<boolean> {
    if (!this.cached$) {
      this.cached$ = new Observable<boolean>((observer) => {
        this.authApi.checkSetup().subscribe({
          next: (res) => {
            observer.next(res.setupRequired);
            observer.complete();
          },
          error: () => {
            observer.next(false);
            observer.complete();
          },
        });
      }).pipe(shareReplay(1));
    }
    return this.cached$;
  }

  clearCache(): void {
    this.cached$ = null;
  }
}
