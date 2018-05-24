package org.nutz.boot.maven;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
            if (project == null)
                throw new MojoFailureException("require repo.file or pom.xml!!!");
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

        ProcessBuilder processBuilder = new ProcessBuilder("scp",
                                                           file.getPath(),
                                                           String.format("%s@%s:%s/%s",
                                                                         repoSshUser,
                                                                         repoSshServer,
                                                                         uploadpath,
                                                                         file.getName()));

        if (!Strings.isEmpty(repoSshKeypath)) {
            processBuilder.command().add(1, "-i");
            processBuilder.command().add(2, repoSshKeypath);
        }
        try {
            processBuilder.start();
        }
        catch (IOException e) {
            throw new MojoFailureException("ssh upload has some problem, check about ssh params PLZ");
        }
    }
}
