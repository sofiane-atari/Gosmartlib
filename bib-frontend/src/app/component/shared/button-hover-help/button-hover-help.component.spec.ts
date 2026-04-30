import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ButtonHoverHelpComponent } from './button-hover-help.component';

describe('ButtonHoverHelpComponent', () => {
  let component: ButtonHoverHelpComponent;
  let fixture: ComponentFixture<ButtonHoverHelpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonHoverHelpComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ButtonHoverHelpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
