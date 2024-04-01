import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryCtrlComponent } from './category.ctrl.component';

describe('CategoryCtrlComponent', () => {
  let component: CategoryCtrlComponent;
  let fixture: ComponentFixture<CategoryCtrlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CategoryCtrlComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CategoryCtrlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
