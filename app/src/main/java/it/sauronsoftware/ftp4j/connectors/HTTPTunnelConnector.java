package it.sauronsoftware.ftp4j.connectors;

import it.sauronsoftware.ftp4j.FTPConnector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class HTTPTunnelConnector extends FTPConnector {
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPass;

    public HTTPTunnelConnector(String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPass = proxyPass;
    }

    public HTTPTunnelConnector(String proxyHost, int proxyPort) {
        this(proxyHost, proxyPort, (String)null, (String)null);
    }

    private Socket httpConnect(String host, int port, boolean forDataTransfer) throws IOException {
        byte[] CRLF = "\r\n".getBytes("UTF-8");
        String connect = "CONNECT " + host + ":" + port + " HTTP/1.1";
        String hostHeader = "Host: " + host + ":" + port;
        boolean connected = false;
        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            if (forDataTransfer) {
                socket = this.tcpConnectForDataTransferChannel(this.proxyHost, this.proxyPort);
            } else {
                socket = this.tcpConnectForCommunicationChannel(this.proxyHost, this.proxyPort);
            }

            in = socket.getInputStream();
            out = socket.getOutputStream();
            out.write(connect.getBytes("UTF-8"));
            out.write(CRLF);
            out.write(hostHeader.getBytes("UTF-8"));
            out.write(CRLF);
            if (this.proxyUser != null && this.proxyPass != null) {
                String header = "Proxy-Authorization: Basic " + Base64.encode(this.proxyUser + ":" + this.proxyPass);
                out.write(header.getBytes("UTF-8"));
                out.write(CRLF);
            }

            out.write(CRLF);
            out.flush();
            ArrayList responseLines = new ArrayList();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            for(String line = reader.readLine(); line != null && line.length() > 0; line = reader.readLine()) {
                responseLines.add(line);
            }

            int size = responseLines.size();
            if (size < 1) {
                throw new IOException("HTTPTunnelConnector: invalid proxy response");
            }

            String code = null;
            String response = (String)responseLines.get(0);
            if (!response.startsWith("HTTP/") || response.length() < 12) {
                throw new IOException("HTTPTunnelConnector: invalid proxy response");
            }

            code = response.substring(9, 12);
            if (!"200".equals(code)) {
                StringBuffer msg = new StringBuffer();
                msg.append("HTTPTunnelConnector: connection failed\r\n");
                msg.append("Response received from the proxy:\r\n");

                for(int i = 0; i < size; ++i) {
                    String line = (String)responseLines.get(i);
                    msg.append(line);
                    msg.append("\r\n");
                }

                throw new IOException(msg.toString());
            }

            connected = true;
        } catch (IOException var32) {
            throw var32;
        } finally {
            if (!connected) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var31) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable var30) {
                    }
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable var29) {
                    }
                }
            }

        }

        return socket;
    }

    public Socket connectForCommunicationChannel(String host, int port) throws IOException {
        return this.httpConnect(host, port, false);
    }

    public Socket connectForDataTransferChannel(String host, int port) throws IOException {
        return this.httpConnect(host, port, true);
    }
}
