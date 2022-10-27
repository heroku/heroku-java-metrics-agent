package com.heroku.agent.metrics;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class UtilsTest {
    @Test
    public void testReadAllUtf8() throws IOException {
        String string = "Hello World!\nThis is another line!\nAnd this is an emoji: \uD83E\uDD8A";
        InputStream inputStream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));

        assertThat(Utils.readAllUtf8(inputStream), is(equalTo(string)));
    }
}
