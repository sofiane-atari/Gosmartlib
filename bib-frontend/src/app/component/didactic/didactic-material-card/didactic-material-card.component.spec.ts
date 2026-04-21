import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialCardComponent } from './didactic-material-card.component';

describe('DidacticMaterialCardComponent', () => {
  let component: DidacticMaterialCardComponent;
  let fixture: ComponentFixture<DidacticMaterialCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
