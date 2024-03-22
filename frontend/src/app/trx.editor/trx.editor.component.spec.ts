import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrxEditorComponent } from './trx.editor.component';

describe('TrxEditorComponent', () => {
  let component: TrxEditorComponent;
  let fixture: ComponentFixture<TrxEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrxEditorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TrxEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
