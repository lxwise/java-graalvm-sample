## 说明
- Spring Boot 2.7.1 ~ Spring Native 0.12.1 之前长期使用
- Spring Boot 2.7.7 ~ Spring Native 0.12.2 最新

## 构建方式

使用命令行进入项目目录,使用mvn命令构建
``mvn -Pnative native:compile -DskipTests``

注意:在命令行下执行时,需要安装下载native相关jar包执行失败,可使用IDEA执行native:compile命令

exe和jar文件在target下