package com.star.graalvm;

import java.util.Scanner;

/**
 * @create 2023-09
 * @author lstar
 */
public class AppStart {
    public static void main(String[] args) {
        System.out.println("Hello GraalVM ! \n \n请输入q退出程序!");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            System.out.println(line);

            if ("q".equals(line)) {
                System.out.println("Bye");
                System.exit(0);
            }
        }
    }
}
