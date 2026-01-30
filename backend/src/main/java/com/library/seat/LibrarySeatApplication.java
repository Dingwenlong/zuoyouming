package com.library.seat;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@SpringBootApplication
@MapperScan("com.library.seat.modules.*.mapper")
@EnableScheduling
public class LibrarySeatApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(LibrarySeatApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path");
        if (path == null || "/".equals(path)) {
            path = "";
        }
        log.info("\n----------------------------------------------------------\n\t" +
                "Application Library-Seat-System is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + path + "/\n\t" +
                "External: \thttp://" + ip + ":" + port + path + "/\n\t" +
                "Swagger-UI: \thttp://localhost:" + port + path + "/swagger-ui/index.html\n\t" +
                "Knife4j-UI: \thttp://localhost:" + port + path + "/doc.html\n" +
                "----------------------------------------------------------");
    }

}
