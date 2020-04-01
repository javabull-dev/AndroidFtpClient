package it.sauronsoftware.ftp4j.listparsers;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOSListParser implements FTPListParser {
    private static final Pattern PATTERN = Pattern.compile("^(\\d{2})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2})(AM|PM)\\s+(<DIR>|\\d+)\\s+([^\\\\/*?\"<>|]+)$");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy hh:mm a");

    public DOSListParser() {
    }

    public FTPFile[] parse(String[] lines) throws FTPListParseException {
        int size = lines.length;
        FTPFile[] ret = new FTPFile[size];

        for(int i = 0; i < size; ++i) {
            Matcher m = PATTERN.matcher(lines[i]);
            if (!m.matches()) {
                throw new FTPListParseException();
            }

            String month = m.group(1);
            String day = m.group(2);
            String year = m.group(3);
            String hour = m.group(4);
            String minute = m.group(5);
            String ampm = m.group(6);
            String dirOrSize = m.group(7);
            String name = m.group(8);
            ret[i] = new FTPFile();
            ret[i].setName(name);
            if (dirOrSize.equalsIgnoreCase("<DIR>")) {
                ret[i].setType(1);
                ret[i].setSize(0L);
            } else {
                long fileSize;
                try {
                    fileSize = Long.parseLong(dirOrSize);
                } catch (Throwable var20) {
                    throw new FTPListParseException();
                }

                ret[i].setType(0);
                ret[i].setSize(fileSize);
            }

            String mdString = month + "/" + day + "/" + year + " " + hour + ":" + minute + " " + ampm;

            Date md;
            try {
                synchronized(DATE_FORMAT) {
                    md = DATE_FORMAT.parse(mdString);
                }
            } catch (ParseException var19) {
                throw new FTPListParseException();
            }

            ret[i].setModifiedDate(md);
        }

        return ret;
    }
}
