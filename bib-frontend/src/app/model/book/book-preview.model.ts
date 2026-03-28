export interface BookPreview {
  previewAvailable: boolean;
  previewUrl: string | null;
  detailsUrl: string;
  message: string;
  embedUrl: string | null;
}
