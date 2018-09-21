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

打包完成后的jar文件可以直接运行

```
java -jar XXX.jar
```

若需要设置jvm内存大小等参数:

```
java -Xmx512m -Xms512m -jar XXX.jar
```

多模块打包并输出到指定目录
```
// 在项目根目录执行
mvn -Dnutzboot.dst=E:/dst clean package nutzboot:shade
// 会在E:/dst目录生成多个子模块的可运行jar
```

### 直接运行

```shell
mvn compile nutzboot:run
```

### 输出配置文档

```shell
mvn dependency:copy-dependencies nutzboot:propdoc
```

会打印在控制台,并写入 target/configure.md 文件

### 打包成war

将nutzboot:shade生成的jar进一步加工成war文件,供传统模式下的部署

```
mvn clean package nutzboot:shade nutzboot:war
```

提醒, war模式下, 有部分限制:

- jetty/tomcat/undertow的配置项自然会失效,它们对应的starter也会自动移除
- server.port 若使用RPC相关的功能,应该修改成web容器的端口号

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

- [ ] init 项目初始化命令,根据一个远程/本地模板生成项目
- [ ] repo-upload 将jar上传到部署服务器
- [ ] repo-download 从部署服务器下载jar
- [ ] repo-search 搜索部署服务器

## 如需使用快照版的,请在pom.xml中加入

```xml
	<pluginRepositories>
		<pluginRepository>
			<id>nutz-snapshots</id>
			<url>http://jfrog.nutz.cn/artifactory/snapshots</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
			<releases>
				<enabled>false</enabled>
			</releases>
		</pluginRepository>
	</pluginRepositories>
```