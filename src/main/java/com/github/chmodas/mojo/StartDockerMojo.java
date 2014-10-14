package com.github.chmodas.mojo;

import com.github.chmodas.DockerWhisperer;
import com.github.dockerjava.api.DockerClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartDockerMojo extends AbstractDockerMojo {
    @Override
    public void executeMojo(DockerClient dockerClient) throws MojoExecutionException, MojoFailureException {
        DockerWhisperer whisperer = new DockerWhisperer(dockerClient, project, prefix);
        whisperer.startContainers(images, pullImages);
    }
}
