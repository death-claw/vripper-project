import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DownloadTableComponent } from './download-table.component';

describe('DownloadTableComponent', () => {
  let component: DownloadTableComponent;
  let fixture: ComponentFixture<DownloadTableComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DownloadTableComponent],
    });
    fixture = TestBed.createComponent(DownloadTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
