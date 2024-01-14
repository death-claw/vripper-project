export class Post {
  constructor(
    public postId: number,
    public postTitle: string,
    public status: string,
    public url: string,
    public done: number,
    public total: number,
    public hosts: string[],
    public addedOn: string,
    public rank: number,
    public downloadDirectory: string,
    public folderName: string,
    public downloadFolder: string,
    public downloaded: number
  ) {}
}
