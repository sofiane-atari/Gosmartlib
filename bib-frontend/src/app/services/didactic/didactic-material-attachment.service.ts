import {
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpResponse,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  AttachmentUploadProgress,
  DidacticMaterialAttachment,
} from '../../model/didactic/didactic-material-attachment.model';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class DidacticMaterialAttachmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/didactic-materials`;

  public getAllByMaterialId(
    didacticMaterialId: number,
  ): Observable<DidacticMaterialAttachment[]> {
    return this.http.get<DidacticMaterialAttachment[]>(
      `${this.apiUrl}/${didacticMaterialId}/attachments`,
    );
  }

  public upload(
    didacticMaterialId: number,
    file: File,
  ): Observable<AttachmentUploadProgress> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<DidacticMaterialAttachment>(
        `${this.apiUrl}/${didacticMaterialId}/attachments`,
        formData,
        {
          observe: 'events',
          reportProgress: true,
        },
      )
      .pipe(
        map((event: HttpEvent<DidacticMaterialAttachment>) =>
          this.mapUploadEvent(event),
        ),
      );
  }

  public download(
    didacticMaterialId: number,
    attachmentId: number,
  ): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/${didacticMaterialId}/attachments/${attachmentId}/download`,
      {
        responseType: 'blob',
      },
    );
  }

  public delete(
    didacticMaterialId: number,
    attachmentId: number,
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${didacticMaterialId}/attachments/${attachmentId}`,
    );
  }

  private mapUploadEvent(
    event: HttpEvent<DidacticMaterialAttachment>,
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
