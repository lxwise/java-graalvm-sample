package com.shin.graalvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * @author lstar
 * @create 2023-09
 * @description: httpFileServer 上传  下载  创建目录  删除文件/目录  MD5
 */
public class NanoFileServer extends NanoHTTPD {
    private static final Logger LOG = Logger.getLogger(NanoFileServer.class.getName());

    /**
     * 基本目录
     */
    URI baseDir;
    NanoFileUpload uploader;
    /**
     * 是否开启删除 false：关闭 true：开启
     */
    boolean is_del_flag;
    /**
     * 端口
     */
    private static final int SERVER_PORT = 12345;

    /**
     * 获取本地ip
     *
     * @return
     */
    public static String getIP() {
        String ip = "127.0.0.1";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
            return ip;
        } catch (UnknownHostException e) {
            LOG.warning("get ip error :" + e.getMessage());
        }
        return ip;
    }

    /**
     * 创建服务构造方法
     *
     * @param port
     * @param uri
     * @throws IOException
     */
    public NanoFileServer(int port, URI uri) throws IOException {
        super(port);
        baseDir = uri;
        uploader = new NanoFileUpload(new DiskFileItemFactory());
        LOG.info("开始运行:" + "共享目录为:" + baseDir + ",访问地址: http://" + getIP() + ":" + port + "/");
    }

    /**
     * 创建会话
     *
     * @param tempFileManager
     * @param inputStream
     * @param outputStream
     * @return
     */
    public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream,
                                     OutputStream outputStream) {
        return new HTTPSession(tempFileManager, inputStream, outputStream);
    }

    public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream,
                                     OutputStream outputStream, InetAddress inetAddress) {
        return new HTTPSession(tempFileManager, inputStream, outputStream, inetAddress);
    }

    /**
     * 服务主方法
     *
     * @param session
     * @return
     */
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        // 上传=====================================
        Map<String, List<FileItem>> files;
        Map<String, List<String>> decodedParamters;
        Map<String, List<String>> decodedParamtersFromParameter;
        String queryParameterString;
        // ==========================================
        if (uri.contains("/favicon.ico")) {
            return null;
        }
        StringBuffer msg = new StringBuffer();// 新的返回信息
        LOG.info("uri=" + uri + "|method=" + method + "|parms=" + parms);
        try {
            URI reqfile_uri = new URI(baseDir + "/" + uri);
            File reqFile = new File(reqfile_uri);
            if (method == Method.GET) { // 处理GET请求
                if (parms.containsKey("delete")) { // 处理删除文件操作；
                    return deal_delFile(session, parms, msg);
                } else if (parms.containsKey("getMD5")) { // 处理获取md5操作；
                    return deal_getMD5(session, parms, msg);
                } else if (parms.containsKey("makedir")) { //处理创建文件夹操作；
                    return deal_makeDir(session, parms, msg);
                } else if (parms.containsKey("ifdel")) { //处理是否开启删除操作
                    return deal_ifdel(session, parms, msg);
                } else {
                    if (reqFile.exists() && reqFile.isFile() && !FileUtils.isSymlink(reqFile)) {
                        return render200(session, reqFile);
                    } else if (reqFile.exists() && reqFile.isDirectory()) {
                        list_directory(reqFile, msg);
                        return render200(session, msg.toString());
                    } else if (reqFile.exists() && FileUtils.isSymlink(reqFile)) {
                        list_directory(reqFile, msg);
                        return render200(session, msg.toString());
                    } else {
                        return render404();
                    }
                }
            } else if (session.getMethod() == Method.POST && NanoFileUpload.isMultipartContent(session)) { // 处理POST请求
                List<FileItem> parseRequest = uploader.parseRequest(session);
                FileItem fileItem = parseRequest.get(0);
                String fileItemName = fileItem.getName();
                InputStream is = fileItem.getInputStream();
                File file = new File(new URI(reqFile.toURI() + "/" + fileItemName));
                while (file.exists()) {
                    file = new File(new URI(reqFile.toURI() + "/_" + file.getName()));
                }
                LOG.info("上传文件：" + fileItemName);
                FileOutputStream os = FileUtils.openOutputStream(file, false);
                Streams.copy(is, os, true);
                queryParameterString = session.getQueryParameterString();
                decodedParamtersFromParameter = decodeParameters(queryParameterString);
                decodedParamters = decodeParameters(session.getQueryParameterString());
                os.flush();
                os.close();
                is.close();
                list_directory(reqFile, msg);
                return render200(session, msg.toString());
            }
            return render404();
        } catch (Exception e) {
            return render500(e.getMessage());
        }
    }

    /**
     * 处理删除文件操作
     *
     * @param session
     * @param parms
     * @param msg
     * @return
     */
    private Response deal_ifdel(IHTTPSession session, Map<String, String> parms, StringBuffer msg) {
        try {
            URI requri = new URI(baseDir + "/" + session.getUri());
            File reqFile = new File(requri);
            is_del_flag = !is_del_flag;
            LOG.info("设置删除功能：" + is_del_flag);
            list_directory(reqFile, msg);
            return render200(session, msg.toString());
        } catch (Exception e) {
            LOG.warning(">>>>>>>>>" + e.getMessage());
            return render500("设置删除文件功能失败");
        }
    }

    public static void main(String[] args) {
        try {
			URI uri = new File("/").toURI();
//            URI uri = new File("").toURI();
            File path = new File(uri);
            if (!path.exists() || !path.isDirectory()) {
                LOG.warning("无法打开共享目录:" + path.getAbsolutePath());
                return;
            }
            NanoFileServer nanoFileServer = new NanoFileServer(SERVER_PORT, path.toURI());
            nanoFileServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            onApplicationRunning();
        } catch (Exception e) {
            LOG.warning("无法启动共享服务器" + e.getMessage());
        }
    }

    /**
     * 构建js脚本
     *
     * @param msg
     */
    public void do_buildJS(StringBuffer msg) {
        msg.append("<script type=\"text/javascript\">");
        msg.append("function callBackMD5(murl,domId){\r\n"
                + "http.get({url:murl,timeout:100000},function(err,result){document.getElementById(domId).innerHTML=result;});\r\n"
                + "}");
        msg.append("var http = {};\n");
        msg.append("http.quest = function (option, callback) {\n");
        msg.append("    var url = option.url;\n");
        msg.append("    var method = option.method;\n");
        msg.append("    var data = option.data;\n");
        msg.append("    var timeout = option.timeout || 0;\n");
        msg.append("    var xhr = new XMLHttpRequest();\n");
        msg.append("    (timeout > 0) && (xhr.timeout = timeout);\n");
        msg.append("    xhr.onreadystatechange = function () {\n");
        msg.append("        if (xhr.readyState == 4) {\n");
        msg.append("            if (xhr.status >= 200 && xhr.status < 400) {\n");
        msg.append("            var result = xhr.responseText;\n");
        msg.append("            try {result = JSON.parse(xhr.responseText);} catch (e) {}\n");
        msg.append("                callback && callback(null, result);\n");
        msg.append("            } else {\n");
        msg.append("                callback && callback('status: ' + xhr.status);\n");
        msg.append("            }\n");
        msg.append("        }\n");
        msg.append("    }.bind(this);\n");
        msg.append("    xhr.open(method, url, true);\n");
        msg.append("    if(typeof data === 'object'){\n");
        msg.append("        try{\n");
        msg.append("            data = JSON.stringify(data);\n");
        msg.append("        }catch(e){}\n");
        msg.append("    }\n");
        msg.append("    xhr.send(data);\n");
        msg.append("    xhr.ontimeout = function () {\n");
        msg.append("        callback && callback('timeout');\n");
        msg.append(
                "        console.log('%c连%c接%c超%c时', 'color:red', 'color:orange', 'color:purple', 'color:green');\n");
        msg.append("    };\n");
        msg.append("};\n");
        msg.append("http.get = function (url, callback) {\n");
        msg.append("    var option = url.url ? url : { url: url };\n");
        msg.append("    option.method = 'get';\n");
        msg.append("    this.quest(option, callback);\n");
        msg.append("};\n");
        msg.append("http.post = function (option, callback) {\n");
        msg.append("    option.method = 'post';\n");
        msg.append("    this.quest(option, callback);\n");
        msg.append("};\n");
        msg.append("</script>");
    }

    /**
     * 构建网页头
     *
     * @param msg
     * @param title
     */
    public void do_buildHeadMessage(StringBuffer msg, String title) {
        msg.append(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><html>\n<head><STYLE><!--H1 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525D76;font-size: 22px;}H2 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525D76;font-size: 16px;}H3 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525D76;font-size: 14px;}BODY {font-family: Tahoma, Arial, sans-serif;color: black;background-color: white;}B {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525D76;}P {font-family: Tahoma, Arial, sans-serif;background: white;color: black;font-size: 12px;}A {color: black;}A.name {color: black;}HR {color: #525D76;}--></STYLE>");
        msg.append("<title>" + title + "</title>\n");
        msg.append("<link rel=\"shortcut icon\" href=\"favicon.ico\" type=\"image/x-icon\">");
        do_buildJS(msg);
        msg.append("</head>\n");
    }

    private Response deal_delFile(IHTTPSession session, Map<String, String> parms, StringBuffer msg) {
        try {
            String delFileName = parms.get("delete");
            URI reqUri = new URI(baseDir + "/" + session.getUri());
            File reqFile = new File(reqUri);
            File tgFile = new File(new URI(reqFile.toURI() + "/" + delFileName));
            LOG.info("删除文件：" + tgFile.getAbsolutePath());
            LOG.info("删除文件状态：" + is_del_flag);
            if (is_del_flag) {
                FileUtils.deleteQuietly(tgFile);
                list_directory(reqFile, msg);
                return render200(session, msg.toString());
            } else {
                list_directory(reqFile, msg);
                return render200(session, msg.toString());
            }
        } catch (Exception e) {
            LOG.warning(">>>>>>>>" + e.getMessage());
            return render500("刪除文件失败:" + e.getMessage());
        }

    }

    private Response deal_getMD5(IHTTPSession session, Map<String, String> parms, StringBuffer msg) {
        String delFileName = parms.get("getMD5");
        try {
            URI requri = new URI(baseDir + "/" + session.getUri());
            File reqFile = new File(requri);
            File tgFile = new File(new URI(reqFile.toURI() + "/" + delFileName));
            LOG.info("获取MD5：" + tgFile.getAbsolutePath());
            FileInputStream data = new FileInputStream(tgFile);
            String md5Hex = DigestUtils.md5Hex(data.toString());
            data.close();
            return render200(session, "" + md5Hex);
        } catch (Exception e) {
            LOG.warning(">>>>>>>>" + e.getMessage());
            return render500("获取MD5失败:" + e.getMessage());
        }
    }

    private Response deal_makeDir(IHTTPSession session, Map<String, String> parms, StringBuffer msg) {
        try {
            String dirName = parms.get("makedir");
            URI requri = new URI(baseDir + "/" + session.getUri());
            File reqFile = new File(requri);
            File tgDir = new File(new URI(reqFile.toURI() + "/" + dirName));
            if (dirName == null || "".equals(dirName)) {
                LOG.info("创建目录，参数为空：" + tgDir.getAbsolutePath());
                list_directory(reqFile, msg);
                return render200(session, msg.toString());
            } else if (tgDir.exists() || tgDir.mkdirs()) {
                LOG.info("创建目录：" + tgDir.getAbsolutePath());
                list_directory(reqFile, msg);
                return render200(session, msg.toString());
            } else {
                return render500("创建目录失败.");
            }
        } catch (Exception e) {
            LOG.warning(">>>>>>>>>" + e.getMessage());
            return render500("创建目录失败:" + e.getMessage());
        }
    }


    private void list_directory(File showDir, StringBuffer html) throws IOException {

        String htmlStr = String.format("""
                	<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
                	<html lang="en">
                	<head>
                	    <meta charset="UTF-8">
                	    <STYLE>
                	    H1 {
                	        font-family: Tahoma, Arial, sans-serif;
                	        color: white;
                	        background-color: #525D76;
                	        font-size: 22px;
                	    }
                	    
                	    H2 {
                	        font-family: Tahoma, Arial, sans-serif;
                	        color: white;
                	        background-color: #525D76;
                	        font-size: 16px;
                	    }
                	    
                	    H3 {
                	        font-family: Tahoma, Arial, sans-serif;
                	        color: white;
                	        background-color: #525D76;
                	        font-size: 14px;
                	    }
                	    
                	    BODY {
                	        font-family: Tahoma, Arial, sans-serif;
                	        color: black;
                	        background-color: white;
                	    }
                	    
                	    B {
                	        font-family: Tahoma, Arial, sans-serif;
                	        color: white;
                	        background-color: #525D76;
                	    }
                	    
                	    P {
                	        font-family: Tahoma, Arial, sans-serif;
                	        background: white;
                	        color: black;
                	        font-size: 12px;
                	    }
                	    
                	    A {
                	        color: black;
                	    }
                	    
                	    A.name {
                	        color: black;
                	    }
                	    
                	    HR {
                	        color: #525D76;
                	    }
                	    
                	    </STYLE>
                	    <title>文件列表:%s</title>
                	    <link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
                	    <script type="text/javascript">
                         function callBackMD5(murl, domId) {
                                            console.log(murl,domId);

                                    http.get({url:murl,timeout:100000},
                                    function(err,result){
                                    document.getElementById(domId).innerHTML=result;
                                    });
                         };
                                    
                               function deleteFile(file) {
                                            console.log(file);
                                // 创建确认对话框
                                var confirmation = window.confirm("您确定要执行此操作吗？");
                                if (confirmation) {
                                // 用户点击了"确定"按钮
                                    http.get({url:file,timeout:100000},
                                    function(err,result){
                                    console.log('接口调用成功');
                                   window.location.reload();
                                     
                                    });
                                
                                }

                         };
                	    
                	    var http = {};
                	    http.quest = function (option, callback) {
                	        var url = option.url;
                	        var method = option.method;
                	        var data = option.data;
                	        var timeout = option.timeout || 0;
                	        var xhr = new XMLHttpRequest();
                	        (timeout > 0) && (xhr.timeout = timeout);
                	        xhr.onreadystatechange = function () {
                	            if (xhr.readyState == 4) {
                	                if (xhr.status >= 200 && xhr.status < 400) {
                	                    var result = xhr.responseText;
                	                    try {
                	                        result = JSON.parse(xhr.responseText);
                	                    } catch (e) {
                	                    }
                	                    callback && callback(null, result);
                	                } else {
                	                    callback && callback('status: ' + xhr.status);
                	                }
                	            }
                	        }.bind(this);
                	        xhr.open(method, url, true);
                	        if (typeof data === 'object') {
                	            try {
                	                data = JSON.stringify(data);
                	            } catch (e) {
                	            }
                	        }
                	        xhr.send(data);
                	        xhr.ontimeout = function () {
                	            callback && callback('timeout');
                	            console.log('连接超时');
                	        };
                	    };
                	    http.get = function (url, callback) {
                	        var option = url.url ? url : {url: url};
                	        option.method = 'get';
                	        this.quest(option, callback);
                	    };
                	    http.post = function (option, callback) {
                	        option.method = 'post';
                	        this.quest(option, callback);
                	    };
                	    </script>
                	</head>
                	<body>
                	<h1>文件列表:%s</h1>
                	<HR size="1" noshade="noshade">
                	<table>
                	    <tr>
                	        <td><input type="button" value="返回" onClick="javascript:history.back();"></td>
                	        <td>
                	            <form ENCTYPE="multipart/form-data" method="post">
                	                <input name="file" type="file"/>
                	                <input type="submit" value="上传"/>
                	            </form>
                	        </td>
                	        <td>
                	            <form method="get">
                	                <input type="text" name="makedir"/>
                	                <input type="submit" value="新建文件夹"/>
                	            </form>
                	        </td>
                	        <td>
                	            <form method="get">
                	                <input type="submit" value="开启/关闭删除"/>
                	                <input type="hidden" name="ifdel" readonly="readonly"/>
                	            </form>
                	        </td>
                	        <td><input type="button" value="返回首页" onClick="location='/'"></td>
                	    </tr>
                	</table>
                	<HR size="1" noshade="noshade">
                """, showDir.getAbsolutePath(), showDir.getAbsolutePath());

        html.append(htmlStr);
        html.append("""
                			<table border=0 width="100%" cellspacing="0" cellpadding="5" align="center"
                      style="overflow: scroll;word-break: keep-all">
                   <tr bgcolor="#00DB00">
                       <td width="5%">号码</td>
                       <td width="30%">文件名</td>
                       <td width="15%">文件大小</td>
                       <td width="20%">文件创建时间</td>
                       <td align="center">MD5</td>
                       <td width="10%">操作</td>
                   </tr>
                """);
        File[] list = showDir.listFiles();
        int idn = 1;
        if (list != null && list.length > 0) {
            for (File af : list) {
                String colorName;
                String linkName;
                String name = af.getName();
                colorName = linkName = name;
                String filesize = "";
                String lastmdf = "";
                if (af.isFile()) {
                    filesize = getFormatSize(FileUtils.sizeOf(af));
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    BasicFileAttributes attributes = Files.readAttributes(af.toPath(), BasicFileAttributes.class);
                    LocalDateTime fileCreationTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
                            ZoneId.systemDefault());
                    LocalDateTime fileLastModifiedTime = LocalDateTime
                            .ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                    lastmdf = "" + dateTimeFormatter.format(fileCreationTime);
                }
                if (idn % 2 == 0) {
                    html.append("<tr>");
                } else {
                    html.append("<tr bgcolor=\"#eeeeee\">");
                }
                // # 注意：指向目录的链接显示为 @，链接显示为 /
//				String emd5 = "<a id='a_" + idn + "' οnclick=\"callBackMD5('?getMD5=" + linkName + "','a_" + idn
//						+ "')\">MD5</a>";
                String emd5 = String.format("<a id=\"a_%s\"data-url=\"?getMD5=%s\" onClick=\"callBackMD5(this.dataset.url, 'a_%s')\">MD5</a>", idn, linkName, idn);
                if (af.isDirectory()) {
                    colorName = "<span style=\"background-color: #CEFFCE;\">" + name + "/</span>";
                    linkName = name + "/";
                    emd5 = "";
                } else if (FileUtils.isSymlink(af)) {
                    colorName = "<span>" + name + "@</span>";
                    linkName = name + "/";
                    emd5 = "";
                }
                String is_a;
                if (is_del_flag) {
//                    is_a = "<a style=\"background-color: #CEFFCE;\" href=\"?delete=" + linkName + "\">";
                    is_a = String.format("<a style=\"background-color: #CEFFCE;\" data-url=\"?delete=%s\" onClick=\"deleteFile(this.dataset.url)\">", linkName);
                } else {
                    is_a = "<a>";
                }
                html.append("<td>" + idn + "</td>" + "<td><a href=\"" + linkName + "\">" + colorName + "</a></td>"
                        + "<td>" + filesize + "</td>" + "<td>" + lastmdf + "</td>" + "<td align=\"center\">" + emd5
                        + "</td>" + "<td>" + is_a + "刪除</a></td>" + "</tr>\n");
                idn++;
            }
        }
        html.append(
                "</table>\n<HR size=\"1\" noshade=\"noshade\">\n<h2>Powered By lstar980@163.com</h2>\n</body>\n</html>\n");
    }

