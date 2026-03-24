import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams, HttpUrlEncodingCodec } from '@angular/common/http';
import { User } from '../model/user.model';

export interface GetUsersOpt {
    WithfirstName?: string;
    WithlastName?: string; 
    WithId?: String;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private http = inject(HttpClient);   

  public getUsers(opt?: GetUsersOpt): Observable<User[]> {
    let params = new HttpParams({
      fromObject: {
        firstName: String(opt?.WithfirstName?? ''),
        lastName: String(opt?.WithlastName ?? ''),
        id: String(opt?.WithId ?? ''),
      },
    });

    return this.http.get<User[]>('/api/users', { params });
  }
}