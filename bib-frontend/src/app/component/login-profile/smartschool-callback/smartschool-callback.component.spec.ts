import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SmartschoolCallbackComponent } from './smartschool-callback.component';

describe('SmartschoolCallbackComponent', () => {
  let component: SmartschoolCallbackComponent;
  let fixture: ComponentFixture<SmartschoolCallbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SmartschoolCallbackComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SmartschoolCallbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
