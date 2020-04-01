package it.sauronsoftware.ftp4j.connectors;

import it.sauronsoftware.ftp4j.FTPCommunicationChannel;
import it.sauronsoftware.ftp4j.FTPConnector;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPReply;
import java.io.IOException;
import java.net.Socket;

public class FTPProxyConnector extends FTPConnector {
    public static int STYLE_SITE_COMMAND = 0;
    public static int STYLE_OPEN_COMMAND = 1;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPass;
    public int style;

    public FTPProxyConnector(String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        super(true);
        this.style = STYLE_SITE_COMMAND;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPass = proxyPass;
    }

    public FTPProxyConnector(String proxyHost, int proxyPort) {
        this(proxyHost, proxyPort, "anonymous", "it/sauronsoftware/ftp4j");
    }

    public void setStyle(int style) {
        if (style != STYLE_OPEN_COMMAND && style != STYLE_SITE_COMMAND) {
            throw new IllegalArgumentException("Invalid style");
        } else {
            this.style = style;
        }
    }

    public Socket connectForCommunicationChannel(String host, int port) throws IOException {
        Socket socket = this.tcpConnectForCommunicationChannel(this.proxyHost, this.proxyPort);
        FTPCommunicationChannel communication = new FTPCommunicationChannel(socket, "ASCII");

        FTPReply r;
        try {
            r = communication.readFTPReply();
        } catch (FTPIllegalReplyException var10) {
            throw new IOException("Invalid proxy response");
        }

        if (r.getCode() != 220) {
            throw new IOException("Invalid proxy response");
        } else {
            if (this.style == STYLE_SITE_COMMAND) {
                communication.sendFTPCommand("USER " + this.proxyUser);

                try {
                    r = communication.readFTPReply();
                } catch (FTPIllegalReplyException var9) {
                    throw new IOException("Invalid proxy response");
                }

                boolean passwordRequired;
                switch(r.getCode()) {
                case 230:
                    passwordRequired = false;
                    break;
                case 331:
                    passwordRequired = true;
                    break;
                default:
                    throw new IOException("Proxy authentication failed");
                }

                if (passwordRequired) {
                    communication.sendFTPCommand("PASS " + this.proxyPass);

                    try {
                        r = communication.readFTPReply();
                    } catch (FTPIllegalReplyException var8) {
                        throw new IOException("Invalid proxy response");
                    }

                    if (r.getCode() != 230) {
                        throw new IOException("Proxy authentication failed");
                    }
                }

                communication.sendFTPCommand("SITE " + host + ":" + port);
            } else if (this.style == STYLE_OPEN_COMMAND) {
                communication.sendFTPCommand("OPEN " + host + ":" + port);
            }

            return socket;
        }
    }

    public Socket connectForDataTransferChannel(String host, int port) throws IOException {
        return this.tcpConnectForDataTransferChannel(host, port);
    }
}
