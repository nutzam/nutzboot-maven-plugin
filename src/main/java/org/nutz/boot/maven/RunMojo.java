package org.nutz.boot.maven;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.mojo.exec.ExecJavaMojo;
import org.nutz.lang.Strings;

@Mojo(name="run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class RunMojo extends ExecJavaMojo {
    
    @Parameter( required = false, property = "exec.mainClass" )
    private String mainClass;
    
    @Parameter( defaultValue = "${project.build.directory}", readonly = true )
    protected File target;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (Strings.isBlank(mainClass)) {
            mainClass = AbstractNbMojo.searchMainClass(target, log);
        }
        if (!Strings.isBlank(mainClass)) {
            try {
                Field field = ExecJavaMojo.class.getDeclaredField("mainClass");
                field.setAccessible(true);
                field.set(this, mainClass);
            }
            catch (Exception e) {
                log.error("bad bad bad", e);
                throw new MojoFailureException("bad bad bad", e);
            }
        }
        super.execute();
    }

}
