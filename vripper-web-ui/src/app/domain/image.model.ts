export class Image {
  constructor(
    public postId: number,
    public url: string,
    public status: string,
    public index: number,
    public downloaded: number,
    public size: number
  ) {}
}
