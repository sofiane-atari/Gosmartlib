import { PageingOpt } from '../../model/paginated-response.model.ts.js';
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { environment } from '../../../environments/environment.js';
import { Genre, GenreStats } from '../../model/genre.model.js';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../model/paginated-response.model.ts.js';
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
export class GenreService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/genres`;

  public createLabel(genre: Genre): Observable<Genre> {
    return this.http.post<Genre>(this.url, genre);
  }
  getLabels() {
    return this.http.get<Genre[]>(`${this.url}/all`);
  }

  public getLabelsByPage(
    opt: labelGetOptWithPaging,
  ): Observable<PaginatedResponse<Genre>> {
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

    return this.http.get<PaginatedResponse<Genre>>(this.url, { params });
  }

  public getLabelStats(): Observable<GenreStats> {
    return this.http.get<GenreStats>(`${this.url}/_stats`);
  }

  public deleteLabelByName(name: string) {
    return this.http.delete<void>(`${this.url}/${encodeURIComponent(name)}`);
  }

  public updateLabel(label: Genre): Observable<Genre> {
    return this.http.put<Genre>(
      `${this.url}/${encodeURIComponent(label.name)}`,
      label,
    );
  }
}
