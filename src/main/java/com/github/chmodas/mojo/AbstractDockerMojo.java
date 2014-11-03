package com.github.chmodas.mojo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.net.URI;

public abstract class AbstractDockerMojo extends AbstractMojo {
    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
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
     * Namespace prefix used for isolation.
     */
    @Parameter(property = "docker.prefix", defaultValue = "${project.artifactId}")
    protected String prefix;

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
     * @param dockerClient Initialized DockerClient object
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    protected abstract void executeMojo(DockerClient dockerClient) throws MojoFailureException, MojoExecutionException;

    /**
     * Configure and initialize the DockerClient.
     *
     * @return DockerClient
     */
    private DockerClient getDockerClient() {
        DockerClientConfig.DockerClientConfigBuilder builder = new DockerClientConfig.DockerClientConfigBuilder().withUri(url.toString());

        if (version != null) {
            builder.withVersion(version);
        }

        return DockerClientBuilder.getInstance(builder.build()).build();
    }

    /**
     * @param string A string that has to be prefixed with the plugin prefix.
     * @return The prefixed string.
     */
    protected String getPrefixed(String string) {
        return prefix + "-" + string;
    }
}
