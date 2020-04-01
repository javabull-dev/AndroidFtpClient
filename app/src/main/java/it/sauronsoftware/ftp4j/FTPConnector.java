package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class FTPConnector {
    protected int connectionTimeout;
    protected int readTimeout;
    protected int closeTimeout;
    private boolean useSuggestedAddressForDataConnections;
    private Socket connectingCommunicationChannelSocket;

    protected FTPConnector(boolean useSuggestedAddressForDataConnectionsDefValue) {
        this.connectionTimeout = 10;
        this.readTimeout = 10;
        this.closeTimeout = 10;
        String sysprop = System.getProperty("ftp4j.passiveDataTransfer.useSuggestedAddress");
        if (!"true".equalsIgnoreCase(sysprop) && !"yes".equalsIgnoreCase(sysprop) && !"1".equals(sysprop)) {
            if (!"false".equalsIgnoreCase(sysprop) && !"no".equalsIgnoreCase(sysprop) && !"0".equals(sysprop)) {
                this.useSuggestedAddressForDataConnections = useSuggestedAddressForDataConnectionsDefValue;
            } else {
                this.useSuggestedAddressForDataConnections = false;
            }
        } else {
            this.useSuggestedAddressForDataConnections = true;
        }

    }

    protected FTPConnector() {
        this(false);
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
    }

    public void setUseSuggestedAddressForDataConnections(boolean value) {
        this.useSuggestedAddressForDataConnections = value;
    }

    boolean getUseSuggestedAddressForDataConnections() {
        return this.useSuggestedAddressForDataConnections;
    }

    protected Socket tcpConnectForCommunicationChannel(String host, int port) throws IOException {
        Socket var3;
        try {
            this.connectingCommunicationChannelSocket = new Socket();
            this.connectingCommunicationChannelSocket.setKeepAlive(true);
            this.connectingCommunicationChannelSocket.setSoTimeout(this.readTimeout * 1000);
            this.connectingCommunicationChannelSocket.setSoLinger(true, this.closeTimeout);
            this.connectingCommunicationChannelSocket.connect(new InetSocketAddress(host, port), this.connectionTimeout * 1000);
            var3 = this.connectingCommunicationChannelSocket;
        } finally {
            this.connectingCommunicationChannelSocket = null;
        }

        return var3;
    }

    protected Socket tcpConnectForDataTransferChannel(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(this.readTimeout * 1000);
        socket.setSoLinger(true, this.closeTimeout);
        socket.setReceiveBufferSize(524288);
        socket.setSendBufferSize(524288);
        socket.connect(new InetSocketAddress(host, port), this.connectionTimeout * 1000);
        return socket;
    }

    public void abortConnectForCommunicationChannel() {
        if (this.connectingCommunicationChannelSocket != null) {
            try {
                this.connectingCommunicationChannelSocket.close();
            } catch (Throwable var2) {
            }
        }

    }

    public abstract Socket connectForCommunicationChannel(String var1, int var2) throws IOException;

    public abstract Socket connectForDataTransferChannel(String var1, int var2) throws IOException;
}
