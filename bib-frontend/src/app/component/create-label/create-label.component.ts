import { Component, inject, InjectionToken } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-create-label',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './create-label.component.html',
  styleUrl: './create-label.component.css',
})
export class CreateLabelComponent {
  private formBuilder = inject(FormBuilder);
  labelForm = this.formBuilder.group({
    labelName: ['', Validators.required],
    color: ['', Validators.required],
    description: ['', Validators.required],
  });
}
