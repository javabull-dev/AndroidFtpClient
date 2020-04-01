
package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class NVTASCIIReader extends Reader {
    private static final String SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator");
    private InputStream stream;
    private Reader reader;

    public NVTASCIIReader(InputStream stream, String charsetName) throws IOException {
        this.stream = stream;
        this.reader = new InputStreamReader(stream, charsetName);
    }

    public void close() throws IOException {
        synchronized(this) {
            this.reader.close();
        }
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized(this) {
            return this.reader.read(cbuf, off, len);
        }
    }

    public void changeCharset(String charsetName) throws IOException {
        synchronized(this) {
            this.reader = new InputStreamReader(this.stream, charsetName);
        }
    }

    public String readLine() throws IOException {
        StringBuffer buffer = new StringBuffer();
        int current = -1;

        while(true) {
            int i = this.reader.read();
            if (i == -1) {
                return buffer.length() == 0 ? null : buffer.toString();
            }

            int previous = current;
            current = i;
            if (i == 10) {
                return buffer.toString();
            }

            if (previous == 13 && i == 0) {
                buffer.append(SYSTEM_LINE_SEPARATOR);
            } else if (i != 0 && i != 13) {
                buffer.append((char)i);
            }
        }
    }
}
