import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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

  ngOnInit() {
    this.http.get<any[]>('http://localhost:8080/api/hello')
      .subscribe({
        next: (data) => {
          this.messages = data;
          console.log('Data ontvangen:', data);
        },
        error: (err) => console.error('Fout bij verbinden:', err)
      });
  }
}
