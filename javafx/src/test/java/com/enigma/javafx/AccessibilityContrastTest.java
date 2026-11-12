package com.enigma.javafx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessibilityContrastTest {
    private static final String STYLES = readResource("/com/enigma/javafx/styles.css");
    private static final String FXML = readResource("/com/enigma/javafx/main-view.fxml");
    private static final Pattern RULE = Pattern.compile("(?s)([^{}]+)\\{([^{}]*)}");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    @Test
    void normalTextAndInteractiveLabelsMeetWcagAaContrast() {
        assertContrast(".section-title", "-fx-text-fill", ".card", "-fx-background-color", 4.5);
        assertContrast(".section-description", "-fx-text-fill", ".card", "-fx-background-color", 4.5);
        assertContrast(".helper-text", "-fx-text-fill", ".card", "-fx-background-color", 4.5);
        assertContrast(".menu-option-description", "-fx-text-fill",
                ".menu-option", "-fx-background-color", 4.5);
        assertContrast(".menu-encryption-title", "-fx-text-fill",
                ".menu-option", "-fx-background-color", 4.5);
        assertContrast(".menu-decryption-title", "-fx-text-fill",
                ".menu-option", "-fx-background-color", 4.5);
        assertContrast("#encryptionView .primary-button", "-fx-text-fill",
                ".root", "-fx-background-color", 4.5);
        assertContrast("#decryptionView .primary-button", "-fx-text-fill",
                ".root", "-fx-background-color", 4.5);
        assertContrast(".text-field", "-fx-text-fill",
                ".text-field", "-fx-background-color", 4.5);
        assertContrast(".text-field", "-fx-prompt-text-fill",
                ".text-field", "-fx-background-color", 4.5);
        assertContrast(".primary-button", "-fx-text-fill", ".root", "-fx-background-color", 4.5);
        assertContrast(".status-text", "-fx-text-fill",
                ".status-bar", "-fx-background-color", 4.5);
        assertContrast(".app-alert-error .header-panel .label", "-fx-text-fill",
                ".app-alert-error .header-panel", "-fx-background-color", 4.5);
        assertContrast(".check-box:selected .mark", "-fx-background-color",
                ".check-box:selected .box", "-fx-background-color", 4.5);
    }

    @Test
    void usesTheContractPaletteAndNotoSansWithSafeFallbacks() {
        assertTrue(declaration(".menu-encryption-title", "-fx-text-fill").equalsIgnoreCase("#0003C9"));
        assertTrue(declaration(".menu-decryption-title", "-fx-text-fill").equalsIgnoreCase("#C90068"));
        assertTrue(declaration("#encryptionView .primary-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-encryption-title", "-fx-text-fill")));
        assertTrue(declaration("#encryptionView .secondary-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-encryption-title", "-fx-text-fill")));
        assertTrue(declaration("#encryptionView .back-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-encryption-title", "-fx-text-fill")));
        assertTrue(declaration("#decryptionView .primary-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-decryption-title", "-fx-text-fill")));
        assertTrue(declaration("#decryptionView .secondary-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-decryption-title", "-fx-text-fill")));
        assertTrue(declaration("#decryptionView .back-button", "-fx-text-fill")
                .equalsIgnoreCase(declaration(".menu-decryption-title", "-fx-text-fill")));
        assertTrue(declaration(".status-dot", "-fx-background-color").equalsIgnoreCase("#00C962"));
        assertTrue(declaration(".check-box:selected .box", "-fx-background-color").equalsIgnoreCase("#C9C600"));

        String fontFamily = declaration(".root", "-fx-font-family").toLowerCase();
        assertTrue(fontFamily.contains("noto sans"));
        assertTrue(fontFamily.contains("arial"));
        assertTrue(fontFamily.contains("dejavu sans"));
    }

    @Test
    void keepsFocusIndicatorsAndAccessibleLabelsInTheFxml() {
        assertContrast(".primary-button:focused", "-fx-border-color",
                ".root", "-fx-background-color", 3.0);
        assertContrast(".text-field:focused", "-fx-border-color",
                ".text-field:focused", "-fx-background-color", 3.0);
        assertTrue(FXML.contains("accessibleText="));
        assertTrue(countOccurrences(FXML, "accessibleText=") >= 12);
        assertTrue(countOccurrences(FXML, "labelFor=") >= 8);
        assertTrue(FXML.contains(
                "style=\"-fx-text-fill: #0003C9; -fx-font-size: 28px; -fx-font-weight: bold;\""));
        assertTrue(FXML.contains(
                "style=\"-fx-text-fill: #C90068; -fx-font-size: 28px; -fx-font-weight: bold;\""));
    }

    private static void assertContrast(
            String foregroundSelector, String foregroundProperty,
            String backgroundSelector, String backgroundProperty, double minimum) {
        String foreground = declaration(foregroundSelector, foregroundProperty);
        String background = declaration(backgroundSelector, backgroundProperty);
        double ratio = contrastRatio(foreground, background);
        assertTrue(ratio >= minimum,
                () -> foregroundSelector + " on " + backgroundSelector
                        + " contrast " + String.format("%.2f", ratio)
                        + " is below " + minimum + ":1");
    }

    private static double contrastRatio(String foreground, String background) {
        double first = luminance(foreground);
        double second = luminance(background);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(String color) {
        Matcher matcher = HEX_COLOR.matcher(color);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected a six-digit RGB color, got: " + color);
        }
        double red = linear(Integer.parseInt(color.substring(1, 3), 16) / 255.0);
        double green = linear(Integer.parseInt(color.substring(3, 5), 16) / 255.0);
        double blue = linear(Integer.parseInt(color.substring(5, 7), 16) / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linear(double channel) {
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static String declaration(String selector, String property) {
        Matcher rules = RULE.matcher(STYLES);
        while (rules.find()) {
            for (String candidate : rules.group(1).split(",")) {
                if (candidate.trim().equals(selector)) {
                    Matcher value = Pattern.compile(
                            "(?m)" + Pattern.quote(property) + "\\s*:\\s*([^;]+);")
                            .matcher(rules.group(2));
                    if (value.find()) {
                        return value.group(1).trim();
                    }
                }
            }
        }
        throw new IllegalArgumentException("Missing CSS declaration: " + selector + " " + property);
    }

    private static int countOccurrences(String content, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = content.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static String readResource(String path) {
        try (var input = AccessibilityContrastTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing application resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
