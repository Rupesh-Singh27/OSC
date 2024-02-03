package org.orosoft.userservice.common;

import java.util.Random;
import java.util.UUID;

public class HelperClass {

    public static Long generateOTP() {
        Random random = new Random();
        long otp = random.nextLong(900000) + 100000;
        System.out.println("OTP Generated: " + otp);
        return otp;
    }

    public static String generateCustomSessionId(){
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
