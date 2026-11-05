package in.gov.pmegp.pv;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import in.gov.pmegp.pv.core.util.FormatHelpers;

/**
 * UV04-R03 — differential test against the real .NET FormatHelpers.ConvertNumberToWords,
 * compiled and run to produce src/test/resources/maui-words-vectors.tsv.
 */
public class WordsParityTest {

    @Test public void mauiWordsParity() throws Exception {
        int rows = 0, bad = 0;
        StringBuilder first = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("maui-words-vectors.tsv"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] c = line.split("\t", 2);
                rows++;
                String got = FormatHelpers.convertNumberToWords(new BigDecimal(c[0]));
                if (!got.equals(c[1])) {
                    bad++;
                    if (bad <= 5) first.append("\n  ").append(c[0])
                            .append("\n    .NET: ").append(c[1])
                            .append("\n    Java: ").append(got);
                }
            }
        }
        assertTrue("vectors loaded", rows > 500);
        assertEquals("number-to-words mismatches vs .NET" + first, 0, bad);
    }
}
