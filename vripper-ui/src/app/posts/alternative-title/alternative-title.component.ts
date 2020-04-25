import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {RenamePostModel} from "../../common/rename-post.model";
import {HttpClient} from "@angular/common/http";
import {ServerService} from "../../server-service";
import {MatSnackBar} from "@angular/material/snack-bar";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {PostState} from "../post-state.model";

export interface AlternativeTitleDialog {
  post: PostState
}

@Component({
  selector: 'app-alternative-title',
  templateUrl: './alternative-title.component.html',
  styleUrls: ['./alternative-title.component.scss']
})
export class AlternativeTitleComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<AlternativeTitleComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AlternativeTitleDialog,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar) {
  }

  ngOnInit(): void {
    this.form.get('altNameSelect').valueChanges.subscribe(v => this.form.get('altNameInput').setValue(v));
  }

  form = new FormGroup({
    altNameSelect: new FormControl(''),
    altNameInput: new FormControl('', Validators.required)
  });

  rename() {
    this.httpClient.post<RenamePostModel>(this.serverService.baseUrl + '/post/rename', [{
      postId: this.data.post.postId,
      altName: this.form.get('altNameInput').value
    }]).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

}
