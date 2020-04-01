package it.sauronsoftware.ftp4j;

public interface FTPDataTransferListener {
    void started();

    void transferred(int var1);

    void completed();

    void aborted();

    void failed();
}
