import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private messageService = inject(MessageService);

  success(message: string) {
    this.messageService.add({ severity: 'success', summary: 'Успіх', detail: message, life: 4000 });
  }

  error(message: string) {
    this.messageService.add({ severity: 'error', summary: 'Помилка', detail: message, life: 5000 });
  }

  warning(message: string) {
    this.messageService.add({ severity: 'warn', summary: 'Увага', detail: message, life: 4000 });
  }

  info(message: string) {
    this.messageService.add({ severity: 'info', summary: 'Інфо', detail: message, life: 4000 });
  }
}
