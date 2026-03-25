import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Loan } from '../model/loan.model';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoansService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  public createLoan(loan: Loan): Observable<Loan> {
    return this.http.post<Loan>(`${this.apiUrl}/api/loans`, loan);
  }
}
