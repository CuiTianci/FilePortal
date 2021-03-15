package com.dcz.fileportal.network;

import com.dcz.fileportal.utils.ContentType;
import com.dcz.fileportal.utils.Utils;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * https://stackoverflow.com/questions/25962595/tracking-progress-of-multipart-file-upload-using-okhttp
 */
public class CountingFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final File file;
    private final ProgressListener listener;
    private final String contentType;

    public CountingFileRequestBody(File file, ProgressListener listener) {
        this.file = file;
        this.contentType = ContentType.getContentTypeFromExtension(file.getName(), ContentType.IMAGE_PREFIX);
        this.listener = listener;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try (Source source = Okio.source(file)) {
            long total = 0;
            long read;
            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                this.listener.transferred(Utils.getTransferPercent(total, contentLength()));
            }
        }
    }
}
