package in.gov.pmegp.pv.core.upload;

import java.util.Locale;

/**
 * Decides what may be uploaded and what must be compressed first.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C04-image-capture-and-compression.md
 *
 * <p>Two gates with deliberately different conditions. Getting either wrong in either
 * direction reproduces a defect that was already reported and fixed.
 */
public final class UploadGate {

    /** C04-R07: 2 MB BINARY — 2,097,152 bytes. Not 2,000,000. */
    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private UploadGate() { }

    /** C04-R11: the compressible set is narrower than the accepted set. */
    public static boolean isCompressible(String fileName) {
        String ext = extensionOf(fileName);
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
        // F-003: .webp and .heic are NOT here, so an oversized one is rejected although
        // Android could compress it. Open defect — widen only with a tester sign-off.
    }

    /**
     * C04-R03 — the PICK-time gate. Rejects only what cannot be compressed.
     *
     * <p>An oversized JPEG or PNG is ACCEPTED here and compressed later. Rejecting any
     * oversized pick WAS the defect: an ordinary 3–6 MB phone photo was refused outright
     * with no way for the officer to shrink it, while the same photo taken through the
     * in-app camera went through fine.
     *
     * <p>C04-R04: a size of 0 (unreadable) falls through; the upload gate catches it.
     */
    public static boolean rejectAtPick(String fileName, long sizeBytes) {
        if (sizeBytes <= 0) return false;
        return sizeBytes > MAX_BYTES && !isCompressible(fileName);
    }

    /**
     * C04-R06 — the UPLOAD-time gate. BOTH conditions are load-bearing.
     *
     * <p>Dropping the size test re-encodes a camera photo that was already compressed at
     * capture: two JPEG quantisation passes, visible generation loss, no size benefit.
     * That was a reported defect.
     *
     * <p>Dropping the extension test hands a PDF to a JPEG encoder — harmless but pointless.
     */
    public static boolean shouldCompressBeforeUpload(String fileName, long sizeBytes) {
        return sizeBytes > MAX_BYTES && isCompressible(fileName);
    }

    /** C04-R08 — the hard limit, applied AFTER any compression. Abort, send nothing. */
    public static boolean rejectAfterCompression(long sizeBytes) {
        return sizeBytes > MAX_BYTES;
    }

    /**
     * C04-R13 — content sniffing, as Bank Physical Verification does it.
     *
     * <p>This is the CORRECT approach. Unit Verification uses the filename instead
     * (C04-R12 / F-002), so a PNG compressed to JPEG is uploaded labelled ".png".
     * Unifying on this method changes UV behaviour — needs a tester sign-off.
     */
    public static String detectExtension(byte[] data) {
        if (data == null || data.length < 4) return ".jpg";
        if (data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46) return ".pdf";
        if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) return ".png";
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) return ".jpg";
        return ".jpg";
    }

    static String extensionOf(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
