package com.android.volley.misc;

import java.io.File;
import java.util.Map;

/**
 * Created by HaKr on 08/02/15.
 * Modified by FungLAM on 2017-02-23
 */
public class MultipartUtils {

    private MultipartUtils() {}

    public static final String CRLF = "\r\n";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; charset=%s; boundary=%s";
    public static final String BINARY = "binary";
    public static final String EIGHT_BIT = "8bit";
    public static final String FORM_DATA = "form-data; name=\"%s\"";
    public static final String BOUNDARY_PREFIX = "--";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String FILENAME = "filename=\"%s\"";
    public static final String COLON_SPACE = ": ";
    public static final String SEMICOLON_SPACE = "; ";

    public static final int CRLF_LENGTH = 2; //CRLF.getBytes().length;
    public static final int HEADER_CONTENT_DISPOSITION_LENGTH = HEADER_CONTENT_DISPOSITION.getBytes().length;
    public static final int COLON_SPACE_LENGTH = COLON_SPACE.getBytes().length;
    public static final int HEADER_CONTENT_TYPE_LENGTH = HEADER_CONTENT_TYPE.getBytes().length;
    public static final int CONTENT_TYPE_OCTET_STREAM_LENGTH = CONTENT_TYPE_OCTET_STREAM.getBytes().length;
    public static final int HEADER_CONTENT_TRANSFER_ENCODING_LENGTH = HEADER_CONTENT_TRANSFER_ENCODING.getBytes().length;
    public static final int BINARY_LENGTH = BINARY.getBytes().length;
    public static final int BOUNDARY_PREFIX_LENGTH = BOUNDARY_PREFIX.getBytes().length;

    public static final byte[] CRLF_BYTES = {0x0D, 0x0A};

    public static int getContentLengthForMultipartRequest(String boundary, Map<String, MultiPartParam> multipartParams, Map<String, String> filesToUpload) {
        final int boundaryLength = boundary.getBytes().length;
        int contentLength = 0;
        for (Map.Entry<String, MultiPartParam> multipartParam : multipartParams.entrySet()) {
            MultiPartParam param = multipartParam.getValue();
            int size = boundaryLength +
                    CRLF_LENGTH + HEADER_CONTENT_DISPOSITION_LENGTH + COLON_SPACE_LENGTH + String.format(FORM_DATA, multipartParam.getKey()).getBytes().length +
                    CRLF_LENGTH + HEADER_CONTENT_TYPE_LENGTH + COLON_SPACE_LENGTH + param.contentType.getBytes().length +
                    CRLF_LENGTH + CRLF_LENGTH + param.value.getBytes().length + CRLF_LENGTH;

            contentLength += size;
        }

        for (Map.Entry<String, String> filetoUpload : filesToUpload.entrySet()) {
            File file = new File(filetoUpload.getValue());
            int size = boundaryLength +
                    CRLF_LENGTH + HEADER_CONTENT_DISPOSITION_LENGTH + COLON_SPACE_LENGTH + String.format(FORM_DATA + SEMICOLON_SPACE + FILENAME, filetoUpload.getKey(), file.getName()).getBytes().length +
                    CRLF_LENGTH + HEADER_CONTENT_TYPE_LENGTH + COLON_SPACE_LENGTH + CONTENT_TYPE_OCTET_STREAM_LENGTH +
                    CRLF_LENGTH + HEADER_CONTENT_TRANSFER_ENCODING_LENGTH + COLON_SPACE_LENGTH + BINARY_LENGTH + CRLF_LENGTH + CRLF_LENGTH;

            size += (int) file.length();
            size += CRLF_LENGTH;
            contentLength += size;
        }

        int size = boundaryLength + BOUNDARY_PREFIX_LENGTH + CRLF_LENGTH;
        contentLength += size;
        return contentLength;
    }

}
