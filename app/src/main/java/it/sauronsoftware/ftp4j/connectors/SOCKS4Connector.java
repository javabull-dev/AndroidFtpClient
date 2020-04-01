package it.sauronsoftware.ftp4j.connectors;

import it.sauronsoftware.ftp4j.FTPConnector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SOCKS4Connector extends FTPConnector {
    private String socks4host;
    private int socks4port;
    private String socks4user;

    public SOCKS4Connector(String socks4host, int socks4port, String socks4user) {
        this.socks4host = socks4host;
        this.socks4port = socks4port;
        this.socks4user = socks4user;
    }

    public SOCKS4Connector(String socks4host, int socks4port) {
        this(socks4host, socks4port, (String)null);
    }

    private Socket socksConnect(String host, int port, boolean forDataTransfer) throws IOException {
        boolean socks4a = false;

        byte[] address;
        try {
            address = InetAddress.getByName(host).getAddress();
        } catch (Exception var25) {
            socks4a = true;
            address = new byte[]{0, 0, 0, 1};
        }

        boolean connected = false;
        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            if (forDataTransfer) {
                socket = this.tcpConnectForDataTransferChannel(this.socks4host, this.socks4port);
            } else {
                socket = this.tcpConnectForCommunicationChannel(this.socks4host, this.socks4port);
            }

            in = socket.getInputStream();
            out = socket.getOutputStream();
            out.write(4);
            out.write(1);
            out.write(port >> 8);
            out.write(port);
            out.write(address);
            if (this.socks4user != null) {
                out.write(this.socks4user.getBytes("UTF-8"));
            }

            out.write(0);
            if (socks4a) {
                out.write(host.getBytes("UTF-8"));
                out.write(0);
            }

            int aux = this.read(in);
            if (aux != 0) {
                throw new IOException("SOCKS4Connector: invalid proxy response");
            } else {
                aux = this.read(in);
                switch(aux) {
                case 90:
                    in.skip(6L);
                    connected = true;
                    return socket;
                case 91:
                    throw new IOException("SOCKS4Connector: connection refused/failed");
                case 92:
                    throw new IOException("SOCKS4Connector: cannot validate the user");
                case 93:
                    throw new IOException("SOCKS4Connector: invalid user");
                default:
                    throw new IOException("SOCKS4Connector: invalid proxy response");
                }
            }
        } catch (IOException var26) {
            throw var26;
        } finally {
            if (!connected) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var24) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable var23) {
                    }
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable var22) {
                    }
                }
            }

        }
    }

    private int read(InputStream in) throws IOException {
        int aux = in.read();
        if (aux < 0) {
            throw new IOException("SOCKS4Connector: connection closed by the proxy");
        } else {
            return aux;
        }
    }

    public Socket connectForCommunicationChannel(String host, int port) throws IOException {
        return this.socksConnect(host, port, false);
    }

    public Socket connectForDataTransferChannel(String host, int port) throws IOException {
        return this.socksConnect(host, port, true);
    }
}
