package in.gov.pmegp.pv;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import in.gov.pmegp.pv.core.upload.UploadGate;
import in.gov.pmegp.pv.ui.unitverification.validation.EmploymentValidator;
import in.gov.pmegp.pv.ui.unitverification.validation.EmploymentValidator.Input;
import in.gov.pmegp.pv.ui.unitverification.validation.ProductSalesValidator;

/**
 * Differential test: 2,000 randomised inputs run through the .NET MAUI expressions,
 * transcribed verbatim, with their outputs captured in
 * {@code src/test/resources/maui-parity-vectors.tsv}. This asserts the Java port
 * produces identical results for every one.
 *
 * <p>This is parity layer 2 from the spec README — the validator equivalent of the
 * contract-replay suite. If a refactor changes behaviour, this fails and names the row.
 */
public class ParityTest {

    /** SafeParseInt — int.TryParse semantics: anything unparseable is 0. */
    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static BigDecimal safeDecimal(String s) {
        if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    @Test public void mauiParity_allVectors() throws Exception {
        int rows = 0, mismatches = 0;
        StringBuilder firstFew = new StringBuilder();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("maui-parity-vectors.tsv"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] c = line.split("\t", -1);
                rows++;

                Input in = new Input();
                in.male = safeInt(c[0]);  in.female = safeInt(c[1]);  in.transgender = safeInt(c[2]);
                in.general = safeInt(c[3]); in.sc = safeInt(c[4]); in.st = safeInt(c[5]);
                in.obc = safeInt(c[6]); in.minority = safeInt(c[7]);
                in.fullTime = safeInt(c[8]); in.partTime = safeInt(c[9]); in.seasonal = safeInt(c[10]);
                in.wages = safeDecimal(c[11]);

                // UV07-R01 / R02
                String total = EmploymentValidator.totalDisplay(in);
                // UV07-R06..R10
                var live  = EmploymentValidator.validateLive(in);
                var leave = EmploymentValidator.validateForLeaving(in);

                boolean genderErr = live.groupErrors().containsKey("gender");
                boolean catErr    = live.groupErrors().containsKey("socialcategory");
                boolean typeErr   = live.groupErrors().containsKey("employmenttype");
                boolean wagesErr  = live.fieldErrors().containsKey("averagewagespaidperemployeepermonth");

                // UV06-R01
                boolean amtOk = ProductSalesValidator.atLeastMinimum(
                        ProductSalesValidator.parse(c[19]));

                // C04-R03 / R06
                String name = "file" + c[21];
                long size = Long.parseLong(c[22]);
                boolean rejectPick = UploadGate.rejectAtPick(name, size);
                boolean compress   = UploadGate.shouldCompressBeforeUpload(name, size);

                StringBuilder bad = new StringBuilder();
                if (!total.equals(c[12]))                       bad.append(" total");
                if (genderErr   != c[13].equals("1"))           bad.append(" genderErr");
                if (catErr      != c[14].equals("1"))           bad.append(" catErr");
                if (typeErr     != c[15].equals("1"))           bad.append(" typeErr");
                if (wagesErr    != c[16].equals("1"))           bad.append(" wagesErr");
                if (live.isValid()  != c[17].equals("1"))       bad.append(" liveValid");
                if (leave.isValid() != c[18].equals("1"))       bad.append(" leaveValid");
                if (amtOk       != c[20].equals("1"))           bad.append(" amtOk");
                if (rejectPick  != c[23].equals("1"))           bad.append(" rejectPick");
                if (compress    != c[24].equals("1"))           bad.append(" compress");

                if (bad.length() > 0) {
                    mismatches++;
                    if (mismatches <= 5) {
                        firstFew.append("\n  row ").append(rows)
                                .append(" differs:").append(bad)
                                .append("\n    ").append(line);
                    }
                }
            }
        }
        assertEquals("parity vectors loaded", 2000, rows);
        assertEquals("Java/.NET behavioural mismatches" + firstFew, 0, mismatches);
    }
}
