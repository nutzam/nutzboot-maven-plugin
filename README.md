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
