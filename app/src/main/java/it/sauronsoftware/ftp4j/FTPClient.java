package it.sauronsoftware.ftp4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLSocketFactory;

import it.sauronsoftware.ftp4j.connectors.DirectConnector;
import it.sauronsoftware.ftp4j.extrecognizers.DefaultTextualExtensionRecognizer;
import it.sauronsoftware.ftp4j.listparsers.DOSListParser;
import it.sauronsoftware.ftp4j.listparsers.EPLFListParser;
import it.sauronsoftware.ftp4j.listparsers.MLSDListParser;
import it.sauronsoftware.ftp4j.listparsers.NetWareListParser;
import it.sauronsoftware.ftp4j.listparsers.UnixListParser;

public class FTPClient {
    public static final int SECURITY_FTP = 0;
    public static final int SECURITY_FTPS = 1;
    public static final int SECURITY_FTPES = 2;
    public static final int TYPE_AUTO = 0;
    public static final int TYPE_TEXTUAL = 1;
    public static final int TYPE_BINARY = 2;
    public static final int MLSD_IF_SUPPORTED = 0;
    public static final int MLSD_ALWAYS = 1;
    public static final int MLSD_NEVER = 2;
    public static final int SEND_AND_RECEIVE_BUFFER_SIZE = 65536*4;
    private static final DateFormat MDTM_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final Pattern PASV_PATTERN = Pattern.compile("\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3}");
    private static final Pattern PWD_PATTERN = Pattern.compile("\"/.*\"");
    private FTPConnector connector = new DirectConnector();
    private SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
    private ArrayList communicationListeners = new ArrayList();
    private ArrayList listParsers = new ArrayList();
    private FTPTextualExtensionRecognizer textualExtensionRecognizer = DefaultTextualExtensionRecognizer.getInstance();
    private FTPListParser parser = null;
    private String host = null;
    private int port = 0;
    private int security = 0;
    private String username;
    private String password;
    private boolean connected = false;
    private boolean authenticated = false;
    private boolean passive = true;
    private int type = 0;
    private int mlsdPolicy = 0;
    private long autoNoopTimeout = 0L;
    private AutoNoopTimer autoNoopTimer;
    private long nextAutoNoopTime;
    private boolean restSupported = false;
    private String charset = null;
    private boolean compressionEnabled = false;
    private boolean utf8Supported = false;
    private boolean mlsdSupported = false;
    private boolean modezSupported = false;
    private boolean modezEnabled = false;
    private boolean dataChannelEncrypted = false;
    private boolean ongoingDataTransfer = false;
    private InputStream dataTransferInputStream = null;
    private OutputStream dataTransferOutputStream = null;
    private boolean aborted = false;
    private boolean consumeAborCommandReply = false;
    private Object lock = new Object();
    private Object abortLock = new Object();
    private FTPCommunicationChannel communication = null;

    public FTPClient() {
        this.addListParser(new UnixListParser());
        this.addListParser(new DOSListParser());
        this.addListParser(new EPLFListParser());
        this.addListParser(new NetWareListParser());
        this.addListParser(new MLSDListParser());
    }

    public FTPConnector getConnector() {
        synchronized(this.lock) {
            return this.connector;
        }
    }

