package org.nutz.boot.maven;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.nutz.lang.util.Disks;
import org.nutz.lang.util.FileVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public abstract class AbstractNbMojo extends AbstractMojo {

    @Parameter( property = "nutzboot.mainClass", defaultValue = "" )
    protected String mainClass;
 
    @Parameter( defaultValue = "${project.basedir}", readonly = true )
    protected File basedir;
 
    @Parameter( defaultValue = "${project.build.directory}", readonly = true )
    protected File target;
    
    @Parameter( property = "nutzboot.maven.debug", defaultValue = "false" )
    protected boolean debug;
    
    public static String searchMainClass(File target, Log log) {
        List<String> possibleMainClasses = new ArrayList<>();
        Disks.visitFile(target, new FileVisitor() {
            public void visit(File file) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ClassReader cr = new ClassReader(fis);
                    ClassNode node = new ClassNode();
                    cr.accept(node, ClassReader.EXPAND_FRAMES);
                    for (MethodNode method : node.methods) {
                        if (!method.name.equals("main"))
                            continue;
                        if (Modifier.isPublic(method.access) && Modifier.isStatic(method.access)) {
                            for (AbstractInsnNode insn : method.instructions.toArray()) {
                                if (insn instanceof TypeInsnNode) {
                                    TypeInsnNode type = (TypeInsnNode)insn;
                                    if ("org/nutz/boot/NbApp".equals(type.desc)) {
                                        possibleMainClasses.add(node.name.replace('/', '.'));
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Throwable e) {
                    log.info("bad class file? "  + file.getAbsolutePath(), e);
                }
            }
        }, new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || (f.getName().endsWith(".class") && !f.getName().contains("$"));
            }
        });
        if (possibleMainClasses.size() > 0) {
            return possibleMainClasses.get(0);
        }
        return null;
    }
}
