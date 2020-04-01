package it.sauronsoftware.ftp4j;

public interface FTPListParser {
    FTPFile[] parse(String[] var1) throws FTPListParseException;
}
