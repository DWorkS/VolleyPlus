package com.android.volley.misc;

import com.android.volley.Response.ProgressListener;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper for an {@link HttpEntity} that can count the number of bytes
 * transferred.  This is used internally to give updates for uploads.
 */
public class ProgressHttpEntity extends HttpEntityWrapper {

    private final ProgressListener listener;
    private final long length;
    private long contentLength;

    public ProgressHttpEntity(final HttpEntity wrapped,
                              final ProgressListener listener) {
        super(wrapped);
        this.listener = listener;
        length = wrapped.getContentLength();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        wrappedEntity.writeTo(new CountingOutputStream(out, length, listener));
    }

    public void setContentLength(long length){
        contentLength = length;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}