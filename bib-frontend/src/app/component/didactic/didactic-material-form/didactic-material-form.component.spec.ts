import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialFormComponent } from './didactic-material-form.component';

describe('DidacticMaterialFormComponent', () => {
  let component: DidacticMaterialFormComponent;
  let fixture: ComponentFixture<DidacticMaterialFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
