import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-filter',
  templateUrl: './filter.component.html',
  styleUrls: ['./filter.component.scss']
})
export class FilterComponent implements OnInit {

  constructor() { }

  criteria = [
    {value: 'All', selected: true},
    {value: 'Downloading', selected: false},
    {value: 'Completed', selected: false},
    {value: 'Error', selected: false},
  ];

  select(chip: {value: string, selected: boolean}) {
    this.criteria.forEach(e => e.selected = false);
    chip.selected = true;
  }

  ngOnInit() {
  }
}
