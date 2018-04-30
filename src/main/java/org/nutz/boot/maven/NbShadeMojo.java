package org.nutz.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;

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
import org.nutz.lang.Mirror;
import org.nutz.lang.Strings;

@Mojo(name = "shade", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NbShadeMojo extends ShadeMojo {

    @Parameter(required = false, property = "nutzboot.mainClass")
    private String mainClass;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    protected File target;

    protected Field transformersField;

    public NbShadeMojo() throws Exception {
        transformersField = ShadeMojo.class.getDeclaredField("transformers");
        transformersField.setAccessible(true);
    }

    public void execute() throws MojoExecutionException {
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
            // TODO 转换CXF的META-INF/cxf/bus-extensions.txt
            if (!hasManifestResourceTransformer) {
                ManifestResourceTransformer rt = new ManifestResourceTransformer();
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
                    return false;
                }
            });

            // 设置到超类中
            transformersField.set(this, transformers2.toArray(new ResourceTransformer[transformers2.size()]));
        }
        catch (Throwable e) {
            throw new MojoExecutionException("fail to get/set transformers", e);
        }
        super.execute();
    }

}
