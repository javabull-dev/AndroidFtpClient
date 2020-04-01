package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import javax.net.ssl.SSLSocketFactory;

public class FTPCommunicationChannel {
    private ArrayList communicationListeners = new ArrayList();
    private Socket connection = null;
    private String charsetName = null;
    private NVTASCIIReader reader = null;
    private NVTASCIIWriter writer = null;

    public FTPCommunicationChannel(Socket connection, String charsetName) throws IOException {
        this.connection = connection;
        this.charsetName = charsetName;
        InputStream inStream = connection.getInputStream();
        OutputStream outStream = connection.getOutputStream();
        this.reader = new NVTASCIIReader(inStream, charsetName);
        this.writer = new NVTASCIIWriter(outStream, charsetName);
    }

    public FTPCommunicationChannel() {

    }

    public void addCommunicationListener(FTPCommunicationListener listener) {
        this.communicationListeners.add(listener);
    }

    public void removeCommunicationListener(FTPCommunicationListener listener) {
        this.communicationListeners.remove(listener);
    }

    public void close() {
        try {
            this.connection.close();
        } catch (Exception var2) {
        }

    }

    public FTPCommunicationListener[] getCommunicationListeners() {
        int size = this.communicationListeners.size();
        FTPCommunicationListener[] ret = new FTPCommunicationListener[size];

        for(int i = 0; i < size; ++i) {
            ret[i] = (FTPCommunicationListener)this.communicationListeners.get(i);
        }

        return ret;
    }

    private String read() throws IOException {
        String line = this.reader.readLine();
        if (line == null) {
            throw new IOException("FTPConnection closed");
        } else {
            Iterator iter = this.communicationListeners.iterator();

            while(iter.hasNext()) {
                FTPCommunicationListener l = (FTPCommunicationListener)iter.next();
                l.received(line);
            }

            return line;
        }
    }

    public void sendFTPCommand(String command) throws IOException {
        this.writer.writeLine(command);
        Iterator iter = this.communicationListeners.iterator();

        while(iter.hasNext()) {
            FTPCommunicationListener l = (FTPCommunicationListener)iter.next();
            l.sent(command);
        }

    }

    public FTPReply readFTPReply() throws IOException, FTPIllegalReplyException {
        int code = 0;
        ArrayList messages = new ArrayList();

        int i;
        label84:
        while(true) {
            while(true) {
                String statement;
                do {
                    statement = this.read();
                } while(statement.trim().length() == 0);

                if (statement.startsWith("\n")) {
                    statement = statement.substring(1);
                }

                int l = statement.length();
                if (code == 0 && l < 3) {
                    throw new FTPIllegalReplyException();
                }

                try {
                    i = Integer.parseInt(statement.substring(0, 3));
                } catch (Exception var8) {
                    if (code == 0) {
                        throw new FTPIllegalReplyException();
                    }

                    i = 0;
                }

                if (code != 0 && i != 0 && i != code) {
                    throw new FTPIllegalReplyException();
                }

                if (code == 0) {
                    code = i;
                }

                if (i > 0) {
                    if (l > 3) {
                        char s = statement.charAt(3);
                        String message = statement.substring(4, l);
                        messages.add(message);
                        if (s == ' ') {
                            break label84;
                        }

                        if (s != '-') {
                            throw new FTPIllegalReplyException();
                        }
                    } else {
                        if (l == 3) {
                            break label84;
                        }

                        messages.add(statement);
                    }
                } else {
                    messages.add(statement);
                }
            }
        }

        int size = messages.size();
        String[] m = new String[size];

        for(i = 0; i < size; ++i) {
            m[i] = (String)messages.get(i);
        }

        return new FTPReply(code, m);
    }

    public void changeCharset(String charsetName) throws IOException {
        this.charsetName = charsetName;
        this.reader.changeCharset(charsetName);
        this.writer.changeCharset(charsetName);
    }

    public void ssl(SSLSocketFactory sslSocketFactory) throws IOException {
        String host = this.connection.getInetAddress().getHostName();
        int port = this.connection.getPort();
        this.connection = sslSocketFactory.createSocket(this.connection, host, port, true);
        InputStream inStream = this.connection.getInputStream();
        OutputStream outStream = this.connection.getOutputStream();
        this.reader = new NVTASCIIReader(inStream, this.charsetName);
        this.writer = new NVTASCIIWriter(outStream, this.charsetName);
    }
}
