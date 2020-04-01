package it.sauronsoftware.ftp4j.extrecognizers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class DefaultTextualExtensionRecognizer extends ParametricTextualExtensionRecognizer {
    private static final Object lock = new Object();
    private static DefaultTextualExtensionRecognizer instance = null;

    public static DefaultTextualExtensionRecognizer getInstance() {
        synchronized(lock) {
            if (instance == null) {
                instance = new DefaultTextualExtensionRecognizer();
            }
        }

        return instance;
    }

    private DefaultTextualExtensionRecognizer() {
        BufferedReader r = null;

        try {
            r = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("textualexts")));

            String line;
            while((line = r.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);

                while(st.hasMoreTokens()) {
                    this.addExtension(st.nextToken());
                }
            }
        } catch (Exception var13) {
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Throwable var12) {
                }
            }

        }

    }
}
