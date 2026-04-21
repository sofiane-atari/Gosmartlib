import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialBookSelectionPanelComponent } from './didactic-material-book-selection-panel.component';

describe('DidacticMaterialBookSelectionPanelComponent', () => {
  let component: DidacticMaterialBookSelectionPanelComponent;
  let fixture: ComponentFixture<DidacticMaterialBookSelectionPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialBookSelectionPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialBookSelectionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
