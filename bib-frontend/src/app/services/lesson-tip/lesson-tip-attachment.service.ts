import {
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpResponse,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

import { map, Observable } from 'rxjs';
import {
  AttachmentUploadProgress,
  LessonTipAttachment,
} from '../../model/lesson-tip/lesson-tip-attachment.model';

@Injectable({
  providedIn: 'root',
})
export class LessonTipAttachmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/lesson-tips`;

  public getAllByLessonTipId(
    lessonTipId: number,
  ): Observable<LessonTipAttachment[]> {
    return this.http.get<LessonTipAttachment[]>(
      `${this.apiUrl}/${lessonTipId}/attachments`,
    );
  }

  public upload(
    lessonTipId: number,
    file: File,
  ): Observable<AttachmentUploadProgress> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<LessonTipAttachment>(
        `${this.apiUrl}/${lessonTipId}/attachments`,
        formData,
        {
          observe: 'events',
          reportProgress: true,
        },
      )
      .pipe(
        map((event: HttpEvent<LessonTipAttachment>) =>
          this.mapUploadEvent(event),
        ),
      );
  }

  public download(lessonTipId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/${lessonTipId}/attachments/${attachmentId}/download`,
      {
        responseType: 'blob',
      },
    );
  }

  public delete(lessonTipId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${lessonTipId}/attachments/${attachmentId}`,
    );
  }

  private mapUploadEvent(
    event: HttpEvent<LessonTipAttachment>,
  ): AttachmentUploadProgress {
    if (event.type === HttpEventType.UploadProgress) {
      const total = event.total ?? 0;
      const progress = total > 0 ? Math.round((event.loaded / total) * 100) : 0;

      return {
        progress,
        attachment: null,
      };
    }

    if (event instanceof HttpResponse) {
      return {
        progress: 100,
        attachment: event.body ?? null,
      };
    }

    return {
      progress: 0,
      attachment: null,
    };
  }
}
