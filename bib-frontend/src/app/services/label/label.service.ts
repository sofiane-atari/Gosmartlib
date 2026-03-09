import { PageingOpt } from './../../model/paginated-response.model.ts';
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Label, LabelStats } from '../../model/label.model';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../model/paginated-response.model.ts';

export interface labelGetOpt {
  includeBookCount?: boolean;
}

export interface labelGetOptWithPaging {
  labelGetOpt?: labelGetOpt;
  pagingOpt: PageingOpt;
}

@Injectable({
  providedIn: 'root',
})
export class LabelService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/labels`;

  public createLabel(label: Label): Observable<Label> {
    return this.http.post<Label>(this.url, label);
  }
  public getLabels(opt?: labelGetOpt): Observable<Label[]> {
    const params = new HttpParams({
      fromObject: {
        includeBookCount: String(opt?.includeBookCount),
      },
    });

    return this.http.get<Label[]>(this.url, { params });
  }

  public getLabelsByPage(
    opt: labelGetOptWithPaging,
  ): Observable<PaginatedResponse<Label>> {
    const params = new HttpParams({
      fromObject: {
        includeBooksCount: String(opt.labelGetOpt?.includeBookCount ?? false),
        page: String(opt.pagingOpt.page),
        pageSize: String(opt.pagingOpt.pageSize),
      },
    });

    return this.http.get<PaginatedResponse<Label>>(this.url, { params });
  }

  public getLabelStats(): Observable<LabelStats> {
    return this.http.get<LabelStats>(`${this.url}/_stats`);
  }
}
