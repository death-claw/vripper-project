import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogTableComponent } from './log-table.component';

describe('LogTableComponent', () => {
  let component: LogTableComponent;
  let fixture: ComponentFixture<LogTableComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogTableComponent],
    });
    fixture = TestBed.createComponent(LogTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
