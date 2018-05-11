package org.nutz.boot.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractRepoMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    protected MavenProject project;
    
    @Parameter(property = "repo.file", required = false)
    protected File file;

    @Parameter(property = "repo.url", defaultValue = "http://127.0.0.1:8080/repo", required = false)
    protected String repoUrl;

    @Parameter(property = "repo.user", defaultValue = "demo", required = false)
    protected String repoUser;

    @Parameter(property = "repo.app.name", defaultValue = "${project.model.artifactId}", required = false)
    protected String repoAppName;

    @Parameter(property = "repo.app.version", defaultValue = "${project.model.version}", required = false)
    protected String repoAppVersion;
}
