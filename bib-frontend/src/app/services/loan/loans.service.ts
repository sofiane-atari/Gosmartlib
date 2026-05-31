import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Loan, LoanHistory, LoanReturnRequest } from '../../model/loan/loan.model';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import { LoanExtensionRequest } from '../../model/loan/loanExtension.model';
import { LoanGroup, LoanGroupStatus } from '../../model/loan/loanGroup.model';

@Injectable({
  providedIn: 'root',
})
export class LoansService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl + '/loans';

  public createLoans(loan: Loan): Observable<Loan[]> {
    return this.http.post<Loan[]>(`${this.apiUrl}`, loan);
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

  public returnLoan(
    loanId: number,
    request: LoanReturnRequest = { copyConditions: [] },
  ): Observable<Loan> {
    return this.http.patch<Loan>(`${this.apiUrl}/${loanId}/return`, request);
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

  public getGroupedLoans(
    page: number,
    pageSize: number,
    search?: string,
    status?: LoanGroupStatus | 'ALL',
    extendable?: boolean | null,
  ): Observable<PaginatedResponse<LoanGroup>> {
    const params: Record<string, string | number | boolean> = {
      page,
      pageSize,
    };

    if (search?.trim()) {
      params['search'] = search.trim();
    }

    if (status && status !== 'ALL') {
      params['status'] = status;
    }

    if (extendable !== null && extendable !== undefined) {
      params['extendable'] = extendable;
    }

    return this.http.get<PaginatedResponse<LoanGroup>>(
      `${this.apiUrl}/grouped`,
      { params },
    );
  }

  public getActiveGroupedLoans(
    page: number,
    pageSize: number,
    search?: string,
  ): Observable<PaginatedResponse<LoanGroup>> {
    const params: Record<string, string | number> = {
      page,
      pageSize,
    };

    if (search?.trim()) {
      params['search'] = search.trim();
    }

    return this.http.get<PaginatedResponse<LoanGroup>>(
      `${this.apiUrl}/active/grouped`,
      { params },
    );
  }

  public getActiveBatchLoans(
    page: number,
    pageSize: number,
    search?: string,
  ): Observable<PaginatedResponse<LoanGroup>> {
    const params: Record<string, string | number> = {
      page,
      pageSize,
    };

    if (search?.trim()) {
      params['search'] = search.trim();
    }

    return this.http.get<PaginatedResponse<LoanGroup>>(
      `${this.apiUrl}/active/batches`,
      { params },
    );
  }
}
