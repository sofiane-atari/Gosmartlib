import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialFormPageComponent } from './didactic-material-form-page.component';

describe('DidacticMaterialFormPageComponent', () => {
  let component: DidacticMaterialFormPageComponent;
  let fixture: ComponentFixture<DidacticMaterialFormPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialFormPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialFormPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
