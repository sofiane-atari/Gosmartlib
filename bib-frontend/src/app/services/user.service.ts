import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { User } from '../model/user.model';

export interface GetUsersOpt {
    WithfirstName?: string;
    WithlastName?: string; 
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
      },
    });

    return this.http.get<User[]>('/api/users', { params });
  }
}