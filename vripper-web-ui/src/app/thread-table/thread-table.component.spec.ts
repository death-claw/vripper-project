import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThreadTableComponent } from './thread-table.component';

describe('ThreadTableComponent', () => {
  let component: ThreadTableComponent;
  let fixture: ComponentFixture<ThreadTableComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ThreadTableComponent],
    });
    fixture = TestBed.createComponent(ThreadTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
