import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
   http = inject(HttpClient);
  messages: any[] = [];
  private baseUrl = `${environment.apiUrl}/hello`;

  ngOnInit() {
    this.http.get<any[]>(this.baseUrl)
      .subscribe({
        next: (data) => {
          this.messages = data;
          console.log('Data ontvangen:', data);
        },
        error: (err) => console.error('Fout bij verbinden:', err)
      });
  }
}
