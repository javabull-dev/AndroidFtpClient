package com.ljpc.createfile.exception;

/**
 * IO运行时异常，常用于对IOException的包装
 *
 * @author xiaoleilu
 */
public class IORuntimeException extends RuntimeException {
    private static final long serialVersionUID = 8247610319171014183L;

    public IORuntimeException(Throwable e) {
        super(e.getCause());
    }
}
