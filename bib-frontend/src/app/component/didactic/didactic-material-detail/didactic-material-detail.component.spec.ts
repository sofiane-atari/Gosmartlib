import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialDetailComponent } from './didactic-material-detail.component';

describe('DidacticMaterialDetailComponent', () => {
  let component: DidacticMaterialDetailComponent;
  let fixture: ComponentFixture<DidacticMaterialDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialDetailComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
