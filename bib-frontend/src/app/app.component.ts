import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
    http = inject(HttpClient);
    messages: any[] = [];
    private baseUrl = `${environment.apiUrl}/hello`;
    newMessage: string = '';

  ngOnInit() {
    this.fetchMessages();
  }

  fetchMessages() {
    this.http.get<any[]>(this.baseUrl)
      .subscribe({
        next: (data) => {
          this.messages = data;
          console.log('Data ontvangen:', data);
        },
        error: (err) => console.error('Fout bij ophalen:', err)
      });
  }

  addMessage() {
    if (!this.newMessage.trim()) return; // Niet verzenden als leeg

    const postData = { message: this.newMessage };

    this.http.post(this.baseUrl, postData)
      .subscribe({
        next: (response) => {
          console.log('Opgeslagen:', response);
          this.newMessage = ''; 
          this.fetchMessages();
        },
        error: (err) => console.error('Fout bij opslaan:', err)
      });
  }


}
