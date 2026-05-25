import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StepWizardComponent } from './step-wizard.component';

describe('StepWizardComponent', () => {
  let component: StepWizardComponent;
  let fixture: ComponentFixture<StepWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepWizardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StepWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
