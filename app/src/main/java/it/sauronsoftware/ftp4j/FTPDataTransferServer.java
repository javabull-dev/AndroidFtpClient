package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

class FTPDataTransferServer implements FTPDataTransferConnectionProvider, Runnable {
    private ServerSocket serverSocket = null;
    private Socket socket;
    private IOException exception;
    private Thread thread;

    public FTPDataTransferServer() throws FTPDataTransferException {
        boolean useRange = false;
        String aux = System.getProperty("ftp4j.activeDataTransfer.portRange");
        int start = 0;
        int stop = 0;
        int port;
        if (aux != null) {
            boolean valid = false;
            StringTokenizer st = new StringTokenizer(aux, "-");
            if (st.countTokens() == 2) {
                String s1 = st.nextToken();
                String s2 = st.nextToken();

                try {
                    port = Integer.parseInt(s1);
                } catch (NumberFormatException var15) {
                    port = 0;
                }

                int v2;
                try {
                    v2 = Integer.parseInt(s2);
                } catch (NumberFormatException var14) {
                    v2 = 0;
                }

                if (port > 0 && v2 > 0 && v2 >= port) {
                    start = port;
                    stop = v2;
                    valid = true;
                    useRange = true;
                }
            }

            if (!valid) {
                System.err.println("WARNING: invalid value \"" + aux + "\" for the " + "ftp4j.activeDataTransfer.portRange" + " system property. The value should " + "be in the start-stop form, with " + "start > 0, stop > 0 and start <= stop.");
            }
        }

        if (useRange) {
            ArrayList availables = new ArrayList();

            int size;
            for(size = start; size <= stop; ++size) {
                availables.add(new Integer(size));
            }

            boolean done = false;

            while(!done && (size = availables.size()) > 0) {
                int rand = (int)Math.floor(Math.random() * (double)size);
                port = (Integer)availables.remove(rand);

                try {
                    this.serverSocket = new ServerSocket();
                    this.serverSocket.setReceiveBufferSize(524288);
                    this.serverSocket.bind(new InetSocketAddress(port));
                    done = true;
                } catch (IOException var13) {
                }
            }

            if (!done) {
                throw new FTPDataTransferException("Cannot open the ServerSocket. No available port found in range " + aux);
            }
        } else {
            try {
                this.serverSocket = new ServerSocket();
                this.serverSocket.setReceiveBufferSize(524288);
                this.serverSocket.bind(new InetSocketAddress(0));
            } catch (IOException var12) {
                throw new FTPDataTransferException("Cannot open the ServerSocket", var12);
            }
        }

        this.thread = new Thread(this);
        this.thread.start();
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    public void run() {
        int timeout = 30000;
        String aux = System.getProperty("ftp4j.activeDataTransfer.acceptTimeout");
        if (aux != null) {
            boolean valid = false;

            int value;
            try {
                value = Integer.parseInt(aux);
            } catch (NumberFormatException var17) {
                value = -1;
            }

            if (value >= 0) {
                timeout = value;
                valid = true;
            }

            if (!valid) {
                System.err.println("WARNING: invalid value \"" + aux + "\" for the " + "ftp4j.activeDataTransfer.acceptTimeout" + " system property. The value should " + "be an integer greater or equal to 0.");
            }
        }

        try {
            this.serverSocket.setSoTimeout(timeout);
            this.socket = this.serverSocket.accept();
            this.socket.setSendBufferSize(524288);
        } catch (IOException var15) {
            this.exception = var15;
        } finally {
            try {
                this.serverSocket.close();
            } catch (IOException var14) {
            }

        }

    }

    public void dispose() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException var2) {
            }
        }

    }

    public Socket openDataTransferConnection() throws FTPDataTransferException {
        if (this.socket == null && this.exception == null) {
            try {
                this.thread.join();
            } catch (Exception var2) {
            }
        }

        if (this.exception != null) {
            throw new FTPDataTransferException("Cannot receive the incoming connection", this.exception);
        } else if (this.socket == null) {
            throw new FTPDataTransferException("No socket available");
        } else {
            return this.socket;
        }
    }
}
