package com.warranty.service;

/** Loi vi pham nghiep vu, thong bao truc tiep cho nguoi dung. */
public class BusinessException extends Exception {

    public BusinessException(String message) {
        super(message);
    }
}