    public void setConnector(FTPConnector connector) {
        synchronized(this.lock) {
            this.connector = connector;
        }
    }

    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        synchronized(this.lock) {
            this.sslSocketFactory = sslSocketFactory;
        }
    }

    public SSLSocketFactory getSSLSocketFactory() {
        synchronized(this.lock) {
            return this.sslSocketFactory;
        }
    }

    public void setSecurity(int security) throws IllegalStateException, IllegalArgumentException {
        if (security != SECURITY_FTP  && security != SECURITY_FTPS  && security != SECURITY_FTPES ) {
            throw new IllegalArgumentException("Invalid security");
        } else {
            synchronized(this.lock) {
                if (this.connected) {
                    throw new IllegalStateException("The security level of the connection can't be changed while the client is connected");
                } else {
                    this.security = security;
                }
            }
        }
    }

    public int getSecurity() {
        return this.security;
    }

    private Socket ssl(Socket socket, String host, int port) throws IOException {
        return this.sslSocketFactory.createSocket(socket, host, port, true);
    }

    public void setPassive(boolean passive) {
        synchronized(this.lock) {
            this.passive = passive;
        }
    }

    public void setType(int type) throws IllegalArgumentException {
        if (type != SECURITY_FTP  && type != SECURITY_FTPES  && type != SECURITY_FTPS ) {
            throw new IllegalArgumentException("Invalid type");
        } else {
            synchronized(this.lock) {
                this.type = type;
            }
        }
    }

    public int getType() {
        synchronized(this.lock) {
            return this.type;
        }
    }

    public void setMLSDPolicy(int mlsdPolicy) throws IllegalArgumentException {
        if (this.type != MLSD_IF_SUPPORTED  && this.type != TYPE_TEXTUAL  && this.type != TYPE_BINARY ) {
            throw new IllegalArgumentException("Invalid MLSD policy");
        } else {
            synchronized(this.lock) {
                this.mlsdPolicy = mlsdPolicy;
            }
        }
    }

    public int getMLSDPolicy() {
        synchronized(this.lock) {
            return this.mlsdPolicy;
        }
    }

    public String getCharset() {
        synchronized(this.lock) {
            return this.charset;
        }
    }

    public void setCharset(String charset) {
        synchronized(this.lock) {
            this.charset = charset;
            if (this.connected) {
                try {
                    this.communication.changeCharset(this.pickCharset());
                } catch (IOException var5) {
                    var5.printStackTrace();
                }
            }

        }
    }

    public boolean isResumeSupported() {
        synchronized(this.lock) {
            return this.restSupported;
        }
    }

    public boolean isCompressionSupported() {
        return this.modezSupported;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public boolean isCompressionEnabled() {
        return this.compressionEnabled;
    }

    public FTPTextualExtensionRecognizer getTextualExtensionRecognizer() {
        synchronized(this.lock) {
            return this.textualExtensionRecognizer;
        }
    }

    public void setTextualExtensionRecognizer(FTPTextualExtensionRecognizer textualExtensionRecognizer) {
        synchronized(this.lock) {
            this.textualExtensionRecognizer = textualExtensionRecognizer;
        }
    }

    public boolean isAuthenticated() {
        synchronized(this.lock) {
            return this.authenticated;
        }
    }

    public boolean isConnected() {
        synchronized(this.lock) {
            return this.connected;
        }
    }

    public boolean isPassive() {
        synchronized(this.lock) {
            return this.passive;
        }
    }

    public String getHost() {
        synchronized(this.lock) {
            return this.host;
        }
    }

    public int getPort() {
        synchronized(this.lock) {
            return this.port;
        }
    }

    public String getPassword() {
        synchronized(this.lock) {
            return this.password;
        }
    }

    public String getUsername() {
        synchronized(this.lock) {
            return this.username;
        }
    }

    public void setAutoNoopTimeout(long autoNoopTimeout) {
        synchronized(this.lock) {
            if (this.connected && this.authenticated) {
                this.stopAutoNoopTimer();
            }

            long oldValue = this.autoNoopTimeout;
            this.autoNoopTimeout = autoNoopTimeout;
            if (oldValue != 0L && autoNoopTimeout != 0L && this.nextAutoNoopTime > 0L) {
                this.nextAutoNoopTime -= oldValue - autoNoopTimeout;
            }

            if (this.connected && this.authenticated) {
                this.startAutoNoopTimer();
            }

        }
    }

    public long getAutoNoopTimeout() {
        synchronized(this.lock) {
            return this.autoNoopTimeout;
        }
    }

    public void addCommunicationListener(FTPCommunicationListener listener) {
        synchronized(this.lock) {
            this.communicationListeners.add(listener);
            if (this.communication != null) {
                this.communication.addCommunicationListener(listener);
            }

        }
    }

    public void removeCommunicationListener(FTPCommunicationListener listener) {
        synchronized(this.lock) {
            this.communicationListeners.remove(listener);
            if (this.communication != null) {
                this.communication.removeCommunicationListener(listener);
            }

        }
    }

    public FTPCommunicationListener[] getCommunicationListeners() {
        synchronized(this.lock) {
            int size = this.communicationListeners.size();
            FTPCommunicationListener[] ret = new FTPCommunicationListener[size];

            for(int i = 0; i < size; ++i) {
                ret[i] = (FTPCommunicationListener)this.communicationListeners.get(i);
            }

            return ret;
        }
    }

    public void addListParser(FTPListParser listParser) {
        synchronized(this.lock) {
            this.listParsers.add(listParser);
        }
    }

    public void removeListParser(FTPListParser listParser) {
        synchronized(this.lock) {
            this.listParsers.remove(listParser);
        }
    }

    public FTPListParser[] getListParsers() {
        synchronized(this.lock) {
            int size = this.listParsers.size();
            FTPListParser[] ret = new FTPListParser[size];

            for(int i = 0; i < size; ++i) {
                ret[i] = (FTPListParser)this.listParsers.get(i);
            }

            return ret;
        }
    }

    public String[] connect(String host) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        short def;
        if (this.security == 1) {
            def = 990;
        } else {
            def = 21;
        }

        return this.connect(host, def);
    }

    public String[] connect(String host, int port) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (this.connected) {
                throw new IllegalStateException("Client already connected to " + host + " on port " + port);
            } else {
                Socket connection = null;

                String[] var6;
                try {
                    connection = this.connector.connectForCommunicationChannel(host, port);
                    if (this.security == 1) {
                        connection = this.ssl(connection, host, port);
                    }

                    this.communication = new FTPCommunicationChannel(connection, this.pickCharset());
                    Iterator i = this.communicationListeners.iterator();

                    while(i.hasNext()) {
                        this.communication.addCommunicationListener((FTPCommunicationListener)i.next());
                    }

                    FTPReply wm = this.communication.readFTPReply();
                    if (!wm.isSuccessCode()) {
                        throw new FTPException(wm);
                    }

                    this.connected = true;
                    this.authenticated = false;
                    this.parser = null;
                    this.host = host;
                    this.port = port;
                    this.username = null;
                    this.password = null;
                    this.utf8Supported = false;
                    this.restSupported = false;
                    this.mlsdSupported = false;
                    this.modezSupported = false;
                    this.dataChannelEncrypted = false;
                    var6 = wm.getMessages();
                } catch (IOException var17) {
                    throw var17;
                } finally {
                    if (!this.connected && connection != null) {
                        try {
                            connection.close();
                        } catch (Throwable var16) {
                        }
                    }

                }

                return var6;
            }
        }
    }

    public void abortCurrentConnectionAttempt() {
        this.connector.abortConnectForCommunicationChannel();
    }

    public void disconnect(boolean sendQuitCommand) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else {
                if (this.authenticated) {
                    this.stopAutoNoopTimer();
                }

                if (sendQuitCommand) {
                    this.communication.sendFTPCommand("QUIT");
                    FTPReply r = this.communication.readFTPReply();
                    if (!r.isSuccessCode()) {
                        throw new FTPException(r);
                    }
                }

                this.communication.close();
                this.communication = null;
                this.connected = false;
            }
        }
    }

    public void abruptlyCloseCommunication() {
        if (this.communication != null) {
            this.communication.close();
            this.communication = null;
        }

        this.connected = false;
        this.stopAutoNoopTimer();
    }

    public void login(String username, String password) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        this.login(username, password, (String)null);
    }

    public void login(String username, String password, String account) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            }

            if (this.security == 2) {
                this.communication.sendFTPCommand("AUTH TLS");
                FTPReply r = this.communication.readFTPReply();
                if (r.isSuccessCode()) {
                    this.communication.ssl(this.sslSocketFactory);
                } else {
                    this.communication.sendFTPCommand("AUTH SSL");
                    r = this.communication.readFTPReply();
                    if (!r.isSuccessCode()) {
                        throw new FTPException(r.getCode(), "SECURITY_FTPES cannot be applied: the server refused both AUTH TLS and AUTH SSL commands");
                    }

                    this.communication.ssl(this.sslSocketFactory);
                }
            }

            this.authenticated = false;
            this.communication.sendFTPCommand("USER " + username);
            FTPReply r = this.communication.readFTPReply();
            boolean accountRequired;
            boolean passwordRequired;
            switch(r.getCode()) {
            case 230:
                passwordRequired = false;
                accountRequired = false;
                break;
            case 331:
                passwordRequired = true;
                accountRequired = false;
                break;
            case 332:
                passwordRequired = false;
                accountRequired = true;
            default:
                throw new FTPException(r);
            }

            if (passwordRequired) {
                if (password == null) {
                    throw new FTPException(331);
                }

                this.communication.sendFTPCommand("PASS " + password);
                r = this.communication.readFTPReply();
                switch(r.getCode()) {
                case 230:
                    accountRequired = false;
                    break;
                case 332:
                    accountRequired = true;
                    break;
                default:
                    throw new FTPException(r);
                }
            }

            if (accountRequired) {
                if (account == null) {
                    throw new FTPException(332);
                }

                this.communication.sendFTPCommand("ACCT " + account);
                r = this.communication.readFTPReply();
                switch(r.getCode()) {
                case 230:
                    break;
                default:
                    throw new FTPException(r);
                }
            }

            this.authenticated = true;
            this.username = username;
            this.password = password;
        }

        this.postLoginOperations();
        this.startAutoNoopTimer();
    }

    private void postLoginOperations() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            this.utf8Supported = false;
            this.restSupported = false;
            this.mlsdSupported = false;
            this.modezSupported = false;
            this.dataChannelEncrypted = false;
            this.communication.sendFTPCommand("FEAT");
            FTPReply r = this.communication.readFTPReply();
            if (r.getCode() == 211) {
                String[] lines = r.getMessages();

                for(int i = 1; i < lines.length - 1; ++i) {
                    String feat = lines[i].trim().toUpperCase();
                    if ("REST STREAM".equalsIgnoreCase(feat)) {
                        this.restSupported = true;
                    } else if ("UTF8".equalsIgnoreCase(feat)) {
                        this.utf8Supported = true;
                        this.communication.changeCharset("UTF-8");
                    } else if ("MLSD".equalsIgnoreCase(feat)) {
                        this.mlsdSupported = true;
                    } else if ("MODE Z".equalsIgnoreCase(feat) || feat.startsWith("MODE Z ")) {
                        this.modezSupported = true;
                    }
                }
            }

            if (this.utf8Supported) {
                this.communication.sendFTPCommand("OPTS UTF8 ON");
                this.communication.readFTPReply();
            }

            if (this.security == 1 || this.security == 2) {
                this.communication.sendFTPCommand("PBSZ 0");
                this.communication.readFTPReply();
                this.communication.sendFTPCommand("PROT P");
                FTPReply reply = this.communication.readFTPReply();
                if (reply.isSuccessCode()) {
                    this.dataChannelEncrypted = true;
                }
            }

        }
    }

    public void logout() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("REIN");
                FTPReply r = this.communication.readFTPReply();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    this.stopAutoNoopTimer();
                    this.authenticated = false;
                    this.username = null;
                    this.password = null;
                }
            }
        }
    }

    public void noop() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                try {
                    this.communication.sendFTPCommand("NOOP");
                    FTPReply r = this.communication.readFTPReply();
                    if (!r.isSuccessCode()) {
                        throw new FTPException(r);
                    }
                } finally {
                    this.touchAutoNoopTimer();
                }

            }
        }
    }

    public FTPReply sendCustomCommand(String command) throws IllegalStateException, IOException, FTPIllegalReplyException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else {
                this.communication.sendFTPCommand(command);
                this.touchAutoNoopTimer();
                return this.communication.readFTPReply();
            }
        }
    }

    public FTPReply sendSiteCommand(String command) throws IllegalStateException, IOException, FTPIllegalReplyException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else {
                this.communication.sendFTPCommand("SITE " + command);
                this.touchAutoNoopTimer();
                return this.communication.readFTPReply();
            }
        }
    }

    public void changeAccount(String account) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("ACCT " + account);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public String currentDirectory() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("PWD");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    String[] messages = r.getMessages();
                    if (messages.length != 1) {
                        throw new FTPIllegalReplyException();
                    } else {
                        Matcher m = PWD_PATTERN.matcher(messages[0]);
                        if (m.find()) {
                            return messages[0].substring(m.start() + 1, m.end() - 1);
                        } else {
                            throw new FTPIllegalReplyException();
                        }
                    }
                }
            }
        }
    }

    public void changeDirectory(String path) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("CWD " + path);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public void changeDirectoryUp() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("CDUP");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public Date modifiedDate(String path) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("MDTM " + path);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    String[] messages = r.getMessages();
                    if (messages.length != 1) {
                        throw new FTPIllegalReplyException();
                    } else {
                        Date var10000;
                        try {
                            var10000 = MDTM_DATE_FORMAT.parse(messages[0]);
                        } catch (ParseException var7) {
                            throw new FTPIllegalReplyException();
                        }

                        return var10000;
                    }
                }
            }
        }
    }

    public long fileSize(String path) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("TYPE I");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    this.communication.sendFTPCommand("SIZE " + path);
                    r = this.communication.readFTPReply();
                    this.touchAutoNoopTimer();
                    if (!r.isSuccessCode()) {
                        throw new FTPException(r);
                    } else {
                        String[] messages = r.getMessages();
                        if (messages.length != 1) {
                            throw new FTPIllegalReplyException();
                        } else {
                            long var10000;
                            try {
                                var10000 = Long.parseLong(messages[0]);
                            } catch (Throwable var7) {
                                throw new FTPIllegalReplyException();
                            }

                            return var10000;
                        }
                    }
                }
            }
        }
    }

    public void rename(String oldPath, String newPath) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("RNFR " + oldPath);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (r.getCode() != 350) {
                    throw new FTPException(r);
                } else {
                    this.communication.sendFTPCommand("RNTO " + newPath);
                    r = this.communication.readFTPReply();
                    this.touchAutoNoopTimer();
                    if (!r.isSuccessCode()) {
                        throw new FTPException(r);
                    }
                }
            }
        }
    }

    public void deleteFile(String path) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("DELE " + path);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public void deleteDirectory(String path) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("RMD " + path);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public void createDirectory(String directoryName) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("MKD " + directoryName);
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                }
            }
        }
    }

    public String[] help() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("HELP");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    return r.getMessages();
                }
            }
        }
    }

    public String[] serverStatus() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("STAT");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    return r.getMessages();
                }
            }
        }
    }

    public FTPFile[] list(String fileSpec) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException, FTPListParseException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("TYPE A");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    FTPDataTransferConnectionProvider provider = this.openDataTransferChannel();
                    boolean mlsdCommand;
                    if (this.mlsdPolicy == MLSD_IF_SUPPORTED ) {
                        mlsdCommand = this.mlsdSupported;
                    } else if (this.mlsdPolicy == MLSD_ALWAYS ) {
                        mlsdCommand = true;
                    } else {
                        mlsdCommand = false;
                    }

                    String command = mlsdCommand ? "MLSD" : "LIST";
                    if (fileSpec != null && fileSpec.length() > 0) {
                        command = command + " " + fileSpec;
                    }

                    ArrayList lines = new ArrayList();
                    boolean wasAborted = false;
                    this.communication.sendFTPCommand(command);

                    try {
                        Socket dtConnection;
                        try {
                            dtConnection = provider.openDataTransferConnection();
                        } finally {
                            provider.dispose();
                        }

                        synchronized(this.abortLock) {
                            this.ongoingDataTransfer = true;
                            this.aborted = false;
                            this.consumeAborCommandReply = false;
                        }

                        NVTASCIIReader dataReader = null;

                        try {
                            this.dataTransferInputStream = dtConnection.getInputStream();
                            if (this.modezEnabled) {
                                this.dataTransferInputStream = new InflaterInputStream(this.dataTransferInputStream);
                            }

                            dataReader = new NVTASCIIReader(this.dataTransferInputStream, mlsdCommand ? "UTF-8" : this.pickCharset());

                            String line;
                            while((line = dataReader.readLine()) != null) {
                                if (line.length() > 0) {
                                    lines.add(line);
                                }
                            }
                        } catch (IOException var83) {
                            IOException e = var83;
                            synchronized(this.abortLock){}

                            try {
                                if (this.aborted) {
                                    throw new FTPAbortedException();
                                }

                                throw new FTPDataTransferException("I/O error in data transfer", e);
                            } finally {
                                ;
                            }
                        } finally {
                            if (dataReader != null) {
                                try {
                                    dataReader.close();
                                } catch (Throwable var77) {
                                }
                            }

                            try {
                                dtConnection.close();
                            } catch (Throwable var76) {
                            }

                            this.dataTransferInputStream = null;
                            synchronized(this.abortLock) {
                                wasAborted = this.aborted;
                                this.ongoingDataTransfer = false;
                                this.aborted = false;
                            }
                        }
                    } finally {
                        r = this.communication.readFTPReply();
                        this.touchAutoNoopTimer();
                        if (r.getCode() != 150 && r.getCode() != 125) {
                            throw new FTPException(r);
                        }

                        r = this.communication.readFTPReply();
                        if (!wasAborted && r.getCode() != 226) {
                            throw new FTPException(r);
                        }

                        if (this.consumeAborCommandReply) {
                            this.communication.readFTPReply();
                            this.consumeAborCommandReply = false;
                        }

                    }

                    int size = lines.size();
                    String[] list = new String[size];

                    for(int i = 0; i < size; ++i) {
                        list[i] = (String)lines.get(i);
                    }

                    FTPFile[] ret = null;
                    if (mlsdCommand) {
                        MLSDListParser parser = new MLSDListParser();
                        ret = parser.parse(list);
                    } else {
                        if (this.parser != null) {
                            try {
                                ret = this.parser.parse(list);
                            } catch (FTPListParseException var79) {
                                this.parser = null;
                            }
                        }

                        if (ret == null) {
                            Iterator i = this.listParsers.iterator();

                            while(i.hasNext()) {
                                FTPListParser aux = (FTPListParser)i.next();

                                try {
                                    ret = aux.parse(list);
                                    this.parser = aux;
                                    break;
                                } catch (FTPListParseException var82) {
                                }
                            }
                        }
                    }

                    if (ret == null) {
                        throw new FTPListParseException();
                    } else {
                        return ret;
                    }
                }
            }
        }
    }

    public FTPFile[] list() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException, FTPListParseException {
        return this.list((String)null);
    }

    public String[] listNames() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException, FTPListParseException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                this.communication.sendFTPCommand("TYPE A");
                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    ArrayList lines = new ArrayList();
                    boolean wasAborted = false;
                    FTPDataTransferConnectionProvider provider = this.openDataTransferChannel();
                    this.communication.sendFTPCommand("NLST");

                    try {
                        Socket dtConnection;
                        try {
                            dtConnection = provider.openDataTransferConnection();
                        } finally {
                            provider.dispose();
                        }

                        synchronized(this.abortLock) {
                            this.ongoingDataTransfer = true;
                            this.aborted = false;
                            this.consumeAborCommandReply = false;
                        }

                        NVTASCIIReader dataReader = null;

                        try {
                            this.dataTransferInputStream = dtConnection.getInputStream();
                            if (this.modezEnabled) {
                                this.dataTransferInputStream = new InflaterInputStream(this.dataTransferInputStream);
                            }

                            dataReader = new NVTASCIIReader(this.dataTransferInputStream, this.pickCharset());

                            String line;
                            while((line = dataReader.readLine()) != null) {
                                if (line.length() > 0) {
                                    lines.add(line);
                                }
                            }
                        } catch (IOException var70) {
                            IOException e = var70;
                            synchronized(this.abortLock){}

                            try {
                                if (this.aborted) {
                                    throw new FTPAbortedException();
                                }

                                throw new FTPDataTransferException("I/O error in data transfer", e);
                            } finally {
                                ;
                            }
                        } finally {
                            if (dataReader != null) {
                                try {
                                    dataReader.close();
                                } catch (Throwable var66) {
                                }
                            }

                            try {
                                dtConnection.close();
                            } catch (Throwable var65) {
                            }

                            this.dataTransferInputStream = null;
                            synchronized(this.abortLock) {
                                wasAborted = this.aborted;
                                this.ongoingDataTransfer = false;
                                this.aborted = false;
                            }
                        }
                    } finally {
                        r = this.communication.readFTPReply();
                        if (r.getCode() != 150 && r.getCode() != 125) {
                            throw new FTPException(r);
                        }

                        r = this.communication.readFTPReply();
                        if (!wasAborted && r.getCode() != 226) {
                            throw new FTPException(r);
                        }

                        if (this.consumeAborCommandReply) {
                            this.communication.readFTPReply();
                            this.consumeAborCommandReply = false;
                        }

                    }

                    int size = lines.size();
                    String[] list = new String[size];

                    for(int i = 0; i < size; ++i) {
                        list[i] = (String)lines.get(i);
                    }

                    return list;
                }
            }
        }
    }

    public void upload(File file) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.upload(file, 0L, (FTPDataTransferListener)null);
    }

    public void upload(File file, FTPDataTransferListener listener) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.upload(file, 0L, listener);
    }

    public void upload(File file, long restartAt) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.upload(file, restartAt, (FTPDataTransferListener)null);
    }

    public void upload(File file, long restartAt, FTPDataTransferListener listener) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        } else {
            FileInputStream inputStream = null;

            try {
                inputStream = new FileInputStream(file);
            } catch (IOException var27) {
                throw new FTPDataTransferException(var27);
            }

            try {
                this.upload(file.getName(), inputStream, restartAt, restartAt, listener);
            } catch (IllegalStateException var21) {
                throw var21;
            } catch (IOException var22) {
                throw var22;
            } catch (FTPIllegalReplyException var23) {
                throw var23;
            } catch (FTPException var24) {
                throw var24;
            } catch (FTPDataTransferException var25) {
                throw var25;
            } catch (FTPAbortedException var26) {
                throw var26;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var20) {
                    }
                }

            }

        }
    }

    public void upload(String fileName, InputStream inputStream, long restartAt, long streamOffset, FTPDataTransferListener listener) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                int tp = this.type;
                if (tp == 0) {
                    tp = this.detectType(fileName);
                }

                if (tp == 1) {
                    this.communication.sendFTPCommand("TYPE A");
                } else if (tp == 2) {
                    this.communication.sendFTPCommand("TYPE I");
                }

                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    FTPDataTransferConnectionProvider provider = this.openDataTransferChannel();
                    boolean wasAborted;
                    if (this.restSupported || restartAt > 0L) {
                        wasAborted = false;

                        try {
                            this.communication.sendFTPCommand("REST " + restartAt);
                            r = this.communication.readFTPReply();
                            this.touchAutoNoopTimer();
                            if (r.getCode() != 350 && (r.getCode() != 501 && r.getCode() != 502 || restartAt > 0L)) {
                                throw new FTPException(r);
                            }

                            wasAborted = true;
                        } finally {
                            if (!wasAborted) {
                                provider.dispose();
                            }

                        }
                    }

                    wasAborted = false;
                    this.communication.sendFTPCommand("STOR " + fileName);

                    try {
                        Socket dtConnection;
                        try {
                            dtConnection = provider.openDataTransferConnection();
                        } finally {
                            provider.dispose();
                        }

                        synchronized(this.abortLock) {
                            this.ongoingDataTransfer = true;
                            this.aborted = false;
                            this.consumeAborCommandReply = false;
                        }

                        try {
                            inputStream.skip(streamOffset);
                            this.dataTransferOutputStream = dtConnection.getOutputStream();
                            if (this.modezEnabled) {
                                this.dataTransferOutputStream = new DeflaterOutputStream(this.dataTransferOutputStream);
                            }

                            if (listener != null) {
                                listener.started();
                            }

                            if (tp == 1) {
                                Reader reader = new InputStreamReader(inputStream);
                                Writer writer = new OutputStreamWriter(this.dataTransferOutputStream, this.pickCharset());
                                char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = reader.read(buffer)) != -1) {
                                    writer.write(buffer, 0, l);
                                    writer.flush();
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            } else if (tp == 2) {
                                byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = inputStream.read(buffer)) != -1) {
                                    this.dataTransferOutputStream.write(buffer, 0, l);
                                    this.dataTransferOutputStream.flush();
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            }
                        } catch (IOException var93) {
                            IOException e = var93;
                            synchronized(this.abortLock){}

                            try {
                                if (this.aborted) {
                                    if (listener != null) {
                                        listener.aborted();
                                    }

                                    throw new FTPAbortedException();
                                }

                                if (listener != null) {
                                    listener.failed();
                                }

                                throw new FTPDataTransferException("I/O error in data transfer", e);
                            } finally {
                                ;
                            }
                        } finally {
                            if (this.dataTransferOutputStream != null) {
                                try {
                                    this.dataTransferOutputStream.close();
                                } catch (Throwable var89) {
                                }
                            }

                            try {
                                dtConnection.close();
                            } catch (Throwable var88) {
                            }

                            this.dataTransferOutputStream = null;
                            synchronized(this.abortLock) {
                                wasAborted = this.aborted;
                                this.ongoingDataTransfer = false;
                                this.aborted = false;
                            }
                        }
                    } finally {
                        r = this.communication.readFTPReply();
                        this.touchAutoNoopTimer();
                        if (r.getCode() != 150 && r.getCode() != 125) {
                            throw new FTPException(r);
                        }

                        r = this.communication.readFTPReply();
                        if (!wasAborted && r.getCode() != 226) {
                            throw new FTPException(r);
                        }

                        if (this.consumeAborCommandReply) {
                            this.communication.readFTPReply();
                            this.consumeAborCommandReply = false;
                        }

                    }

                    if (listener != null) {
                        listener.completed();
                    }

                }
            }
        }
    }

    public void append(File file) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.append(file, (FTPDataTransferListener)null);
    }

    public void append(File file, FTPDataTransferListener listener) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        } else {
            FileInputStream inputStream = null;

            try {
                inputStream = new FileInputStream(file);
            } catch (IOException var25) {
                throw new FTPDataTransferException(var25);
            }

            try {
                this.append(file.getName(), inputStream, 0L, listener);
            } catch (IllegalStateException var19) {
                throw var19;
            } catch (IOException var20) {
                throw var20;
            } catch (FTPIllegalReplyException var21) {
                throw var21;
            } catch (FTPException var22) {
                throw var22;
            } catch (FTPDataTransferException var23) {
                throw var23;
            } catch (FTPAbortedException var24) {
                throw var24;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var18) {
                    }
                }

            }

        }
    }

    public void append(String fileName, InputStream inputStream, long streamOffset, FTPDataTransferListener listener) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                int tp = this.type;
                if (tp == 0) {
                    tp = this.detectType(fileName);
                }

                if (tp == 1) {
                    this.communication.sendFTPCommand("TYPE A");
                } else if (tp == 2) {
                    this.communication.sendFTPCommand("TYPE I");
                }

                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    boolean wasAborted = false;
                    FTPDataTransferConnectionProvider provider = this.openDataTransferChannel();
                    this.communication.sendFTPCommand("APPE " + fileName);

                    try {
                        Socket dtConnection;
                        try {
                            dtConnection = provider.openDataTransferConnection();
                        } finally {
                            provider.dispose();
                        }

                        synchronized(this.abortLock) {
                            this.ongoingDataTransfer = true;
                            this.aborted = false;
                            this.consumeAborCommandReply = false;
                        }

                        try {
                            inputStream.skip(streamOffset);
                            this.dataTransferOutputStream = dtConnection.getOutputStream();
                            if (this.modezEnabled) {
                                this.dataTransferOutputStream = new DeflaterOutputStream(this.dataTransferOutputStream);
                            }

                            if (listener != null) {
                                listener.started();
                            }

                            if (tp == 1) {
                                Reader reader = new InputStreamReader(inputStream);
                                Writer writer = new OutputStreamWriter(this.dataTransferOutputStream, this.pickCharset());
                                char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = reader.read(buffer)) != -1) {
                                    writer.write(buffer, 0, l);
                                    writer.flush();
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            } else if (tp == 2) {
                                byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = inputStream.read(buffer)) != -1) {
                                    this.dataTransferOutputStream.write(buffer, 0, l);
                                    this.dataTransferOutputStream.flush();
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            }
                        } catch (IOException var76) {
                            IOException e = var76;
                            synchronized(this.abortLock){}

                            try {
                                if (this.aborted) {
                                    if (listener != null) {
                                        listener.aborted();
                                    }

                                    throw new FTPAbortedException();
                                }

                                if (listener != null) {
                                    listener.failed();
                                }

                                throw new FTPDataTransferException("I/O error in data transfer", e);
                            } finally {
                                ;
                            }
                        } finally {
                            if (this.dataTransferOutputStream != null) {
                                try {
                                    this.dataTransferOutputStream.close();
                                } catch (Throwable var72) {
                                }
                            }

                            try {
                                dtConnection.close();
                            } catch (Throwable var71) {
                            }

                            this.dataTransferOutputStream = null;
                            synchronized(this.abortLock) {
                                wasAborted = this.aborted;
                                this.ongoingDataTransfer = false;
                                this.aborted = false;
                            }
                        }
                    } finally {
                        r = this.communication.readFTPReply();
                        this.touchAutoNoopTimer();
                        if (r.getCode() != 150 && r.getCode() != 125) {
                            throw new FTPException(r);
                        }

                        r = this.communication.readFTPReply();
                        if (!wasAborted && r.getCode() != 226) {
                            throw new FTPException(r);
                        }

                        if (this.consumeAborCommandReply) {
                            this.communication.readFTPReply();
                            this.consumeAborCommandReply = false;
                        }

                    }

                    if (listener != null) {
                        listener.completed();
                    }

                }
            }
        }
    }

    public void download(String remoteFileName, File localFile) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.download(remoteFileName, (File)localFile, 0L, (FTPDataTransferListener)null);
    }

    public void download(String remoteFileName, File localFile, FTPDataTransferListener listener) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.download(remoteFileName, localFile, 0L, listener);
    }

    public void download(String remoteFileName, File localFile, long restartAt) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        this.download(remoteFileName, (File)localFile, restartAt, (FTPDataTransferListener)null);
    }

    public void download(String remoteFileName, File localFile, long restartAt, FTPDataTransferListener listener) throws IllegalStateException, FileNotFoundException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(localFile, restartAt > 0L);
        } catch (IOException var28) {
            throw new FTPDataTransferException(var28);
        }

        try {
            this.download(remoteFileName, (OutputStream)outputStream, restartAt, listener);
        } catch (IllegalStateException var22) {
            throw var22;
        } catch (IOException var23) {
            throw var23;
        } catch (FTPIllegalReplyException var24) {
            throw var24;
        } catch (FTPException var25) {
            throw var25;
        } catch (FTPDataTransferException var26) {
            throw var26;
        } catch (FTPAbortedException var27) {
            throw var27;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable var21) {
                }
            }

        }

    }

    public void download(String fileName, OutputStream outputStream, long restartAt, FTPDataTransferListener listener) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException {
        synchronized(this.lock) {
            if (!this.connected) {
                throw new IllegalStateException("Client not connected");
            } else if (!this.authenticated) {
                throw new IllegalStateException("Client not authenticated");
            } else {
                int tp = this.type;
                if (tp == 0) {
                    tp = this.detectType(fileName);
                }

                if (tp == 1) {
                    this.communication.sendFTPCommand("TYPE A");
                } else if (tp == 2) {
                    this.communication.sendFTPCommand("TYPE I");
                }

                FTPReply r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (!r.isSuccessCode()) {
                    throw new FTPException(r);
                } else {
                    FTPDataTransferConnectionProvider provider = this.openDataTransferChannel();
                    boolean wasAborted;
                    if (this.restSupported || restartAt > 0L) {
                        wasAborted = false;

                        try {
                            this.communication.sendFTPCommand("REST " + restartAt);
                            r = this.communication.readFTPReply();
                            this.touchAutoNoopTimer();
                            if (r.getCode() != 350 && (r.getCode() != 501 && r.getCode() != 502 || restartAt > 0L)) {
                                throw new FTPException(r);
                            }

                            wasAborted = true;
                        } finally {
                            if (!wasAborted) {
                                provider.dispose();
                            }

                        }
                    }

                    wasAborted = false;
                    this.communication.sendFTPCommand("RETR " + fileName);

                    try {
                        Socket dtConnection;
                        try {
                            dtConnection = provider.openDataTransferConnection();
                        } finally {
                            provider.dispose();
                        }

                        synchronized(this.abortLock) {
                            this.ongoingDataTransfer = true;
                            this.aborted = false;
                            this.consumeAborCommandReply = false;
                        }

                        try {
                            this.dataTransferInputStream = dtConnection.getInputStream();
                            if (this.modezEnabled) {
                                this.dataTransferInputStream = new InflaterInputStream(this.dataTransferInputStream);
                            }

                            if (listener != null) {
                                listener.started();
                            }

                            if (tp == 1) {
                                Reader reader = new InputStreamReader(this.dataTransferInputStream, this.pickCharset());
                                Writer writer = new OutputStreamWriter(outputStream);
                                char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = reader.read(buffer, 0, buffer.length)) != -1) {
                                    writer.write(buffer, 0, l);
                                    writer.flush();
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            } else if (tp == 2) {
                                byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];

                                int l;
                                while((l = this.dataTransferInputStream.read(buffer, 0, buffer.length)) != -1) {
                                    outputStream.write(buffer, 0, l);
                                    if (listener != null) {
                                        listener.transferred(l);
                                    }
                                }
                            }
                        } catch (IOException var91) {
                            IOException e = var91;
                            synchronized(this.abortLock){}

                            try {
                                if (this.aborted) {
                                    if (listener != null) {
                                        listener.aborted();
                                    }

                                    throw new FTPAbortedException();
                                }

                                if (listener != null) {
                                    listener.failed();
                                }

                                throw new FTPDataTransferException("I/O error in data transfer", e);
                            } finally {
                                ;
                            }
                        } finally {
                            if (this.dataTransferInputStream != null) {
                                try {
                                    this.dataTransferInputStream.close();
                                } catch (Throwable var87) {
                                }
                            }

                            try {
                                dtConnection.close();
                            } catch (Throwable var86) {
                            }

                            this.dataTransferInputStream = null;
                            synchronized(this.abortLock) {
                                wasAborted = this.aborted;
                                this.ongoingDataTransfer = false;
                                this.aborted = false;
                            }
                        }
                    } finally {
                        r = this.communication.readFTPReply();
                        this.touchAutoNoopTimer();
                        if (r.getCode() != 150 && r.getCode() != 125) {
                            throw new FTPException(r);
                        }

                        r = this.communication.readFTPReply();
                        if (!wasAborted && r.getCode() != 226) {
                            throw new FTPException(r);
                        }

                        if (this.consumeAborCommandReply) {
                            this.communication.readFTPReply();
                            this.consumeAborCommandReply = false;
                        }

                    }

                    if (listener != null) {
                        listener.completed();
                    }

                }
            }
        }
    }

    private int detectType(String fileName) throws IOException, FTPIllegalReplyException, FTPException {
        int start = fileName.lastIndexOf(46) + 1;
        int stop = fileName.length();
        if (start > 0 && start < stop - 1) {
            String ext = fileName.substring(start, stop);
            ext = ext.toLowerCase();
            return this.textualExtensionRecognizer.isTextualExt(ext) ? 1 : 2;
        } else {
            return 2;
        }
    }

    private FTPDataTransferConnectionProvider openDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException {
        FTPReply r;
        if (this.modezSupported && this.compressionEnabled) {
            if (!this.modezEnabled) {
                this.communication.sendFTPCommand("MODE Z");
                r = this.communication.readFTPReply();
                this.touchAutoNoopTimer();
                if (r.isSuccessCode()) {
                    this.modezEnabled = true;
                }
            }
        } else if (this.modezEnabled) {
            this.communication.sendFTPCommand("MODE S");
            r = this.communication.readFTPReply();
            this.touchAutoNoopTimer();
            if (r.isSuccessCode()) {
                this.modezEnabled = false;
            }
        }

        return this.passive ? this.openPassiveDataTransferChannel() : this.openActiveDataTransferChannel();
    }

    private FTPDataTransferConnectionProvider openActiveDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException {
        FTPDataTransferServer server = new FTPDataTransferServer() {
            public Socket openDataTransferConnection() throws FTPDataTransferException {
                Socket socket = super.openDataTransferConnection();
                if (FTPClient.this.dataChannelEncrypted) {
                    try {
                        socket = FTPClient.this.ssl(socket, socket.getInetAddress().getHostName(), socket.getPort());
                    } catch (IOException var5) {
                        try {
                            socket.close();
                        } catch (Throwable var4) {
                        }

                        throw new FTPDataTransferException(var5);
                    }
                }

                return socket;
            }
        };
        int port = server.getPort();
        int p1 = port >>> 8;
        int p2 = port & 255;
        int[] addr = this.pickLocalAddress();
        this.communication.sendFTPCommand("PORT " + addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3] + "," + p1 + "," + p2);
        FTPReply r = this.communication.readFTPReply();
        this.touchAutoNoopTimer();
        if (!r.isSuccessCode()) {
            server.dispose();

            try {
                Socket aux = server.openDataTransferConnection();
                aux.close();
            } catch (Throwable var8) {
            }

            throw new FTPException(r);
        } else {
            return server;
        }
    }

    private FTPDataTransferConnectionProvider openPassiveDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException {
        this.communication.sendFTPCommand("PASV");
        FTPReply r = this.communication.readFTPReply();
        this.touchAutoNoopTimer();
        if (!r.isSuccessCode()) {
            throw new FTPException(r);
        } else {
            String addressAndPort = null;
            String[] messages = r.getMessages();

            int b2;
            int b3;
            for(int i = 0; i < messages.length; ++i) {
                Matcher m = PASV_PATTERN.matcher(messages[i]);
                if (m.find()) {
                    b2 = m.start();
                    b3 = m.end();
                    addressAndPort = messages[i].substring(b2, b3);
                    break;
                }
            }

            if (addressAndPort == null) {
                throw new FTPIllegalReplyException();
            } else {
                StringTokenizer st = new StringTokenizer(addressAndPort, ",");
                int b1 = Integer.parseInt(st.nextToken());
                b2 = Integer.parseInt(st.nextToken());
                b3 = Integer.parseInt(st.nextToken());
                int b4 = Integer.parseInt(st.nextToken());
                int p1 = Integer.parseInt(st.nextToken());
                int p2 = Integer.parseInt(st.nextToken());
                final String pasvHost = b1 + "." + b2 + "." + b3 + "." + b4;
                final int pasvPort = p1 << 8 | p2;
                FTPDataTransferConnectionProvider provider = new FTPDataTransferConnectionProvider() {
                    public Socket openDataTransferConnection() throws FTPDataTransferException {
                        try {
                            String selectedHost = FTPClient.this.connector.getUseSuggestedAddressForDataConnections() ? pasvHost : FTPClient.this.host;
                            Socket dtConnection = FTPClient.this.connector.connectForDataTransferChannel(selectedHost, pasvPort);
                            if (FTPClient.this.dataChannelEncrypted) {
                                dtConnection = FTPClient.this.ssl(dtConnection, selectedHost, pasvPort);
                            }

                            return dtConnection;
                        } catch (IOException var3) {
                            throw new FTPDataTransferException("Cannot connect to the remote server", var3);
                        }
                    }

                    public void dispose() {
                    }
                };
                return provider;
            }
        }
    }

    public void abortCurrentDataTransfer(boolean sendAborCommand) throws IOException, FTPIllegalReplyException {
        synchronized(this.abortLock) {
            if (this.ongoingDataTransfer && !this.aborted) {
                if (sendAborCommand) {
                    this.communication.sendFTPCommand("ABOR");
                    this.touchAutoNoopTimer();
                    this.consumeAborCommandReply = true;
                }

                if (this.dataTransferInputStream != null) {
                    try {
                        this.dataTransferInputStream.close();
                    } catch (Throwable var6) {
                    }
                }

                if (this.dataTransferOutputStream != null) {
                    try {
                        this.dataTransferOutputStream.close();
                    } catch (Throwable var5) {
                    }
                }

                this.aborted = true;
            }

        }
    }

    private String pickCharset() {
        if (this.charset != null) {
            return this.charset;
        } else {
            return this.utf8Supported ? "UTF-8" : System.getProperty("file.encoding");
        }
    }

    private int[] pickLocalAddress() throws IOException {
        int[] ret = this.pickForcedLocalAddress();
        if (ret == null) {
            ret = this.pickAutoDetectedLocalAddress();
        }

        return ret;
    }

    private int[] pickForcedLocalAddress() {
        int[] ret = null;
        String aux = System.getProperty("ftp4j.activeDataTransfer.hostAddress");
        if (aux != null) {
            boolean valid = false;
            StringTokenizer st = new StringTokenizer(aux, ".");
            if (st.countTokens() == 4) {
                valid = true;
                int[] arr = new int[4];

                for(int i = 0; i < 4; ++i) {
                    String tk = st.nextToken();

                    try {
                        arr[i] = Integer.parseInt(tk);
                    } catch (NumberFormatException var9) {
                        arr[i] = -1;
                    }

                    if (arr[i] < 0 || arr[i] > 255) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    ret = arr;
                }
            }

            if (!valid) {
                System.err.println("WARNING: invalid value \"" + aux + "\" for the " + "ftp4j.activeDataTransfer.hostAddress" + " system property. The value should " + "be in the x.x.x.x form.");
            }
        }

        return ret;
    }

    private int[] pickAutoDetectedLocalAddress() throws IOException {
        InetAddress addressObj = InetAddress.getLocalHost();
        byte[] addr = addressObj.getAddress();
        int b1 = addr[0] & 255;
        int b2 = addr[1] & 255;
        int b3 = addr[2] & 255;
        int b4 = addr[3] & 255;
        int[] ret = new int[]{b1, b2, b3, b4};
        return ret;
    }

    public String toString() {
        synchronized(this.lock) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(this.getClass().getName());
            buffer.append(" [connected=");
            buffer.append(this.connected);
            if (this.connected) {
                buffer.append(", host=");
                buffer.append(this.host);
                buffer.append(", port=");
                buffer.append(this.port);
            }

            buffer.append(", connector=");
            buffer.append(this.connector);
            buffer.append(", security=");
            switch(this.security) {
            case 0:
                buffer.append("SECURITY_FTP");
                break;
            case 1:
                buffer.append("SECURITY_FTPS");
                break;
            case 2:
                buffer.append("SECURITY_FTPES");
            }

            buffer.append(", authenticated=");
            buffer.append(this.authenticated);
            int i;
            if (this.authenticated) {
                buffer.append(", username=");
                buffer.append(this.username);
                buffer.append(", password=");
                StringBuffer buffer2 = new StringBuffer();

                for(i = 0; i < this.password.length(); ++i) {
                    buffer2.append('*');
                }

                buffer.append(buffer2);
                buffer.append(", restSupported=");
                buffer.append(this.restSupported);
                buffer.append(", utf8supported=");
                buffer.append(this.utf8Supported);
                buffer.append(", mlsdSupported=");
                buffer.append(this.mlsdSupported);
                buffer.append(", mode=modezSupported");
                buffer.append(this.modezSupported);
                buffer.append(", mode=modezEnabled");
                buffer.append(this.modezEnabled);
            }

            buffer.append(", transfer mode=");
            buffer.append(this.passive ? "passive" : "active");
            buffer.append(", transfer type=");
            switch(this.type) {
            case 0:
                buffer.append("TYPE_AUTO");
                break;
            case 1:
                buffer.append("TYPE_TEXTUAL");
                break;
            case 2:
                buffer.append("TYPE_BINARY");
            }

            buffer.append(", textualExtensionRecognizer=");
            buffer.append(this.textualExtensionRecognizer);
            FTPListParser[] listParsers = this.getListParsers();
            if (listParsers.length > 0) {
                buffer.append(", listParsers=");

                for(i = 0; i < listParsers.length; ++i) {
                    if (i > 0) {
                        buffer.append(", ");
                    }

                    buffer.append(listParsers[i]);
                }
            }

            FTPCommunicationListener[] communicationListeners = this.getCommunicationListeners();
            if (communicationListeners.length > 0) {
                buffer.append(", communicationListeners=");

                for(int j = 0; j < communicationListeners.length; ++j) {
                    if (j > 0) {
                        buffer.append(", ");
                    }

                    buffer.append(communicationListeners[j]);
                }
            }

            buffer.append(", autoNoopTimeout=");
            buffer.append(this.autoNoopTimeout);
            buffer.append("]");
            return buffer.toString();
        }
    }

    private void startAutoNoopTimer() {
        if (this.autoNoopTimeout > 0L) {
            this.autoNoopTimer = new AutoNoopTimer();
            this.autoNoopTimer.start();
        }

    }

    private void stopAutoNoopTimer() {
        if (this.autoNoopTimer != null) {
            this.autoNoopTimer.interrupt();
            this.autoNoopTimer = null;
        }

    }

    private void touchAutoNoopTimer() {
        if (this.autoNoopTimer != null) {
            this.nextAutoNoopTime = System.currentTimeMillis() + this.autoNoopTimeout;
        }

    }

    private class AutoNoopTimer extends Thread {
        private AutoNoopTimer() {
        }

        public void run() {
            synchronized(FTPClient.this.lock) {
                if (FTPClient.this.nextAutoNoopTime <= 0L && FTPClient.this.autoNoopTimeout > 0L) {
                    FTPClient.this.nextAutoNoopTime = System.currentTimeMillis() + FTPClient.this.autoNoopTimeout;
                }

                while(!Thread.interrupted() && FTPClient.this.autoNoopTimeout > 0L) {
                    long delay = FTPClient.this.nextAutoNoopTime - System.currentTimeMillis();
                    if (delay > 0L) {
                        try {
                            FTPClient.this.lock.wait(delay);
                        } catch (InterruptedException var7) {
                            break;
                        }
                    }

                    if (System.currentTimeMillis() >= FTPClient.this.nextAutoNoopTime) {
                        try {
                            FTPClient.this.noop();
                        } catch (Throwable var6) {
                        }
                    }
                }

            }
        }
    }
}
