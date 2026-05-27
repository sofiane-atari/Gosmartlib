import { Directive, input, TemplateRef } from '@angular/core';

@Directive({
  selector: '[cellTemplate]',
  standalone: true,
})
export class CellTemplateDirective {
  cellTemplate = input.required<string>();

  constructor(public templateRef: TemplateRef<unknown>) {}
}
