import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Toast } from '../../model/toast.model';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastSubject = new Subject<Toast>();
  toast$ = this.toastSubject.asObservable();

  show(message: string, type: 'success' | 'error' = 'success') {
    this.toastSubject.next({ message, type });
  }

  showConfirm(message: string, onConfirm: () => void, onCancel?: () => void) {
  this.toastSubject.next({ message, type: 'confirm', onConfirm, onCancel });
  }
}