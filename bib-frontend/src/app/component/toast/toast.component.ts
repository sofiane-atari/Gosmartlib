import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { Toast } from '../../model/toast.model';
import { ToastService } from '../../services/Toast/toast.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';


@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.css',
  animations: [
    trigger('fade', [
      state('void', style({ opacity: 0, transform: 'translateY(-20px)' })),
      transition(':enter', [animate('200ms ease-out')]),
      transition(':leave', [animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-20px)' }))])
    ])
  ]
})
export class ToastComponent {
  toast?: Toast;
  private readonly destroyRef = inject(DestroyRef);
  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  constructor(private toastService: ToastService) {
    this.toastService.toast$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(t => {
      this.toast = t;

      if (this.timeoutId) {
        clearTimeout(this.timeoutId);
        this.timeoutId = null;
      }

      if (t.type !== 'confirm') {
        this.timeoutId = setTimeout(() => {
          this.toast = undefined;
          this.timeoutId = null;
        }, 5000);
      }
    });
  }

  close() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }

    this.toast = undefined;
  }

  confirm() {
  if (this.toast?.onConfirm) this.toast.onConfirm();
  this.close();
  }

  cancel() {
    if (this.toast?.onCancel) this.toast.onCancel();
    this.close();
  }
}
