package com.android.volley.misc;

import com.android.volley.Response.ProgressListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends DataOutputStream {
    private final ProgressListener progressListener;
    private long transferred;
    private long fileLength;

    public CountingOutputStream(final OutputStream out, long length,
                                final ProgressListener listener) {
        super(out);
        fileLength = length;
        progressListener = listener;
        transferred = 0;
    }

    public void write(int b) throws IOException {
        out.write(b);
        if (progressListener != null) {
            transferred++;
            int prog = (int) (transferred * 100 / fileLength);
            progressListener.onProgress(transferred, prog);
        }
    }

}