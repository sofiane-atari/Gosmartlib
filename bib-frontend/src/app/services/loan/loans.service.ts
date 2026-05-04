import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Loan, LoanHistory } from '../../model/loan.model';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';

@Injectable({
  providedIn: 'root',
})
export class LoansService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  public createLoan(loan: Loan): Observable<Loan> {
    return this.http.post<Loan>(`${this.apiUrl}/loans`, loan);
  }

  public getLoans(page: number, pageSize: number): Observable<PaginatedResponse<Loan>> {
    return this.http.get<PaginatedResponse<Loan>>(`${this.apiUrl}/loans`, {
      params: {
        page,
        pageSize
      }
    });
  }

  public getActiveLoans(page: number, pageSize: number): Observable<PaginatedResponse<Loan>> {
    return this.http.get<PaginatedResponse<Loan>>(`${this.apiUrl}/loans/active`, {
      params: {
        page: page.toString(),
        pageSize: pageSize.toString()
      }
    });
  }

  public returnLoan(loanId: number): Observable<Loan> {
    return this.http.patch<Loan>(`${this.apiUrl}/loans/${loanId}/return`, {});
  }

  public getMyLoanHistory(): Observable<LoanHistory> {
    return this.http.get<LoanHistory>(`${this.apiUrl}/loans/me/history`);
  }
}
