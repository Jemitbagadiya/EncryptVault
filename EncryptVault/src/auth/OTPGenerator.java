package auth;

import java.util.Random;

// Utility class for generating One-Time Password (OTP)
public class OTPGenerator {

    // Generates a 6-digit numeric OTP
    public static String generateOTP() {

        // Create random number generator instance
        Random r = new Random();

        // Generate a number between 100000 and 999999 to ensure 6 digits
        return String.valueOf(100000 + r.nextInt(900000));
    }
}