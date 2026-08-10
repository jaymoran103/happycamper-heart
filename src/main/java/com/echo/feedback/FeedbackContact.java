package com.echo.feedback;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/**
 * Supplies the maintainer's WhatsApp number, injected at build time.
 *
 * The number is deliberately absent from source: this repository is public. CI passes it via
 * -Dhappycamper.whatsapp from a secret, Maven resource filtering writes it into
 * contact.properties, and local builds simply get an empty value.
 *
 * Mirrors HappyCamper.readVersion() in shape - a filtered resource read with a quiet fallback.
 */
public final class FeedbackContact {

    private static final String RESOURCE = "/contact.properties";

    private FeedbackContact() {}

    /** The configured number, or an empty string when this build has none. */
    public static String whatsappNumber() {
        try (InputStream in = FeedbackContact.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return "";
            }
            Properties props = new Properties();
            props.load(in);
            String number = props.getProperty("whatsapp", "");
            return number == null ? "" : number.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Builds a wa.me link carrying a short prefilled message.
     *
     * The message is deliberately short - the full text goes in the attached file, and URL length
     * limits vary by browser. This link does not carry the payload.
     *
     * @return the URI, or empty when no usable number is configured
     */
    public static Optional<URI> whatsappUri(String number, String message) {
        if (number == null || number.isBlank() || number.startsWith("${")) {
            return Optional.empty();
        }
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        // URLEncoder emits '+' for spaces, correct for form bodies but not for a query value
        // WhatsApp shows the user literally.
        String encoded = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8)
                                   .replace("+", "%20");
        return Optional.of(URI.create("https://wa.me/" + digits + "?text=" + encoded));
    }
}