//	private void list_directory(File showDir, StringBuffer msg) throws IOException {
//		File[] list = showDir.listFiles();
//		do_buildHeadMessage(msg, "文件列表 " + showDir.getAbsolutePath());
//		msg.append("<body>\n<h1>文件列表 " + showDir.getAbsolutePath() + "</h1>\n");
//		msg.append("<HR size=\"1\" noshade=\"noshade\">\n");
//		msg.append("<table><tr><td>");
//		msg.append("<input type=\"button\" value=\"返回\" onClick=\"javascript:history.back();\"></td>");
//		msg.append("<td><form ENCTYPE=\"multipart/form-data\" method=\"post\">");
//		msg.append("<input name=\"file\" type=\"file\"/>");
//		msg.append("<input type=\"submit\" value=\"上传\"/>");
//		msg.append("</form></td>");
//		msg.append("<td><form method=\"get\">");
//		msg.append("<input type=\"text\" name=\"makedir\" />");
//		msg.append("<input type=\"submit\" value=\"新建文件夾\" />  ");
//		msg.append("</form></td>");
//		msg.append("<td><form method=\"get\">");
//		if (is_del_flag) {
//			msg.append("<input type=\"submit\" value=\"关闭删除\" />  ");
//			msg.append(
//					"<input type=\"hidden\" name=\"ifdel\" readonly=\"readonly\" ><font color=\"RED\">刪除功能已开启，请谨慎操作！！！</font></input>");
//		} else {
//			msg.append("<input type=\"submit\" value=\"开启删除\" />  ");
//			msg.append("<input type=\"hidden\" name=\"ifdel\" readonly=\"readonly\" />");
//		}
//		msg.append("</form></td><td><input type=\"button\" value=\"返回首页\" onClick=\"location='/'\"></td>");
//		msg.append("</tr></table>");
//		msg.append("<HR size=\"1\" noshade=\"noshade\">");
//		msg.append(
//				"<table border=0 width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" align=\"center\" style=\"overflow: scroll;word-break: keep-all\">"
//						+ "<tr bgcolor=\"#00DB00\">" + "<td width=\"5%\">号码</td>" + "<td width=\"30%\">文件名</td>"
//						+ "<td width=\"15%\">文件大小</td>" + "<td  width=\"20%\">文件创建时间</td>"
//						+ "<td align=\"center\">MD5</td>" + "<td width=\"10%\">操作</td>" + "</tr>");
//		int idn = 1;
//		if (list != null && list.length > 0) {
//			for (File af : list) {
//				String colorName;
//				String linkName;
//				String name = af.getName();
//				colorName = linkName = name;
//				String filesize = "";
//				String lastmdf = "";
//				if (af.isFile()) {
//					filesize = getFormatSize(FileUtils.sizeOf(af));
//					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
//					BasicFileAttributes attributes = Files.readAttributes(af.toPath(), BasicFileAttributes.class);
//					LocalDateTime fileCreationTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
//							ZoneId.systemDefault());
//					LocalDateTime fileLastModifiedTime = LocalDateTime
//							.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
//					lastmdf = "" + dateTimeFormatter.format(fileCreationTime);
//				}
//				if (idn % 2 == 0) {
//					msg.append("<tr>");
//				} else {
//					msg.append("<tr bgcolor=\"#eeeeee\">");
//				}
//				// # 注意：指向目录的链接显示为 @，链接显示为 /
//				String emd5 = "<a id='a_" + idn + "' οnclick=\"callBackMD5('?getMD5=" + linkName + "','a_" + idn
//						+ "')\">MD5</a>";
//				if (af.isDirectory()) {
//					colorName = "<span style=\"background-color: #CEFFCE;\">" + name + "/</span>";
//					linkName = name + "/";
//					emd5 = "";
//				} else if (FileUtils.isSymlink(af)) {
//					colorName = "<span>" + name + "@</span>";
//					linkName = name + "/";
//					emd5 = "";
//				}
//				String is_a;
//				if (is_del_flag) {
//					is_a = "<a style=\"background-color: #CEFFCE;\" href=\"?delete=" + linkName + "\">";
//				} else {
//					is_a = "<a>";
//				}
//				msg.append("<td>" + idn + "</td>" + "<td><a href=\"" + linkName + "\">" + colorName + "</a></td>"
//						+ "<td>" + filesize + "</td>" + "<td>" + lastmdf + "</td>" + "<td align=\"center\">" + emd5
//						+ "</td>" + "<td>" + is_a + "刪除</a></td>" + "</tr>\n");
//				idn++;
//			}
//		}
//		msg.append(
//				"</table>\n<HR size=\"1\" noshade=\"noshade\">\n<h2>Powered By star</h2>\n</body>\n</html>\n");
//	}

    /**
     * 获取文件大小
     *
     * @param size
     * @return
     */
    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "Byte";
        }
        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }
        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }
        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }


    /**
     * 自动打开浏览器访问项目地址
     */
    private static void onApplicationRunning() {
        String osName = System.getProperty("os.name", "");
        if (osName.contains("Windows")) {
            try {
                Runtime.getRuntime().exec("cmd /c start http://127.0.0.1:" + SERVER_PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 返回200
     *
     * @param session
     * @param reqFile
     * @return
     */
    private Response render200(IHTTPSession session, File reqFile) {
        try {
            return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(session.getUri()),
                    new FileInputStream(reqFile), reqFile.length());
        } catch (FileNotFoundException e) {
            LOG.warning(">>>>>" + e.getMessage());
            return render500(e.getMessage());
        }
    }

    private Response render200(IHTTPSession session, String htmlmsg) {
        LOG.info(">>>>>>>200");
        return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, htmlmsg);
    }

    private Response render404() {
        LOG.warning(">>>>>>>>>>>404");
        URI fileuri = null;
        try {
            fileuri = new URI(baseDir + "/" + "404.html");
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        File file = new File(fileuri);
        if (file.exists()) {
            try {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML,
                        new FileInputStream(file), file.length());
            } catch (FileNotFoundException e) {
                LOG.warning(">>>>>>>>>>" + e.getMessage());
                return render500(e.getMessage());
            }
        } else {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "404");
        }
    }

    private Response render301(String next) {
        Response res = newFixedLengthResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, null);
        res.addHeader("Location", next);
        LOG.warning(">>>>>>>>>>301" + next);
        return res;
    }

    private Response render500(String errmsg) {
        LOG.warning(">>>>>>>>>>>>500," + errmsg);
        return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, errmsg);
    }
}