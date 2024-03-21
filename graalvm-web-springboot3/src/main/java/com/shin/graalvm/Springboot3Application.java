package com.shin.graalvm;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Springboot3Application {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Springboot3Application.class);
        //banner模式默认在控制台显示,Banner.Mode.OFF : 关闭banner显示
//        springApplication.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext run = springApplication.run(args);

        Environment environment = run.getBean(Environment.class);
        // 自动打开浏览器访问项目地址
        String osName = System.getProperty("os.name", "");
        if (osName.contains("Windows")) {
            try {
                Runtime.getRuntime().exec("cmd /c start http://127.0.0.1:" + environment.getProperty("server.port") + "?autoclose=1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
