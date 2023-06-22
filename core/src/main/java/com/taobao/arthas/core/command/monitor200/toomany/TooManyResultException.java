package com.taobao.arthas.core.command.monitor200.toomany;

public class TooManyResultException extends Exception{
    public TooManyResultException(){
        super();
    }

    public TooManyResultException(String message){
        super(message);
    }

    public TooManyResultException(String message, Throwable cause){
        super(message,cause);
    }
}
