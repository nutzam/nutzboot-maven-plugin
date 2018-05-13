package org.nutz.boot.maven;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.nutz.http.Request;
import org.nutz.http.Request.METHOD;
import org.nutz.http.Response;
import org.nutz.http.Sender;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

@Mojo(name = "repo-upload")
public class RepoUploadMojo extends AbstractRepoMojo {


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (file == null) {
            if (project == null)
                throw new MojoFailureException("require repo.file or pom.xml!!!");
            file = project.getArtifact().getFile();
        }
        if (!file.exists()) {
            throw new MojoFailureException("file not exist!!! " + file);
        }
        try (InputStream ins = new FileInputStream(file)) {
            String url = repoUrl + "/" + repoUser + "/" + repoAppName + "/" + repoAppVersion + "/?fileName=" + URLEncoder.encode(file.getName(), "UTF-8");
            getLog().info("Upload URL=" + url);
            Request request = Request.create(url, METHOD.POST);
            request.setParams(new NutMap("fileName", file.getName()));
            request.getHeader().set("Content-Type", "application/octet-stream");
            request.getHeader().set("Content-Length", ""+file.length());
            request.getHeader().set("Connection", "close");
            String token = readRepoToken();
            if (!Strings.isBlank(token)) {
                request.getHeader().set("Repo-Token", token);
            }
            request.setInputStream(ins);
            getLog().info("Uploading... size=" + file.length());
            Response resp = Sender.create(request).send();
            if (resp.isOK()) {
                getLog().info("Upload Complete");
            } else {
                getLog().info("Upload FAIL!!! respCode=" + resp.getStatus() + "\r\n" + resp.getContent());
                throw new MojoFailureException("respCode=" + resp.getStatus());
            }
        }
        catch (IOException e) {
            getLog().info("Upload FAIL!!!", e);
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
