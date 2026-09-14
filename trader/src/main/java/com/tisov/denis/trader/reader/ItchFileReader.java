package com.tisov.denis.trader.reader;

import com.tisov.denis.trader.walker.ItchMdWalker;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ItchFileReader {

    public long read(Path file, ItchMdWalker itchMdWalker) throws IOException {
        try (Arena arena = Arena.ofConfined();
             FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.READ)) {
            MemorySegment memorySegment = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size(), arena);
            return itchMdWalker.walk(memorySegment);
        }
    }

}
