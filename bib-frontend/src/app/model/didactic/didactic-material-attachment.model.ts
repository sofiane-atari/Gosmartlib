export interface DidacticMaterialAttachment {
  id: number;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  uploadedAt: string;
}

export interface AttachmentUploadProgress {
  progress: number;
  attachment: DidacticMaterialAttachment | null;
}
