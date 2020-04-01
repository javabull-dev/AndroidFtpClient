package it.sauronsoftware.ftp4j.extrecognizers;

import it.sauronsoftware.ftp4j.FTPTextualExtensionRecognizer;
import java.util.ArrayList;

public class ParametricTextualExtensionRecognizer implements FTPTextualExtensionRecognizer {
    private ArrayList exts = new ArrayList();

    public ParametricTextualExtensionRecognizer() {
    }

    public ParametricTextualExtensionRecognizer(String[] exts) {
        for(int i = 0; i < exts.length; ++i) {
            this.addExtension(exts[i]);
        }

    }

    public ParametricTextualExtensionRecognizer(ArrayList exts) {
        int size = exts.size();

        for(int i = 0; i < size; ++i) {
            Object aux = exts.get(i);
            if (aux instanceof String) {
                String ext = (String)aux;
                this.addExtension(ext);
            }
        }

    }

    public void addExtension(String ext) {
        synchronized(this.exts) {
            ext = ext.toLowerCase();
            this.exts.add(ext);
        }
    }

    public void removeExtension(String ext) {
        synchronized(this.exts) {
            ext = ext.toLowerCase();
            this.exts.remove(ext);
        }
    }

    public String[] getExtensions() {
        synchronized(this.exts) {
            int size = this.exts.size();
            String[] ret = new String[size];

            for(int i = 0; i < size; ++i) {
                ret[i] = (String)this.exts.get(i);
            }

            return ret;
        }
    }

    public boolean isTextualExt(String ext) {
        synchronized(this.exts) {
            return this.exts.contains(ext);
        }
    }
}
