
package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.StringTokenizer;

class NVTASCIIWriter extends Writer {
    private static final String LINE_SEPARATOR = "\r\n";
    private OutputStream stream;
    private Writer writer;

    public NVTASCIIWriter(OutputStream stream, String charsetName) throws IOException {
        this.stream = stream;
        this.writer = new OutputStreamWriter(stream, charsetName);
    }

    public void close() throws IOException {
        synchronized(this) {
            this.writer.close();
        }
    }

    public void flush() throws IOException {
        synchronized(this) {
            this.writer.flush();
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized(this) {
            this.writer.write(cbuf, off, len);
        }
    }

    public void changeCharset(String charsetName) throws IOException {
        synchronized(this) {
            this.writer = new OutputStreamWriter(this.stream, charsetName);
        }
    }

    public void writeLine(String str) throws IOException {
        StringBuffer buffer = new StringBuffer();
        boolean atLeastOne = false;
        StringTokenizer st = new StringTokenizer(str, "\r\n");
        int count = st.countTokens();

        for(int i = 0; i < count; ++i) {
            String line = st.nextToken();
            if (line.length() > 0) {
                if (atLeastOne) {
                    buffer.append('\r');
                    buffer.append('\u0000');
                }

                buffer.append(line);
                atLeastOne = true;
            }
        }

        if (buffer.length() > 0) {
            String statement = buffer.toString();
            this.writer.write(statement);
            this.writer.write("\r\n");
            this.writer.flush();
        }

    }
}
