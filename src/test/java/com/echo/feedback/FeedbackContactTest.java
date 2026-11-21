package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class FeedbackContactTest {

    @Test void buildsAWaMeUriWithAnEncodedMessage() {
        Optional<URI> uri = FeedbackContact.whatsappUri("15551234567", "HappyCamper 2.5.0 feedback");
        assertTrue(uri.isPresent());
        assertEquals("wa.me", uri.get().getHost());
        assertTrue(uri.get().toString().contains("15551234567"), uri.get().toString());
        assertTrue(uri.get().toString().contains("HappyCamper%202.5.0"), uri.get().toString());
    }

    @Test void noUriWhenNumberIsBlank() {
        assertTrue(FeedbackContact.whatsappUri("", "msg").isEmpty());
        assertTrue(FeedbackContact.whatsappUri("   ", "msg").isEmpty());
        assertTrue(FeedbackContact.whatsappUri(null, "msg").isEmpty());
    }

    @Test void noUriWhenFilteringDidNotRun() {
        // An unfiltered resource leaves the literal Maven placeholder behind.
        assertTrue(FeedbackContact.whatsappUri("${happycamper.whatsapp}", "msg").isEmpty());
    }

    @Test void stripsFormattingFromTheNumber() {
        Optional<URI> uri = FeedbackContact.whatsappUri("+1 (555) 123-4567", "msg");
        assertTrue(uri.isPresent());
        assertTrue(uri.get().toString().contains("15551234567"), uri.get().toString());
    }

    @Test void numberIsBlankInLocalBuilds() {
        // The pom leaves the property empty; only CI injects a real number.
        assertEquals("", FeedbackContact.whatsappNumber());
    }
}
