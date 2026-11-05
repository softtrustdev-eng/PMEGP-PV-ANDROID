package in.gov.pmegp.pv.core;

import static org.junit.Assert.*;
import org.junit.Test;
import in.gov.pmegp.pv.core.upload.UploadGate;

/** C04 — both gates, including the two defects they prevent. */
public class UploadGateTest {

    private static final long FIVE_MB = 5L * 1024 * 1024;
    private static final long ONE_MB  = 1L * 1024 * 1024;

    @Test public void c04_r07_limitIsBinaryTwoMegabytes() {
        assertEquals(2_097_152L, UploadGate.MAX_BYTES);
    }

    /** C04-R03 — the fixed defect: an oversized phone photo must be ACCEPTED. */
    @Test public void c04_r03_oversizedJpegIsAcceptedAtPick() {
        assertFalse(UploadGate.rejectAtPick("photo.jpg", FIVE_MB));
        assertFalse(UploadGate.rejectAtPick("photo.JPEG", FIVE_MB));
        assertFalse(UploadGate.rejectAtPick("scan.png", FIVE_MB));
    }

    @Test public void c04_r03_oversizedPdfIsRejectedAtPick() {
        assertTrue(UploadGate.rejectAtPick("report.pdf", FIVE_MB));
    }

    @Test public void c04_r03_smallPdfIsAccepted() {
        assertFalse(UploadGate.rejectAtPick("report.pdf", ONE_MB));
    }

    @Test public void c04_r04_unreadableSizeFallsThrough() {
        assertFalse(UploadGate.rejectAtPick("report.pdf", 0));
    }

    /** C04-R06 — the fixed defect: an already-compressed camera photo must NOT be re-encoded. */
    @Test public void c04_r06_smallJpegIsNotRecompressed() {
        assertFalse(UploadGate.shouldCompressBeforeUpload("uv_photo_abc.jpg", ONE_MB));
    }

    @Test public void c04_r06_oversizedJpegIsCompressed() {
        assertTrue(UploadGate.shouldCompressBeforeUpload("gallery.jpg", FIVE_MB));
    }

    @Test public void c04_r06_pdfIsNeverCompressed() {
        assertFalse(UploadGate.shouldCompressBeforeUpload("report.pdf", FIVE_MB));
    }

    @Test public void c04_r08_stillOversizedAfterCompressionIsRejected() {
        assertTrue(UploadGate.rejectAfterCompression(FIVE_MB));
        assertFalse(UploadGate.rejectAfterCompression(UploadGate.MAX_BYTES));
    }

    /** F-003 — recorded as currently-wrong behaviour, so a fix is a deliberate change. */
    @Test public void f003_webpAndHeicAreNotCompressibleToday() {
        assertFalse(UploadGate.isCompressible("photo.webp"));
        assertFalse(UploadGate.isCompressible("photo.heic"));
        assertTrue(UploadGate.rejectAtPick("photo.heic", FIVE_MB));
    }

    @Test public void c04_r13_magicBytesDetectEachType() {
        assertEquals(".pdf", UploadGate.detectExtension(new byte[]{0x25,0x50,0x44,0x46}));
        assertEquals(".png", UploadGate.detectExtension(new byte[]{(byte)0x89,0x50,0x4E,0x47}));
        assertEquals(".jpg", UploadGate.detectExtension(new byte[]{(byte)0xFF,(byte)0xD8,0,0}));
        assertEquals(".jpg", UploadGate.detectExtension(new byte[]{1,2,3,4}));
        assertEquals(".jpg", UploadGate.detectExtension(new byte[]{1,2}));
        assertEquals(".jpg", UploadGate.detectExtension(null));
    }
}
