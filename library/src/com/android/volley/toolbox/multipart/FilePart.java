package com.android.volley.toolbox.multipart;

import static com.android.volley.misc.MultipartUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.protocol.HTTP;

import com.android.volley.Response.ProgressListener;


/**
 * @author <a href="mailto:vit at cleverua.com">Vitaliy Khudenko</a>
 */
public final class FilePart extends BasePart {

    private final File file;

    /**
     * @param name String - name of parameter (may not be <code>null</code>).
     * @param file File (may not be <code>null</code>).
     * @param filename String. If <code>null</code> is passed, 
     *        then <code>file.getName()</code> is used.
     * @param contentType String. If <code>null</code> is passed, 
     *        then default "application/octet-stream" is used.
     * 
     * @throws IllegalArgumentException if either <code>file</code> 
     *         or <code>name</code> is <code>null</code>.
     */
    public FilePart(String name, File file, String filename, String contentType) {
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");     //$NON-NLS-1$
        }
        if (name == null) {
            throw new IllegalArgumentException("Name may not be null");     //$NON-NLS-1$
        }
        
        this.file                    = file;
        final String partName        = UrlEncodingHelper.encode(name, HTTP.DEFAULT_PROTOCOL_CHARSET);
        final String partFilename    = UrlEncodingHelper.encode(
            (filename == null) ? file.getName() : filename,
            HTTP.DEFAULT_PROTOCOL_CHARSET
        );
        final String partContentType = (contentType == null) ? HTTP.DEFAULT_CONTENT_TYPE : contentType;
        
        headersProvider = new IHeadersProvider() {
            public String getContentDisposition() {
                return String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA + SEMICOLON_SPACE + FILENAME, partName, partFilename);
            }
            public String getContentType() {
                return HEADER_CONTENT_TYPE + COLON_SPACE + partContentType;
            }
            public String getContentTransferEncoding() {
                return HEADER_CONTENT_TRANSFER_ENCODING + COLON_SPACE + BINARY;
            }            
        };
    }
    
    public long getContentLength(Boundary boundary) {
        return getHeader(boundary).length + file.length() + CRLF_BYTES.length;
    }

    @Override
    public void writeTo(OutputStream out, Boundary boundary) throws IOException {
        out.write(getHeader(boundary));
        InputStream in = new FileInputStream(file);
        try {
            byte[] tmp = new byte[4096];
            int l;
            while ((l = in.read(tmp)) != -1) {
                out.write(tmp, 0, l);
            }
        } finally {
            in.close();
        }
        out.write(CRLF_BYTES);
    }
}
