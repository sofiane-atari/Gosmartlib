import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, ɵEmptyOutletComponent } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { FormsModule } from '@angular/forms';
import { BooksComponent } from './component/books/books.component';
import { SidebarComponent } from "./component/sidebar/sidebar.component";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, BooksComponent, SidebarComponent, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
    http = inject(HttpClient);
    private baseUrl = `${environment.apiUrl}/hello`;
    
    cards = ['Catalogus', 'Favorietenlijst', 'Mijn Account', 'Instellingen', 'BookBox', 'Rapporteer'];
}

