package com.android.volley.toolbox.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.AbstractHttpEntity;

/**
 * @author <a href="mailto:vit at cleverua.com">Vitaliy Khudenko</a>
 */
public class MultipartEntity extends AbstractHttpEntity implements Cloneable {
    
    private List<Part> parts = new ArrayList<Part>();
    
    private Boundary boundary;
    
    public MultipartEntity(String boundaryStr) {
        super();
        boundary = new Boundary(boundaryStr);
        setContentType("multipart/form-data; boundary=\"" + boundary.getBoundary() + '"');  //$NON-NLS-1$
    }
    
    public MultipartEntity() {
        this(null);
    }
    
    public void addPart(Part part) {
        parts.add(part);
    }
    
    public boolean isRepeatable() {
        return true;
    }
    
    public String getBoundary() {
        return boundary.getBoundary();
    }

    public long getContentLength() {
        long result = 0;
        for (Part part : parts) {
            result += part.getContentLength(boundary);
        }
        result += boundary.getClosingBoundary().length;
        return result;
    }
    
    /**
     * Returns <code>null</code> since it's not designed to be used for server responses.
     */
    @Override
    public InputStream getContent() throws IOException {
        return null;
    }
    
    @Override
    public void writeTo(final OutputStream out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Output stream may not be null");    //$NON-NLS-1$
        }
        for (Part part : parts) {
            part.writeTo(out, boundary);
        }
        out.write(boundary.getClosingBoundary());
        out.flush();
    }

    /**
     * Tells that this entity is not streaming.
     *
     * @return <code>false</code>
     */
    public boolean isStreaming() {
        return false;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("MultipartEntity does not support cloning"); //$NON-NLS-1$ // TODO
    }
}