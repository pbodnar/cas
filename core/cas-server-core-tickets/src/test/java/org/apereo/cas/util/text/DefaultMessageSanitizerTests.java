package org.apereo.cas.util.text;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ToStringBuilder;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is.
 *
 * @author Petr Bodnar
 */
@Tag("Utility")
public class DefaultMessageSanitizerTests {

    private final MessageSanitizer messageSanitizer = new DefaultMessageSanitizer(Pattern.compile("(?:(?:TGT|PGT|PGTIOU|PT|ST)-\\d+-)([\\w.-]+)"));

    // copied from org.apereo.cas.util.DefaultMessageSanitizerTests
    @Test
    public void verifyOperation() {
        var results = messageSanitizer.sanitize("ticket TGT-1-abcdefg created");
        assertTrue(results.contains("TGT-1-********"));
        results = messageSanitizer.sanitize("ticket PGT-1-abcdefg created");
        assertTrue(results.contains("PGT-1-********"));
        results = messageSanitizer.sanitize("ticket PGTIOU-1-abcdefg created");
        assertTrue(results.contains("PGTIOU-1-********"));
        results = messageSanitizer.sanitize("ticket PT-1-abcdefg created");
        assertTrue(results.contains("PT-1-********"));
        results = messageSanitizer.sanitize("ticket ST-1-abcdefg created");
        assertTrue(results.contains("ST-1-********"));

        results = messageSanitizer.sanitize("found a [password =se!ns4357$##@@**it!!_ive] here...");
        assertTrue(results.contains("[password =********"));

        results = messageSanitizer.sanitize(new ToStringBuilder(this)
                .append("password", "abcdefgs")
                .append("field", "value")
                .toString());
        assertTrue(results.contains("password = ********"));

        results = messageSanitizer.sanitize("found a [token=mgf63isnfb1s!!#ut0__|] here...");
        assertTrue(results.contains("[token=********"));

        results = messageSanitizer.sanitize("found a ,clientSecret = p@$$wordSecret...");
        assertTrue(results.contains(",clientSecret = ********..."));
    }

    @Test
    public void verifyPerformanceDynamic() {
        for (var i = 0; i < 1_000_000; i++) {
            val text = """
                    %dLorem ipsum dolor sit amet, TGT-1-abcdefg consectetuer adipiscing elit. Mauris suscipit, ligula sit amet pharetra
                    semper, nibh ante cursus purus, vel sagittis velit mauris vel metus. In rutrum. Donec iaculis gravida
                    nulla. Maecenas lorem. password=[SECRET123] Nullam lectus justo, vulputate eget mollis sed, tempor sed magna. Vivamus luctus
                    egestas leo. Morbi scelerisque luctus velit. Class aptent taciti sociosqu ad litora torquent per
                    conubia nostra, per inceptos hymenaeos. Maecenas fermentum, sem in pharetra pellentesque, velit turpis
                    volutpat ante, in pharetra metus odio a lectus. Nulla pulvinar eleifend sem. Integer lacinia. Aliquam
                    ornare wisi eu metus. Fusce consectetuer risus a nunc. Nam sed tellus id magna elementum tincidunt.
                    Cras elementum. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat
                    nulla pariatur. Etiam ligula pede, sagittis quis, interdum ultricies, scelerisque eu.""".formatted(i);
            val results = messageSanitizer.sanitize(text);
            assertTrue(results.contains("TGT-1-********"));
            assertTrue(results.contains("password=********]"));
        }
    }

    @Test
    public void verifyPerformanceStatic() {
        for (var i = 0; i < 1_000_000; i++) {
            val text = """
                    Lorem ipsum dolor sit amet, TGT-1-abcdefg consectetuer adipiscing elit. Mauris suscipit, ligula sit amet pharetra
                    semper, nibh ante cursus purus, vel sagittis velit mauris vel metus. In rutrum. Donec iaculis gravida
                    nulla. Maecenas lorem. password=[SECRET123] Nullam lectus justo, vulputate eget mollis sed, tempor sed magna. Vivamus luctus
                    egestas leo. Morbi scelerisque luctus velit. Class aptent taciti sociosqu ad litora torquent per
                    conubia nostra, per inceptos hymenaeos. Maecenas fermentum, sem in pharetra pellentesque, velit turpis
                    volutpat ante, in pharetra metus odio a lectus. Nulla pulvinar eleifend sem. Integer lacinia. Aliquam
                    ornare wisi eu metus. Fusce consectetuer risus a nunc. Nam sed tellus id magna elementum tincidunt.
                    Cras elementum. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat
                    nulla pariatur. Etiam ligula pede, sagittis quis, interdum ultricies, scelerisque eu.""";
            val results = messageSanitizer.sanitize(text);
            assertTrue(results.contains("TGT-1-********"));
            assertTrue(results.contains("password=********]"));
        }
    }
}
