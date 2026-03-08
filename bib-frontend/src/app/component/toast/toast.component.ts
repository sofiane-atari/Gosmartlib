import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { Toast } from '../../model/toast.model';
import { ToastService } from '../../services/Toast/toast.service';


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
  private timeoutId: any;

  constructor(private toastService: ToastService) {
    this.toastService.toast$.subscribe(t => {
      this.toast = t;
      
      if (this.timeoutId) {
        clearTimeout(this.timeoutId);
      }

      if (t.type !== 'confirm') {
        this.timeoutId = setTimeout(() => this.toast = undefined, 4000);
      }
    });
  }

  confirm() {
  if (this.toast?.onConfirm) this.toast.onConfirm();
  this.toast = undefined;
  }

  cancel() {
    if (this.toast?.onCancel) this.toast.onCancel();
    this.toast = undefined;
  }
}
