package com.github.chmodas.mojo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.net.URI;

public abstract class AbstractDockerMojo extends AbstractMojo {
    /**
     * The current Maven project.
     */
    @Component
    protected MavenProject project;

    /**
     * The URL to Docker server.
     */
    @Parameter(property = "docker.url", defaultValue = "http://localhost:4243")
    protected URI url;

    /**
     * Docker API version to use, the default is provided by docker-java.
     */
    @Parameter(property = "docker.version")
    protected String version;

    /**
     * Skip execution.
     */
    @Parameter(property = "docker.skip", defaultValue = "false")
    private Boolean skip;

    /**
     * Plugin entrypoint.
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public void execute() throws MojoFailureException, MojoExecutionException {
        if (skip) {
            return; // Nothing to do here.
        }

        try {
            executeMojo(getDockerClient());
        } catch (DockerException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    /**
     * A subclass hook for doing the real job.
     *
     * @param dockerClient
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    protected abstract void executeMojo(DockerClient dockerClient) throws MojoFailureException, MojoExecutionException;

    private DockerClient getDockerClient() {
        DockerClientConfig.DockerClientConfigBuilder builder = new DockerClientConfig.DockerClientConfigBuilder().withUri(url.toString());

        if (version != null) {
            builder.withVersion(version);
        }

        return DockerClientBuilder.getInstance(builder.build()).build();
    }
}