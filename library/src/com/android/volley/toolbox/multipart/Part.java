package com.android.volley.toolbox.multipart;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:vit at cleverua.com">Vitaliy Khudenko</a>
 */
public interface Part {
    public long getContentLength(Boundary boundary);
    public void writeTo(final OutputStream out, Boundary boundary) throws IOException;
}
