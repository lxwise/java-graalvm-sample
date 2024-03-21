package com.star.graalvm;

import com.star.graalvm.conf.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;


import java.util.regex.Pattern;

/**
 * @create 2023-09
 * @author lstar
 * @description: 主程序
 */
public class App extends Application {

    public static void main(String[] args) {
        // 解决Linux上编译为native-image时运行错误：
        String osName = System.getProperty("os.name", "");
        if (Pattern.matches("Linux.*", osName)) {
            System.setProperty("prism.forceGPU", "true");
        }

        AppConfig.init();
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        // 设置系统屏幕缩放比例
        try {
            var scaleX =  Screen.getScreens().get(0).getOutputScaleX();
            System.setProperty("glass.win.uiScale", String.valueOf(scaleX));
        } catch (Exception ignored) {
            System.setProperty("glass.win.uiScale", "1.0");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载并创建主场景
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
        Scene scene = new Scene(root, AppConfig.stageWidth, AppConfig.stageHeight);
        // 设置窗口信息
        primaryStage.setTitle(AppConfig.title);
        primaryStage.setResizable(AppConfig.stageResizable);
        primaryStage.getIcons().add(new Image(App.class.getResourceAsStream(AppConfig.icon)));
        primaryStage.setScene(scene);
        primaryStage.show();

    }


    @Override
    public void stop() throws Exception {
        System.out.println("stop");
        super.stop();
    }
}
