package com.shin.graalvm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @create 2023-09
 * @author lstar
 */
@Controller
@CrossOrigin
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Home";
    }

    @ResponseBody
    @GetMapping("/hello/{name}")
    public Map home(@PathVariable String name) {
        return new HashMap<String, Object>() {{
            put("code", 0);
            put("message", "Hello " + name);
        }};
    }
    @ResponseBody
    @GetMapping("/info/graalvmInfo")
    public String graalvmInfo() {
        return """
                一、GraalVM是什么？
                GraalVM是 Oracle 开源的一款通用虚拟机产品，官方称之为 Universal GraalVM，是新一代的通用多语言高性能虚拟机。它可以运行多种编程语言，如Java、JavaScript、Python等，并提供了即时编译（JIT）和AOT编译（AOT）的支持。GraalVM还支持在不同语言之间互相调用，以及嵌入到其他应用程序中使用。这使得GraalVM成为一个非常强大和灵活的工具，可以满足不同应用场景的需求。\n
                                
                                
                                
                二、GraalVM有哪些特点？
                2.1、高性能
                GraalVM的即时编译器采用了新型的JIT技术，能够在运行时对代码进行优化，从而提高应用程序的性能。同时，GraalVM还支持AOT编译，能够将代码预先编译成本地机器代码，进一步提高应用程序的运行速度。\n
                                
                2.2、多语言支持
                GraalVM支持多种编程语言，如Java、JavaScript、Python等，甚至还支持R、Ruby、C等语言。这意味着开发者可以使用他们最喜欢的语言来开发应用程序，同时还可以将不同语言的代码集成在一起。\n
                                
                2.3、互操作性
                GraalVM允许不同语言之间互相调用，甚至可以将代码嵌入到其他应用程序中。这使得开发者可以选择最适合他们需求的编程语言，并且不需要为了使用不同的语言而进行大量的代码转换。\n
                                
                2.4、安全性
                GraalVM的安全性是由于其特有的“沙箱”机制。这个机制允许开发者在GraalVM上运行不受信任的代码，而不会影响到系统的其他部分。这种机制可以防止恶意代码的传播，从而保证系统的安全性。\n
                                
                三、GraalVM的应用效果
                GraalVM的应用效果非常显著，尤其在大规模、复杂的应用程序中。以下是GraalVM的一些应用效果：\n
                                
                3.1、提高性能
                GraalVM的即时编译器和AOT编译器可以显著提高应用程序的性能。据测试，GraalVM的性能可以比传统的JVM高出20%-100%。\n
                                
                3.2、简化开发
                GraalVM的多语言支持和互操作性可以大大简化开发工作。开发者可以使用他们最熟悉的语言来开发应用程序，并且不需要进行大量的代码转换和集成工作。\n
                                
                3.3、降低成本
                GraalVM的性能提升和简化开发的特性，可以帮助企业在开发过程中减少人力资源和时间成本，同时提高开发效率。\n
                                
                3.4、节省资源
                GraalVM采用了高效的内存管理技术，可以在运行时动态调整内存大小。这意味着应用程序可以更好地利用系统资源，从而提高整体性能。\n
                  
                                
                3.5、支持云环境
                GraalVM的云支持使得它可以在云环境中运行，这意味着开发者可以将应用程序轻松迁移到云上，并且能够利用云环境的弹性扩展和负载均衡等特性。
                """;
    }
}
