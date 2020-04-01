package it.sauronsoftware.ftp4j;

import java.net.Socket;

interface FTPDataTransferConnectionProvider {
    Socket openDataTransferConnection() throws FTPDataTransferException;

    void dispose();
}
