import { useCallback, useEffect, useState } from "react";

export interface LocalPhoto {
  file: File;
  /** A temporary URL to preview the file with. */
  url: string;
}

/** A photo picked on this device, with a preview URL that's released when it's replaced. */
export function useLocalPhoto(): [LocalPhoto | null, (file: File | null) => void] {
  const [photo, setPhoto] = useState<LocalPhoto | null>(null);

  useEffect(() => {
    return () => {
      if (photo) URL.revokeObjectURL(photo.url);
    };
  }, [photo]);

  const pick = useCallback((file: File | null) => {
    setPhoto(file ? { file, url: URL.createObjectURL(file) } : null);
  }, []);

  return [photo, pick];
}
