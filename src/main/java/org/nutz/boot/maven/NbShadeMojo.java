package org.nutz.boot.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.shade.mojo.ShadeMojo;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.AppendingTransformer;
import org.apache.maven.plugins.shade.resource.ManifestResourceTransformer;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.plugins.shade.resource.ServicesResourceTransformer;
import org.apache.maven.project.MavenProject;
import org.nutz.lang.Encoding;
import org.nutz.lang.Mirror;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;

@Mojo(name = "shade", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class NbShadeMojo extends ShadeMojo {

    @Parameter(required = false, property = "nutzboot.mainClass")
    private String mainClass;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    protected File target;
    
    @Parameter(required = false, property = "nutzboot.dst")
    protected File dst;
    
    @Parameter(required=false)
    private boolean compression = true;

    protected Field transformersField;
    
    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    protected MavenProject project2;

    public NbShadeMojo() throws Exception {
        transformersField = ShadeMojo.class.getDeclaredField("transformers");
        transformersField.setAccessible(true);
    }

    public void execute() throws MojoExecutionException {
        if ("pom".equals(project2.getPackaging()))
            return;
        if (Strings.isBlank(mainClass)) {
            mainClass = AbstractNbMojo.searchMainClass(target, getLog());
        }
        if (Strings.isBlank(mainClass)) {
            getLog().info("MainLaucher not found, skip this action!!!");
            return;
        }
        // 设置transformers
        try {
            ResourceTransformer[] transformers = (ResourceTransformer[]) transformersField.get(this);
            if (transformers == null) {
                transformers = new ResourceTransformer[0];
            }
            List<ResourceTransformer> transformers2 = new ArrayList<>();
            boolean hasServicesResourceTransformer = false;
            boolean hasManifestResourceTransformer = false;
            for (ResourceTransformer rt : transformers) {
                if (rt instanceof ServicesResourceTransformer) {
                    hasServicesResourceTransformer = true;
                }
                if (rt instanceof ManifestResourceTransformer) {
                    hasManifestResourceTransformer = true;
                }
                transformers2.add(rt);
            }
            // 转换META-INF/service/** 文件
            if (!hasServicesResourceTransformer) {
                transformers2.add(new ServicesResourceTransformer());
            }
            // 添加build.version
            if (!hasManifestResourceTransformer) {
                ManifestResourceTransformer rt = new ManifestResourceTransformer() {
                    @Override
                    public void modifyOutputStream(JarOutputStream jos) throws IOException {
                        super.modifyOutputStream(jos);
                        try {
                            JarEntry en = new JarEntry("build.version");
                            jos.putNextEntry(en);
                            StringBuilder sb = new StringBuilder();
                            sb.append("app.build.version=").append(project2.getVersion()).append("\r\n");
                            sb.append("app.build.groupId=").append(project2.getGroupId()).append("\r\n");
                            sb.append("app.build.artifactId=").append(project2.getArtifactId()).append("\r\n");
                            sb.append("buildNumber=").append("_"); // TODO 读取VCS的版本号
                            jos.write(sb.toString().getBytes());
                            jos.closeEntry();
                        }
                        catch (Throwable e) {
                        }
                    }
                };
                if (Strings.isBlank(mainClass)) {
                    mainClass = AbstractNbMojo.searchMainClass(target, getLog());
                }
                Mirror.me(ManifestResourceTransformer.class).setValue(rt, "mainClass", mainClass);
                transformers2.add(rt);
            }
            // 转换NbStater文件
            AppendingTransformer at = new AppendingTransformer();
            Mirror.me(AppendingTransformer.class).setValue(at, "resource", "META-INF/nutz/org.nutz.boot.starter.NbStarter");
            transformers2.add(at);
            // 转换CXF的BUS扩展文件
            at = new AppendingTransformer();
            Mirror.me(AppendingTransformer.class).setValue(at, "resource", "META-INF/cxf/bus-extensions.txt");
            transformers2.add(at);
            // 转换Spring的各种扩展文件
            at = new AppendingTransformer();
            Mirror.me(AppendingTransformer.class).setValue(at, "resource", "META-INF/spring.handlers");
            transformers2.add(at);
            at = new AppendingTransformer();
            Mirror.me(AppendingTransformer.class).setValue(at, "resource", "META-INF/spring.schemas");
            transformers2.add(at);
            // 转换dubbo的filters文件
            at = new AppendingTransformer();
            Mirror.me(AppendingTransformer.class).setValue(at, "resource", "META-INF/dubbo/com.alibaba.dubbo.rpc.Filter");
            transformers2.add(at);
            
            // 过滤签名文件
            transformers2.add(new ResourceTransformer() {
                public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
                    getLog().info("Remove " + resource);
                }
                public void modifyOutputStream(JarOutputStream os) throws IOException {
                }
                public boolean hasTransformedResource() {
                    return false;
                }
                public boolean canTransformResource(String resource) {
                    if (resource.startsWith("META-INF")) {
                        // 删除签名文件
                        if (resource.endsWith(".SF") || resource.endsWith(".DSA") || resource.endsWith(".RSA"))
                            return true;
                        // 删除NOTICE文件
                        if (resource.startsWith("META-INF/NOTICE"))
                            return true;
                        // 删除LICENSE文件
                        if (resource.startsWith("META-INF/LICENSE"))
                            return true;
                    }
                    if (resource.startsWith("rest-management-private-classpath/"))
                        return true;
                    if (resource.equals("build.version"))
                        return true;
                    return false;
                }
            });

            // 设置到超类中
            transformersField.set(this, transformers2.toArray(new ResourceTransformer[transformers2.size()]));
        }
        catch (Throwable e) {
            throw new MojoExecutionException("fail to get/set transformers", e);
        }
        if (dst != null) {
            // final String shadedName = shadedArtifactId + "-" + artifact.getVersion() + "-" + shadedClassifierName + "." + artifact.getArtifactHandler().getExtension()
            //Mirror.me(this).setValue(this, "outputDirectory", dst);
            Mirror.me(this).setValue(this, "outputFile", new File(dst, project2.getArtifactId() + "-" + project2.getVersion() + ".jar"));
        }
        super.execute();
        if (!compression) {
            getLog().info("making uncompress jar ...");
            File mainZip = project2.getArtifact().getFile();
            long time = System.currentTimeMillis();
            FileTime t2 = FileTime.fromMillis(time);
            File tmpZip = new File(mainZip + ".zip");
            try (ZipInputStream ins = new ZipInputStream(new FileInputStream(mainZip), Encoding.CHARSET_UTF8);
                    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmpZip), Encoding.CHARSET_UTF8)) {
                out.setLevel(ZipOutputStream.STORED);
                ZipEntry en = null;
                ZipEntry en2 = null;
                while ((en = ins.getNextEntry()) != null) {
                    en2 = new ZipEntry(en.getName());
                    en2.setLastModifiedTime(t2);
                    en2.setTime(time);
                    en2.setCreationTime(t2);
                    en2.setLastAccessTime(t2);
                    out.putNextEntry(en2);
                    if (!en.isDirectory()) {
                        Streams.write(out, ins);
                    }
                    out.closeEntry();
                }
                out.flush();
            }
            catch (Exception e) {
                throw new MojoExecutionException("error when doing unzip", e);
            }
            getLog().info("replace origin jar ...");
            mainZip.renameTo(new File(mainZip.getParent(), "shade-" + mainZip.getName()));
            tmpZip.renameTo(mainZip);
        }
    }

}
