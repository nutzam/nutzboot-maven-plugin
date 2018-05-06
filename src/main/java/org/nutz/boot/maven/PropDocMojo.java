package org.nutz.boot.maven;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.IOUtil;
import org.nutz.boot.tools.PropDocBean;
import org.nutz.boot.tools.PropDocReader;
import org.nutz.lang.Files;
import org.nutz.lang.Strings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

@Mojo(name="propdoc")
public class PropDocMojo extends AbstractNbMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        File dependencyDir = new File(target, "dependency");
        if (!dependencyDir.exists()) {
            log.warn("Please run dependency:copy-dependencies first.");
            return;
        }
        PropDocReader docReader = new PropDocReader();
        for (File file : dependencyDir.listFiles()) {
            try (ZipFile zipFile = new ZipFile(file);) {
                ZipEntry markEntry = zipFile.getEntry("META-INF/nutz/org.nutz.boot.starter.NbStarter");
                if (markEntry == null)
                    continue;
                byte[] buf = IOUtil.toByteArray(zipFile.getInputStream(markEntry));
                for (String _line : new String(buf).split("\n")) {
                    String klassName = _line.trim();
                    if (debug)
                        log.info("Found " + klassName);
                    ZipEntry starterEntry = zipFile.getEntry(klassName.replace('.', '/') + ".class");
                    if (starterEntry == null) {
                        if (debug)
                            log.info("Not such class file in jar, skip it. " + klassName + " " + file.getName());
                        continue;
                    }
                    try (InputStream ins = zipFile.getInputStream(starterEntry)) {
                        ClassReader cr = new ClassReader(ins);
                        ClassNode nodes = new ClassNode();
                        cr.accept(nodes, ClassReader.SKIP_CODE);
                        for (Object tmp : nodes.fields) {
                            FieldNode field = (FieldNode)tmp;
                            if (!field.name.startsWith("PROP_")) {
                                continue;
                            }
                            if (field.value == null) {
                                continue;
                            }
                            List<AnnotationNode> annos = field.visibleAnnotations;
                            for (Object tmp2 : annos) {
                                AnnotationNode anno = (AnnotationNode)tmp2;
                                if (anno.desc.contains("PropDoc")) {
                                    PropDocBean doc = new PropDocBean();
                                    doc.key = (String) field.value;
                                    doc.users = new ArrayList<>();
                                    for (int i = 0; i < anno.values.size(); i+=2) {
                                        String key = (String) anno.values.get(i);
                                        Object value = anno.values.get(i+1);
                                        switch (key) {
                                        case "value":
                                            doc.value = String.valueOf(value);
                                            break;
                                        case "group":
                                            doc.group = Strings.isBlank((String)value) ? doc.key.substring(0, doc.key.indexOf('.')) : (String)value;
                                            break;
                                        case "need":
                                            doc.need = (Boolean)value;
                                            break;
                                        case "defaultValue":
                                            doc.defaultValue = (String)value;
                                            break;
                                        case "possible":
                                            doc.possible = (String[])value;
                                            break;
                                        }
                                    }
                                    doc.defaultValue = Strings.sBlank(doc.defaultValue);
                                    docReader.add(klassName, doc.key, doc);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                log.info("bad jar?" + file.getAbsolutePath(), e);
            }
        }
        log.info("Configure Manual:\r\n" + docReader.toMarkdown());
        Files.write(new File(target, "configure.md"), docReader.toMarkdown());
    }

}
