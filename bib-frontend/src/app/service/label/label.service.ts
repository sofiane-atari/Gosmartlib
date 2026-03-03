import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Label } from '../../model/label.model';

@Injectable({
  providedIn: 'root',
})
export class LabelService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/labels`;

  createLabel(label: Label) {
    this.http.post<Label>(this.url, label);
  }
}
