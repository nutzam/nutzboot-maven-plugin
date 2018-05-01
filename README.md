# nutzboot-maven-plugin

NutzBoot的Maven插件

## 用法简介

### 在build-plugins添加本plugin

```xml
    <build>
        <plugins>
            <plugin>
                 <groupId>org.nutz.boot</groupId>
                 <artifactId>nutzboot-maven-plugin</artifactId>
                 <version>${nutzboot.version}</version>
            </plugin>
         </plugins>
    </build>
```

### 打包jar文件

```shell
mvn clean package nutzboot:shade
```

### 直接运行

```shell
mvn compile nutzboot:run
```

### 输出配置文档

```shell
mvn dependency:copy-dependencies nutzboot:propdoc
```

## mainClass探测规则

默认情况下,按以下规则查找mainClass

- 带有public static main 方法
- 在main方法内,引用或使用到NbApp类

例如:

可探测到

```java
public class MainLauncher {
    public static void main(String[] args) {
        new NbApp().run();
        // NbApp app = new NbApp(); 这样也可以
        // ....
        // app.run();
    }
}
```

不可探测到

```java
public class MainLauncher {
    public static void main(String[] args) {
        abc();
    }
    public static void abc() {
        new NbApp().run();
    }
}
```

## 待开发的功能

- [ ] war命令 将nutzboot:shade生成的jar进一步加工成war文件,供传统模式下的部署
- [ ] init 项目初始化命令,根据一个远程/本地模板生成项目
- [ ] upload 将jar上传到部署服务器
- [ ] download 从部署服务器下载jar
- [ ] repo-search 搜索部署服务器
