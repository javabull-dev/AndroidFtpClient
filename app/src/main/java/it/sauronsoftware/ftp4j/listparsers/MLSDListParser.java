package it.sauronsoftware.ftp4j.listparsers;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

public class MLSDListParser implements FTPListParser {
    private static final DateFormat MLSD_DATE_FORMAT_1 = new SimpleDateFormat("yyyyMMddhhmmss.SSS Z");
    private static final DateFormat MLSD_DATE_FORMAT_2 = new SimpleDateFormat("yyyyMMddhhmmss Z");

    public MLSDListParser() {
    }

    public FTPFile[] parse(String[] lines) throws FTPListParseException {
        ArrayList list = new ArrayList();

        int size;
        for(size = 0; size < lines.length; ++size) {
            FTPFile file = this.parseLine(lines[size]);
            if (file != null) {
                list.add(file);
            }
        }

        size = list.size();
        FTPFile[] ret = new FTPFile[size];

        for(int i = 0; i < size; ++i) {
            ret[i] = (FTPFile)list.get(i);
        }

        return ret;
    }

    private FTPFile parseLine(String line) throws FTPListParseException {
        ArrayList list = new ArrayList();
        StringTokenizer st = new StringTokenizer(line, ";");

        String name;
        while(st.hasMoreElements()) {
            name = st.nextToken().trim();
            if (name.length() > 0) {
                list.add(name);
            }
        }

        if (list.size() == 0) {
            throw new FTPListParseException();
        } else {
            name = (String)list.remove(list.size() - 1);
            Properties facts = new Properties();
            Iterator i = list.iterator();

            String typeString;
            String modifyString;
            while(i.hasNext()) {
                typeString = (String)i.next();
                int sep = typeString.indexOf(61);
                if (sep == -1) {
                    throw new FTPListParseException();
                }

                modifyString = typeString.substring(0, sep).trim();
                String value = typeString.substring(sep + 1, typeString.length()).trim();
                if (modifyString.length() == 0 || value.length() == 0) {
                    throw new FTPListParseException();
                }

                facts.setProperty(modifyString, value);
            }

            typeString = facts.getProperty("type");
            if (typeString == null) {
                throw new FTPListParseException();
            } else {
                byte type;
                if ("file".equalsIgnoreCase(typeString)) {
                    type = 0;
                } else {
                    if (!"dir".equalsIgnoreCase(typeString)) {
                        if ("cdir".equalsIgnoreCase(typeString)) {
                            return null;
                        }

                        if ("pdir".equalsIgnoreCase(typeString)) {
                            return null;
                        }

                        return null;
                    }

                    type = 1;
                }

                Date modifiedDate = null;
                modifyString = facts.getProperty("modify");
                if (modifyString != null) {
                    modifyString = modifyString + " +0000";

                    try {
                        synchronized(MLSD_DATE_FORMAT_1) {
                            modifiedDate = MLSD_DATE_FORMAT_1.parse(modifyString);
                        }
                    } catch (ParseException var18) {
                        try {
                            synchronized(MLSD_DATE_FORMAT_2) {
                                modifiedDate = MLSD_DATE_FORMAT_2.parse(modifyString);
                            }
                        } catch (ParseException var16) {
                        }
                    }
                }

                long size = 0L;
                String sizeString = facts.getProperty("size");
                if (sizeString != null) {
                    try {
                        size = Long.parseLong(sizeString);
                    } catch (NumberFormatException var14) {
                    }

                    if (size < 0L) {
                        size = 0L;
                    }
                }

                FTPFile ret = new FTPFile();
                ret.setType(type);
                ret.setModifiedDate(modifiedDate);
                ret.setSize(size);
                ret.setName(name);
                return ret;
            }
        }
    }
}
