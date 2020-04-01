package it.sauronsoftware.ftp4j.connectors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

class Base64 {
    static String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    static char pad = '=';

    Base64() {
    }

    public static String encode(String str) throws RuntimeException {
        byte[] bytes = str.getBytes();
        byte[] encoded = encode(bytes);

        try {
            return new String(encoded, "ASCII");
        } catch (UnsupportedEncodingException var4) {
            throw new RuntimeException("ASCII is not supported!", var4);
        }
    }

    public static String encode(String str, String charset) throws RuntimeException {
        byte[] bytes;
        try {
            bytes = str.getBytes(charset);
        } catch (UnsupportedEncodingException var6) {
            throw new RuntimeException("Unsupported charset: " + charset, var6);
        }

        byte[] encoded = encode(bytes);

        try {
            return new String(encoded, "ASCII");
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("ASCII is not supported!", var5);
        }
    }

    public static String decode(String str) throws RuntimeException {
        byte[] bytes;
        try {
            bytes = str.getBytes("ASCII");
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException("ASCII is not supported!", var3);
        }

        byte[] decoded = decode(bytes);
        return new String(decoded);
    }

    public static String decode(String str, String charset) throws RuntimeException {
        byte[] bytes;
        try {
            bytes = str.getBytes("ASCII");
        } catch (UnsupportedEncodingException var6) {
            throw new RuntimeException("ASCII is not supported!", var6);
        }

        byte[] decoded = decode(bytes);

        try {
            return new String(decoded, charset);
        } catch (UnsupportedEncodingException var5) {
            throw new RuntimeException("Unsupported charset: " + charset, var5);
        }
    }

    public static byte[] encode(byte[] bytes) throws RuntimeException {
        return encode(bytes, 0);
    }

    public static byte[] encode(byte[] bytes, int wrapAt) throws RuntimeException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            encode((InputStream)inputStream, (OutputStream)outputStream, wrapAt);
        } catch (IOException var15) {
            throw new RuntimeException("Unexpected I/O error", var15);
        } finally {
            try {
                inputStream.close();
            } catch (Throwable var14) {
            }

            try {
                outputStream.close();
            } catch (Throwable var13) {
            }

        }

        return outputStream.toByteArray();
    }

    public static byte[] decode(byte[] bytes) throws RuntimeException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            decode((InputStream)inputStream, (OutputStream)outputStream);
        } catch (IOException var14) {
            throw new RuntimeException("Unexpected I/O error", var14);
        } finally {
            try {
                inputStream.close();
            } catch (Throwable var13) {
            }

            try {
                outputStream.close();
            } catch (Throwable var12) {
            }

        }

        return outputStream.toByteArray();
    }

    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        encode((InputStream)inputStream, (OutputStream)outputStream, 0);
    }

    public static void encode(InputStream inputStream, OutputStream outputStream, int wrapAt) throws IOException {
        Base64OutputStream aux = new Base64OutputStream(outputStream, wrapAt);
        copy(inputStream, aux);
        aux.commit();
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        copy(new Base64InputStream(inputStream), outputStream);
    }

    public static void encode(File source, File target, int wrapAt) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);
            encode((InputStream)inputStream, (OutputStream)outputStream, wrapAt);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable var14) {
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var13) {
                }
            }

        }

    }

    public static void encode(File source, File target) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);
            encode((InputStream)inputStream, (OutputStream)outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable var13) {
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var12) {
                }
            }

        }

    }

    public static void decode(File source, File target) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(source);
            outputStream = new FileOutputStream(target);
            decode((InputStream)inputStream, (OutputStream)outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable var13) {
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var12) {
                }
            }

        }

    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] b = new byte[1024];

        int len;
        while((len = inputStream.read(b)) != -1) {
            outputStream.write(b, 0, len);
        }

    }
}
