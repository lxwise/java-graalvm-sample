## 构建方式

> 非Docker方式，使用本机 GraalVM 构建 native-image
> Docker方式，自行百度

使用命令行进入项目目录,使用mvn命令构建

profile native 使用了 pluginManagement，不会直接显示native插件，暂时只能通过命令调用

``mvn -Pnative native:compile -DskipTests``

exe和jar文件在target下
