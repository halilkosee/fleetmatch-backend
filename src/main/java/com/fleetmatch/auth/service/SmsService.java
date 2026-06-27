package com.fleetmatch.auth.service;

public interface SmsService {

    void sendOtp(String phone, String code, String purpose);
}
