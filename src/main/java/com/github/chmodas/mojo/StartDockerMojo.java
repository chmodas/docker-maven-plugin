package com.github.chmodas.mojo;

import com.github.chmodas.DockerWhisperer;
import com.github.dockerjava.api.DockerClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartDockerMojo extends AbstractDockerMojo {
    @Override
    public void executeMojo(DockerClient dockerClient) {
        DockerWhisperer whisperer = new DockerWhisperer(dockerClient, prefix);
        whisperer.startContainers(images, pullImages);
    }
}
