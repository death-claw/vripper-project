import {NgModule} from '@angular/core';
import {MatBadgeModule} from '@angular/material/badge';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatChipsModule} from '@angular/material/chips';
import {MatDialogModule} from '@angular/material/dialog';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatListModule} from '@angular/material/list';
import {MatMenuModule} from '@angular/material/menu';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTabsModule} from '@angular/material/tabs';
import {MatToolbarModule} from '@angular/material/toolbar';

@NgModule({
  exports: [
    MatButtonModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatToolbarModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
        MatMenuModule,
        MatDialogModule,
        MatCardModule,
        MatSlideToggleModule,
        MatSnackBarModule,
        MatTabsModule,
        MatDividerModule,
        MatChipsModule,
        MatListModule,
        MatProgressSpinnerModule,
        MatBadgeModule
    ],
    providers: [MatSnackBar]
})
export class MaterialModule { }
