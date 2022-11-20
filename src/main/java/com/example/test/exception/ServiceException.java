package com.example.test.exception;

/**
 * @Auther: CAI
 * @Date: 2022/11/9 - 11 - 09 - 1:02
 * @Description: com.example.test.exception
 * @version: 1.0
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
