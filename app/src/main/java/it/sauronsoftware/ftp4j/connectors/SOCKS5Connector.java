package it.sauronsoftware.ftp4j.connectors;

import it.sauronsoftware.ftp4j.FTPConnector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SOCKS5Connector extends FTPConnector {
    private String socks5host;
    private int socks5port;
    private String socks5user;
    private String socks5pass;

    public SOCKS5Connector(String socks5host, int socks5port, String socks5user, String socks5pass) {
        this.socks5host = socks5host;
        this.socks5port = socks5port;
        this.socks5user = socks5user;
        this.socks5pass = socks5pass;
    }

    public SOCKS5Connector(String socks5host, int socks5port) {
        this(socks5host, socks5port, (String)null, (String)null);
    }

    private Socket socksConnect(String host, int port, boolean forDataTransfer) throws IOException {
        boolean authentication = this.socks5user != null && this.socks5pass != null;
        boolean connected = false;
        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            if (forDataTransfer) {
                socket = this.tcpConnectForDataTransferChannel(this.socks5host, this.socks5port);
            } else {
                socket = this.tcpConnectForCommunicationChannel(this.socks5host, this.socks5port);
            }

            in = socket.getInputStream();
            out = socket.getOutputStream();
            out.write(5);
            if (authentication) {
                out.write(1);
                out.write(2);
            } else {
                out.write(1);
                out.write(0);
            }

            int aux = this.read(in);
            if (aux != 5) {
                throw new IOException("SOCKS5Connector: invalid proxy response");
            } else {
                aux = this.read(in);
                byte[] user;
                if (authentication) {
                    if (aux != 2) {
                        throw new IOException("SOCKS5Connector: proxy doesn't support username/password authentication method");
                    }

                    user = this.socks5user.getBytes("UTF-8");
                    byte[] pass = this.socks5pass.getBytes("UTF-8");
                    int userLength = user.length;
                    int passLength = pass.length;
                    if (userLength > 255) {
                        throw new IOException("SOCKS5Connector: username too long");
                    }

                    if (passLength > 255) {
                        throw new IOException("SOCKS5Connector: password too long");
                    }

                    out.write(1);
                    out.write(userLength);
                    out.write(user);
                    out.write(passLength);
                    out.write(pass);
                    aux = this.read(in);
                    if (aux != 1) {
                        throw new IOException("SOCKS5Connector: invalid proxy response");
                    }

                    aux = this.read(in);
                    if (aux != 0) {
                        throw new IOException("SOCKS5Connector: authentication failed");
                    }
                } else if (aux != 0) {
                    throw new IOException("SOCKS5Connector: proxy requires authentication");
                }

                out.write(5);
                out.write(1);
                out.write(0);
                out.write(3);
                user = host.getBytes("UTF-8");
                if (user.length > 255) {
                    throw new IOException("SOCKS5Connector: domain name too long");
                } else {
                    out.write(user.length);
                    out.write(user);
                    out.write(port >> 8);
                    out.write(port);
                    aux = this.read(in);
                    if (aux != 5) {
                        throw new IOException("SOCKS5Connector: invalid proxy response");
                    } else {
                        aux = this.read(in);
                        switch(aux) {
                        case 0:
                            in.skip(1L);
                            aux = this.read(in);
                            if (aux == 1) {
                                in.skip(4L);
                            } else if (aux == 3) {
                                aux = this.read(in);
                                in.skip((long)aux);
                            } else {
                                if (aux != 4) {
                                    throw new IOException("SOCKS5Connector: invalid proxy response");
                                }

                                in.skip(16L);
                            }

                            in.skip(2L);
                            connected = true;
                            return socket;
                        case 1:
                            throw new IOException("SOCKS5Connector: general failure");
                        case 2:
                            throw new IOException("SOCKS5Connector: connection not allowed by ruleset");
                        case 3:
                            throw new IOException("SOCKS5Connector: network unreachable");
                        case 4:
                            throw new IOException("SOCKS5Connector: host unreachable");
                        case 5:
                            throw new IOException("SOCKS5Connector: connection refused by destination host");
                        case 6:
                            throw new IOException("SOCKS5Connector: TTL expired");
                        case 7:
                            throw new IOException("SOCKS5Connector: command not supported / protocol error");
                        case 8:
                            throw new IOException("SOCKS5Connector: address type not supported");
                        default:
                            throw new IOException("SOCKS5Connector: invalid proxy response");
                        }
                    }
                }
            }
        } catch (IOException var27) {
            throw var27;
        } finally {
            if (!connected) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var26) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable var25) {
                    }
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable var24) {
                    }
                }
            }

        }
    }

    private int read(InputStream in) throws IOException {
        int aux = in.read();
        if (aux < 0) {
            throw new IOException("SOCKS5Connector: connection closed by the proxy");
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
