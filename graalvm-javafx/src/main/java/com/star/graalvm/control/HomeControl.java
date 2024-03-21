package com.star.graalvm.control;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;

/**
 * @create 2023-09
 * @author lstar
 * @description: 视图控制类
 */
public class HomeControl {

    @FXML
    public Button alterBtn;
    @FXML
    public Button fileBtn;
    @FXML
    public TextField fileText;
    @FXML
    public AnchorPane rootPane;

    @FXML
    public ImageView imgView;

    @FXML
    public void initialize() {
        System.out.println("init");
    }

    /**
     * 文件按钮单击事件
     */
    @FXML
    public void fileBtnClick(MouseEvent actionEvent) {

        Window window = rootPane.getScene().getWindow();
        FileChooser fc = new FileChooser();
        //设置选择框的左上角标题
        fc.setTitle("单文件选择");
        //设置文件初始化打开路径
        fc.setInitialDirectory(new File("D:" + File.separator));
        //设置文件的选择类型
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片类型", "*.jpg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("文本类型", "*.txt", "*.java", "*.doc", "*.docx", "*.xlx", "*.xlsx", "*.fxml"),
                new FileChooser.ExtensionFilter("所有类型", "*.*")
        );
        //文件显示框 选择的文件返回一个file
        File file = fc.showOpenDialog(window);
        String fileName = file == null ? "" : file.getName();
        String fileAbsolutePath = file == null ? "" : file.getAbsolutePath();
        if (file != null) {
            fileText.setText("文件名:" + fileName+"========" + "文件路径:" + fileAbsolutePath);

            if(isImageFile(file)){
             imgView.setImage(new Image(fileAbsolutePath));
            }
        }
    }

    /**
     * 判断文件后缀
     * @param file
     * @return
     */
    public static boolean isImageFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            // 没有后缀名或者点在文件名末尾，都不是图片
            return false;
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(); // 获取小写后缀名
        return Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension);
    }
    /**
     * 弹出框按钮单击事件
     */
    @FXML
    public void alterBtnClick(MouseEvent actionEvent) {

        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Hello Graalvm");
        alert.setContentText("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        alert.show();


    }

}
