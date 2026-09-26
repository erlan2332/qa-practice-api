package kg.qalab;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticUiContractTest {

    private static final Pattern REQUIRED_ELEMENT =
        Pattern.compile("\\$\\('([^']+)'\\)");

    @Test
    void everyRequiredJavaScriptElementExistsInHtml() throws IOException {
        String html = resource("static/index.html");
        String javascript = resource("static/app.js");
        Set<String> requiredIds = new LinkedHashSet<>();
        Matcher matcher = REQUIRED_ELEMENT.matcher(javascript);

        while (matcher.find()) {
            requiredIds.add(matcher.group(1));
        }

        for (String id : requiredIds) {
            assertTrue(
                html.contains("id=\"" + id + "\""),
                () -> "app.js requires missing index.html element #" + id
            );
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass()
            .getClassLoader()
            .getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
