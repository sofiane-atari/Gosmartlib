import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './home.component.html',
    styleUrl: './home.component.css'
})
export class HomeComponent {
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
        if (!this.newMessage.trim()) return;

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
