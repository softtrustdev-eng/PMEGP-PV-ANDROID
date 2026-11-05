package in.gov.pmegp.pv.core.security;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** C03 §6 — the 17 vectors verified against the .NET implementation. */
public class InputSanitizerTest {

    // --- blocking ---
    @Test public void c03_r01_angleBrackets() {
        assertEquals("scriptalert(1)/script", InputSanitizer.clean("<script>alert(1)</script>")); }
    @Test public void c03_r01_doubleQuote() {
        assertEquals(" onload=x", InputSanitizer.clean("\" onload=\"x")); }
    @Test public void c03_r01_backslash() {
        assertEquals("ab", InputSanitizer.clean("a\\b")); }
    @Test public void c03_r02_controlChars() {
        assertEquals("ab", InputSanitizer.clean("a\u0000\u0007b")); }
    @Test public void c03_r03_nonBmpEmoji() {
        assertEquals("Unit  OK", InputSanitizer.clean("Unit 😀 OK")); }
    @Test public void c03_r04_bmpEmojiWithVariationSelector() {
        assertEquals("Good  job", InputSanitizer.clean("Good ❤️ job")); }
    @Test public void c03_r05_dingbats() {
        assertEquals("", InputSanitizer.clean("☀✈★")); }
    @Test public void c03_r06_zeroWidthJoiner() {
        assertEquals("ab", InputSanitizer.clean("a‍b")); }
    @Test public void c03_r06_bidiOverride() {
        assertEquals("xy", InputSanitizer.clean("x‮y")); }
    @Test public void c03_r07_keycapCombiner() {
        assertEquals("12", InputSanitizer.clean("1⃣2⃣")); }
    @Test public void c03_r10_newlineDroppedSingleLine() {
        assertEquals("ab", InputSanitizer.clean("a\nb")); }
    @Test public void c03_r10_newlineKeptMultiLine() {
        assertEquals("a\nb", InputSanitizer.clean("a\nb", true)); }

    // --- must NOT over-block (C03-R08). These five are the important ones. ---
    @Test public void c03_r08_indianAddressPunctuation() {
        String s = "Shah & Sons, Plot 12/4, D'Souza (Unit-2) #3";
        assertEquals(s, InputSanitizer.clean(s)); }
    @Test public void c03_r08_rupeeSign() {
        String s = "₹1,20,000";
        assertEquals(s, InputSanitizer.clean(s)); }
    @Test public void c03_r08_devanagari() {
        String s = "श्री अक्षर";
        assertEquals(s, InputSanitizer.clean(s)); }
    @Test public void c03_r09_nullPassthrough() {
        assertEquals(null, InputSanitizer.clean(null)); }
    @Test public void c03_r09_emptyPassthrough() {
        assertEquals("", InputSanitizer.clean("")); }
}
