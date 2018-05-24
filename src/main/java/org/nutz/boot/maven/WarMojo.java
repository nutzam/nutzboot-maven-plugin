package org.nutz.boot.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.nutz.lang.Encoding;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;

@Mojo(name = "war")
public class WarMojo extends AbstractNbMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Strings.isBlank(mainClass)) {
            mainClass = AbstractNbMojo.searchMainClass(target, getLog());
        }
        File jarFile = project.getArtifact().getFile();
        org.apache.maven.plugin.logging.Log log = getLog();
        log.info("Convert " + jarFile.getName());
        try {
            File dstFile = new File(jarFile.getParentFile(), project.getArtifactId() + ".war");
            try (ZipInputStream sourceZip = new ZipInputStream(new FileInputStream(jarFile));
                    ZipOutputStream dstZip = new ZipOutputStream(new FileOutputStream(dstFile), Encoding.CHARSET_UTF8)) {
                ZipEntry sourceEn = null;
                String webXml = null;
                String nbStarterMark = "";
                OUT: while ((sourceEn = sourceZip.getNextEntry()) != null) {
                    String sourceName = sourceEn.getName();
                    String targetName = sourceName;
                    if (sourceName.startsWith("static/") || sourceName.startsWith("webapp/")) {
                        if (sourceName.equals("static/") || sourceName.equals("webapp/")) {
                            continue;// 因为只是个目录,跳过即可
                        } else {
                            targetName = sourceName.substring("static/".length());
                            if ("WEB-INF/web.xml".equalsIgnoreCase(targetName)) {
                                log.info("found web.xml, need rewrite");
                                webXml = new String(Streams.readBytes(sourceZip), "UTF-8");
                                continue;
                            }
                        }
                    } else {
                        // 需要跳过web容器的类, jetty/tomcat/undertow的package
                        for (String pkg : Arrays.asList("org/eclipse/jetty/server",
                                                        "org/eclipse/jetty/webapp",
                                                        "io/undertow",
                                                        "org/apache/catalina",
                                                        "javax/servlet",
                                                        "javax/websocket",
                                                        "META-INF/services/javax.servlet",
                                                        "META-INF/services/javax.websocket",
                                                        "org/nutz/boot/starter/jetty",
                                                        "org/nutz/boot/starter/tomcat",
                                                        "org/nutz/boot/starter/undertow")) {
                            if (sourceName.startsWith(pkg)) {
                                continue OUT;
                            }
                        }
                        // 修正org.nutz.boot.starter.NbStarter标记文件
                        if ("META-INF/nutz/org.nutz.boot.starter.NbStarter".equals(sourceName)) {
                            nbStarterMark = new String(Streams.readBytes(sourceZip), Encoding.CHARSET_UTF8);
                            nbStarterMark = nbStarterMark.replace("org.nutz.boot.starter.jetty.JettyStarter", "");
                            nbStarterMark = nbStarterMark.replace("org.nutz.boot.starter.tomcat.TomcatStarter", "");
                            nbStarterMark = nbStarterMark.replace("org.nutz.boot.starter.undertow.UndertowStarter", "");
                            continue;
                        }
                        targetName = "WEB-INF/classes/" + sourceName;
                    }
                    dstZip.putNextEntry(new ZipEntry(targetName));
                    Streams.write(dstZip, sourceZip);
                    dstZip.closeEntry();
                }
                webXml = createOrRewriteWebXml(webXml, mainClass);
                dstZip.putNextEntry(new ZipEntry("WEB-INF/web.xml"));
                dstZip.write(webXml.getBytes(Encoding.CHARSET_UTF8));
                dstZip.closeEntry();
                // 最后,写入web.xml
                dstZip.finish();
                dstZip.flush();
                AttachedArtifact artifact = new AttachedArtifact(project.getArtifact(), "", "war", null);
                artifact.setFile(dstFile);
                project.addAttachedArtifact(artifact);
            }
        }
        catch (Exception e) {
            log.error("convert fail", e);
        }
    }

    protected String createOrRewriteWebXml(String sourceWebXml, String mainClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("<listener><listener-class>org.nutz.boot.starter.servlet3.NbServletContextListener</listener-class></listener>\r\n");
        sb.append("<context-param><param-name>nutzboot.mainClass</param-name><param-value>").append(mainClass).append("</param-value></context-param>");
        if (Strings.isBlank(sourceWebXml)) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<web-app>\r\n    " + sb + "\r\n</web-app>";
        } else {
            int index = sourceWebXml.indexOf('>', sourceWebXml.indexOf("<web-app" + 8)) + 1;
            return sourceWebXml.substring(0, index) + sb + sourceWebXml.substring(index);
        }
    }
}
