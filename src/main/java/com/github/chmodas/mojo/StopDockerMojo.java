package com.github.chmodas.mojo;

import com.github.chmodas.mojo.util.DockerWhisperer;
import com.github.dockerjava.api.DockerClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopDockerMojo extends AbstractDockerMojo {
    @Override
    public void executeMojo(DockerClient dockerClient) {
        DockerWhisperer whisperer = new DockerWhisperer(dockerClient, prefix);
        whisperer.stopContainers(images);
    }
}
