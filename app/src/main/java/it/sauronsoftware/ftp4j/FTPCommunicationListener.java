package it.sauronsoftware.ftp4j;

public interface FTPCommunicationListener {
    void sent(String var1);

    void received(String var1);
}
