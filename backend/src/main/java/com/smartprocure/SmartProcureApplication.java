package com.smartprocure;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class SmartProcureApplication {

    public static void main(String[] args) {
        // Automatically load .env from project root or current directory
        loadDotenv();
        SpringApplication.run(SmartProcureApplication.class, args);
    }

    private static void loadDotenv() {
        try {
            // Check current directory or parent directory for .env
            File rootEnv = new File("../.env");
            File currentEnv = new File(".env");
            
            Dotenv dotenv = null;
            if (rootEnv.exists()) {
                dotenv = Dotenv.configure().directory("../").ignoreIfMissing().load();
            } else if (currentEnv.exists()) {
                dotenv = Dotenv.configure().ignoreIfMissing().load();
            }

            if (dotenv != null) {
                dotenv.entries().forEach(entry -> {
                    // Set as system property if not already set in environment
                    if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
            }
        } catch (Exception ignored) {
            // Gracefully continue if dotenv fails or is not present
        }
    }
}
