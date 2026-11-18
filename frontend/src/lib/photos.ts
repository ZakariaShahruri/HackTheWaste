/** The backend downscales to 1280 px anyway; a bit more leaves room for its own crop. */
export const MAX_PHOTO_SIDE = 1600;

export function fitWithin(width: number, height: number, maxSide: number): { width: number; height: number } {
  const scale = Math.min(1, maxSide / Math.max(width, height));
  return { width: Math.round(width * scale), height: Math.round(height * scale) };
}

/**
 * A phone photo as an upright JPEG of at most MAX_PHOTO_SIDE pixels. Phones
 * store portrait shots sideways with an EXIF rotation the backend ignores, and
 * a raw shot can pass the 5 MB upload limit. Formats the browser can't decode
 * go up unchanged, so the backend can explain what's wrong with them.
 */
export async function shrinkPhoto(file: Blob, maxSide = MAX_PHOTO_SIDE): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
  } catch {
    return file;
  }
  const { width, height } = fitWithin(bitmap.width, bitmap.height, maxSide);
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  canvas.getContext("2d")?.drawImage(bitmap, 0, 0, width, height);
  bitmap.close();
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob ?? file), "image/jpeg", 0.85));
}
