import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Loan, LoanHistory } from '../../model/loan.model';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import { LoanExtensionRequest } from '../../model/loan/loanExtension.model';

@Injectable({
  providedIn: 'root',
})
export class LoansService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl + '/loans';

  public createLoan(loan: Loan): Observable<Loan> {
    return this.http.post<Loan>(`${this.apiUrl}`, loan);
  }

  public getLoans(
    page: number,
    pageSize: number,
  ): Observable<PaginatedResponse<Loan>> {
    return this.http.get<PaginatedResponse<Loan>>(`${this.apiUrl}`, {
      params: {
        page,
        pageSize,
      },
    });
  }

  public getActiveLoans(
    page: number,
    pageSize: number,
  ): Observable<PaginatedResponse<Loan>> {
    return this.http.get<PaginatedResponse<Loan>>(`${this.apiUrl}/active`, {
      params: {
        page: page.toString(),
        pageSize: pageSize.toString(),
      },
    });
  }

  public returnLoan(loanId: number): Observable<Loan> {
    return this.http.patch<Loan>(`${this.apiUrl}/${loanId}/return`, {});
  }

  public getMyLoanHistory(
    page: number,
    pageSize: number,
  ): Observable<LoanHistory> {
    return this.http.get<LoanHistory>(`${this.apiUrl}/me/history`, {
      params: {
        page: page.toString(),
        pageSize: pageSize.toString(),
      },
    });
  }

  public extendLoan(
    loanId: number,
    request: LoanExtensionRequest,
  ): Observable<Loan> {
    return this.http.patch<Loan>(`${this.apiUrl}/${loanId}/extend`, request);
  }

  getLoanStats() {
    return this.http.get<{
      activeLoans: number;
      overdueLoans: number;
      returnedLoans: number;
    }>(`${this.apiUrl}/stats`);
  }
}
