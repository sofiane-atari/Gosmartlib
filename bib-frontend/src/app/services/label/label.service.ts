import { PageingOpt } from './../../model/paginated-response.model.ts';
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Label, LabelStats } from '../../model/label.model';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../model/paginated-response.model.ts';
import { Search } from 'lucide-angular';

export interface labelGetOpt {
  includeBookCount?: boolean;
  bookCount?: string;
  search?: string;
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
    let params = new HttpParams({
      fromObject: {
        includeBookCount: String(opt.labelGetOpt?.includeBookCount ?? false),
        page: String(opt.pagingOpt.page),
        pageSize: String(opt.pagingOpt.pageSize),
      },
    });

    // Add search parameter if provided
    if (opt.labelGetOpt?.search) {
      params = params.set('search', opt.labelGetOpt.search);
    }

    // Add bookCount filter if provided
    if (opt.labelGetOpt?.bookCount) {
      params = params.set('bookCount', opt.labelGetOpt.bookCount);
    }

    return this.http.get<PaginatedResponse<Label>>(this.url, { params });
  }

  public getLabelStats(): Observable<LabelStats> {
    return this.http.get<LabelStats>(`${this.url}/_stats`);
  }

  public deleteLabelByName(name: string) {
    return this.http.delete<void>(`${this.url}/${encodeURIComponent(name)}`);
  }
}
