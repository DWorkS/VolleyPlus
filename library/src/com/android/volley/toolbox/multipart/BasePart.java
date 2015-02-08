package com.android.volley.toolbox.multipart;

import static com.android.volley.misc.MultipartUtils.*;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

/**
 * Parent class for FilePart and StringPart.
 * 
 * @author <a href="mailto:vit at cleverua.com">Vitaliy Khudenko</a>
 */
/* package */ abstract class BasePart implements Part {

	
    protected interface IHeadersProvider {
        public String getContentDisposition();
        public String getContentType();
        public String getContentTransferEncoding();
    }
    
    protected IHeadersProvider headersProvider; // must be initialized in descendant constructor
    
    private byte[] header;
    
    protected byte[] getHeader(Boundary boundary) {
        if (header == null) {
            header = generateHeader(boundary); // lazy init
        }
        return header;
    }
    
    private byte[] generateHeader(Boundary boundary) {
        if (headersProvider == null) {
            throw new RuntimeException("Uninitialized headersProvider");    //$NON-NLS-1$
        }
        final ByteArrayBuffer buf = new ByteArrayBuffer(256);
        append(buf, boundary.getStartingBoundary());
        append(buf, headersProvider.getContentDisposition());
        append(buf, CRLF_BYTES);
        append(buf, headersProvider.getContentType());
        append(buf, CRLF_BYTES);
        //ContentTransferEncoding causes bug 
        //append(buf, headersProvider.getContentTransferEncoding());
        //append(buf, CRLF_BYTES);
        append(buf, CRLF_BYTES);
        return buf.toByteArray();
    }
    
    private static void append(ByteArrayBuffer buf, String data) {
        append(buf, EncodingUtils.getAsciiBytes(data));
    }
    
    private static void append(ByteArrayBuffer buf, byte[] data) {
        buf.append(data, 0, data.length);
    }
}
