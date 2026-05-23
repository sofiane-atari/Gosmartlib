import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Tag } from '../../model/tag/tag.model';
import { TagStats } from '../../model/tag/tag-stats.model';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import { environment } from '../../../environments/environment';

interface GetTagsByPageRequest {
  tagGetOpt: {
    includeBookCount?: boolean;
    search?: string;
    bookCount?: string;
  };
  pagingOpt: {
    page: number;
    pageSize: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class TagService {
  private http = inject(HttpClient);

  private readonly url = `${environment.apiUrl}/tags`;

  getTagsByPage(request: GetTagsByPageRequest): Observable<PaginatedResponse<Tag>> {
    let params = new HttpParams()
      .set('page', request.pagingOpt.page)
      .set('pageSize', request.pagingOpt.pageSize)
      .set('includeBookCount', request.tagGetOpt.includeBookCount ?? false);

    if (request.tagGetOpt.search) {
      params = params.set('search', request.tagGetOpt.search);
    }

    if (request.tagGetOpt.bookCount) {
      params = params.set('bookCount', request.tagGetOpt.bookCount);
    }

    return this.http.get<PaginatedResponse<Tag>>(this.url, { params });
  }

  getAllTags(): Observable<PaginatedResponse<Tag>> {
    return this.getTagsByPage({
      tagGetOpt: { includeBookCount: false },
      pagingOpt: { page: 0, pageSize: 1000 },
    });
  }

  getTagStats(): Observable<TagStats> {
    return this.http.get<TagStats>(`${this.url}/stats`);
  }

  createTag(tag: Partial<Tag>): Observable<Tag> {
    return this.http.post<Tag>(this.url, tag);
  }

  updateTag(id: string, tag: Partial<Tag>): Observable<Tag> {
    return this.http.put<Tag>(`${this.url}/${id}`, tag);
  }

  deleteTag(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
