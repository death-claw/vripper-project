export class PostDetails {

    constructor(public postId: string, public postName: string, public url: string, public progress: number, public status: string) {
      this.postId = postId;
      this.postName = postName;
      this.url = url;
      this.progress = progress;
      this.status = status;
    }
  }
