package org.nutz.boot.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.nutz.json.Json;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

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
    
    @Parameter(property = "repo.token", defaultValue = "", required = false)
    protected String repoToken;
    
    public String readRepoToken() {
        if (!Strings.isBlank(repoToken))
            return repoToken.trim();
        File authJson = new File(System.getProperty("user.home") + "/.nutzboot/repo/auth.json");
        if (!authJson.exists())
            return null;
        NutMap map = Json.fromJsonFile(NutMap.class, authJson);
        if (map.containsKey(repoUrl)) {
            map = map.getAs(repoUrl, NutMap.class);
        }
        if (map.containsKey(repoUser)) {
            map = map.getAs(repoUser, NutMap.class);
        }
        if (map.containsKey(repoAppName)) {
            map = map.getAs(repoAppName, NutMap.class);
        }
        return map.getString("token");
    }
}
