package com.android.volley.misc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {
    private int bytesRead = 0;

    public CountingInputStream(InputStream in) {
        super(in);
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

    public long getBytesRead() {
        return bytesRead;
    }
}