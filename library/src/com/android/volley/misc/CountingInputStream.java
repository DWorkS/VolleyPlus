package com.android.volley.misc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {
    private final long length;
    private long bytesRead;

    public CountingInputStream(InputStream in, long length) {
        super(in);
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            bytesRead++;
        }
        return result;
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        int result = super.read(buffer, offset, count);
        if (result != -1) {
            bytesRead += result;
        }
        return result;
    }

    //VisibleForTesting
    long bytesRead() {
        return bytesRead;
    }

    public long bytesRemaining() {
        return length - bytesRead;
    }
}