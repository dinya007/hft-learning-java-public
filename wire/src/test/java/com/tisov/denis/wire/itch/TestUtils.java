package com.tisov.denis.wire.itch;

import java.nio.file.Path;

public class TestUtils {

    private static final String SAMPLE = "data/01302020.NASDAQ_ITCH50.5mb.itch";

    public static final Path SAMPLE_FILE = Path.of("..").toAbsolutePath().resolve(SAMPLE);

}
