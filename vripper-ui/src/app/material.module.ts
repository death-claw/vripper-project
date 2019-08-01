import { NgModule } from '@angular/core';
import {
    MatButtonModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatToolbarModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatDialogModule,
    MatCardModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatSnackBar,
    MatTabsModule,
    MatDividerModule
} from '@angular/material';

@NgModule({
    exports: [
        MatButtonModule,
        MatCheckboxModule,
        MatProgressBarModule,
        MatToolbarModule,
        MatIconModule,
        MatInputModule,
        MatFormFieldModule,
        MatProgressSpinnerModule,
        MatMenuModule,
        MatDialogModule,
        MatCardModule,
        MatSlideToggleModule,
        MatSnackBarModule,
        MatTabsModule,
        MatDividerModule
    ],
    providers: [MatSnackBar]
})
export class MaterialModule { }
