package org.nutz.boot.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.nutz.lang.Strings;

@Mojo(name = "repo-ssh-upload")
public class RepoSshUploadMojo extends AbstractRepoMojo {

    @Parameter(property = "repo.ssh.user")
    private String repoSshUser;

    @Parameter(property = "repo.ssh.server")
    private String repoSshServer;

    @Parameter(property = "repo.ssh.uploadpath")
    private String repoSshUploadpath;

    @Parameter(property = "repo.ssh.keypath")
    private String repoSshKeypath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new ProcessBuilder("which", "scp").start();
        }
        catch (IOException e) {
            throw new MojoFailureException("repo-ssh-upload task just run in *uix now!!!");
        }

        if (file == null) {
            if (project == null) {
                throw new MojoFailureException("require repo.file or pom.xml!!!");
            }
            file = project.getArtifact().getFile();
        }
        if (!file.exists()) {
            throw new MojoFailureException("file not exist!!! " + file);
        }
        if (Strings.isEmpty(repoSshUser)) {
            throw new MojoFailureException("require repo.ssh.user!!!");
        }
        if (Strings.isEmpty(repoSshServer)) {
            throw new MojoFailureException("require repo.ssh.server!!!");
        }
        if (Strings.isEmpty(repoSshUploadpath)) {
            throw new MojoFailureException("require repo.ssh.uploadpath!!!");
        }

        String uploadpath = String.join("/", repoSshUploadpath.split("/"));

        String uploadFileName = file.getName();
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
        Artifact artifact = attachedArtifacts.get(attachedArtifacts.size() - 1);
        if (null != artifact && "war".equals(artifact.getClassifier())) {
            uploadFileName = artifact.getFile().getName();
        }

        ProcessBuilder processBuilder = new ProcessBuilder("scp",
                                                           file.getPath(),
                                                           String.format("%s@%s:%s/%s",
                                                                         repoSshUser,
                                                                         repoSshServer,
                                                                         uploadpath,
                                                                         uploadFileName));

        if (!Strings.isEmpty(repoSshKeypath)) {
            processBuilder.command().add(1, "-i");
            processBuilder.command().add(2, repoSshKeypath);
        }

        Log log = getLog();
        log.info("upload file " + uploadFileName + " to path " + uploadpath);
        try {
            Process process = processBuilder.start();
            InputStream errorStream = process.getErrorStream();
            if (errorStream != null) {
                new BufferedReader(new InputStreamReader(errorStream)).lines()
                                                                      .forEach(error -> log.error(error));
                throw new MojoFailureException("repo-ssh-upload task fail");
            }
        }
        catch (IOException e) {
            log.error("repo-ssh-upload task fail", e);
            throw new MojoFailureException("ssh upload has some problem, check about ssh params PLZ");
        }
    }
}
