import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Loan } from '../model/loan.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoansService {
  private http = inject(HttpClient);

  public createLoan(loan: Loan): Observable<Loan> {
    return this.http.post<Loan>('/api/loans', loan);
  }
}
