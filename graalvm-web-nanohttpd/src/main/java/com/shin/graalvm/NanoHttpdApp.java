package com.shin.graalvm;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @create 2023-09
 * @author lstar
 * @description: 主程序
 */
public class NanoHttpdApp extends NanoHTTPD {

    private static final int SERVER_PORT = 9999;

    public NanoHttpdApp(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    public static void main(String[] args )
    {

        try {
            //读取配置文件
            Properties properties = new Properties();
            InputStream in = NanoHttpdApp.class.getResourceAsStream("/application.properties");
            properties.load(in);
            int port = Integer.parseInt(properties.getProperty("server.port"));

            //启动服务
            new NanoHttpdApp(port);

            onApplicationRunning();
        } catch (Exception e) {
            System.err.println("Couldn't initiate server:\n" + e);
        }

    }

    @Override
    public Response serve(IHTTPSession session) {
        // System.out.println(session.getUri());
        Map<String, String> params = session.getParms();

        if ("/".equals(session.getUri())) {
            String html = """
                    <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta http-equiv="X-UA-Compatible" content="IE=edge">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>NanoHttpd</title>
                        </head>
                        <body>
                            <h1>NanoHttpd，在 Java 中易于嵌入的小型 HTTP 服务器</h1>
                            <ul>
                                <li><a href="/">/</a></li>
                                <li><a href="/text">/text</a></li>
                                <li><a href="/json">/json</a></li>
                            </ul>
                            <script>
                                var autoclose = getUrlParam("autoclose");
                                if (autoclose) {
                                    checkIfServerIsDown();
                                }
                                // 检查服务器是否关闭，如果关闭则自动关闭当前页签
                                function checkIfServerIsDown() {
                                    setInterval(function () {
                                        var xhr = new XMLHttpRequest();
                                        xhr.open('GET', "/", true);
                                        xhr.onreadystatechange = function () {
                                            if (xhr.readyState === 4 && xhr.status === 200 || xhr.status === 304) {
                                                var data = xhr.responseText;
                                            }
                                        };
                                        xhr.onerror = function (e) {
                                            open(location, '_self').close();
                                        };
                                        xhr.send();
                                    }, 1000);
                                }
                                // 获取网址参数
                                function getUrlParam (name) {
                                    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
                                    var r = window.location.search.substr(1).match(reg);
                                    if (r != null) return unescape(r[2]); return null;
                                }
                            </script>
                        </body>
                    </html>
                    """;
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html);
        } else if ("/text".equals(session.getUri())) {
            String text = "Hello NanoHttpd";
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", text);
        } else if ("/json".equals(session.getUri())) {
            String data = """
                    {
                        "code": 0,
                        "message": "success",
                        "data": {
                        "text":"hello nanohttpd",
                        "age":10,
                        "isFail":false
                        }
                    }
                    """;
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", data);
        }

        return newFixedLengthResponse("");
    }

    public static void onApplicationRunning() {
        // 自动打开浏览器访问项目地址
        String osName = System.getProperty("os.name", "");
        if (osName.contains("Windows")) {
            try {
                Runtime.getRuntime().exec("cmd /c start http://127.0.0.1:" + SERVER_PORT + "?autoclose=1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
