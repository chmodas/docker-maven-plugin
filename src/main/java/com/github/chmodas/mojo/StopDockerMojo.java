package com.github.chmodas.mojo;

import com.github.chmodas.mojo.objects.StopImage;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopDockerMojo extends AbstractDockerMojo {
    /**
     * Docker images configuration.
     */
    @Parameter(property = "docker.images")
    private List<StopImage> images;

    @Override
    public void executeMojo(DockerClient dockerClient) throws MojoExecutionException, MojoFailureException {
        Map<String, String> containerIds = getStartedContainerIds(dockerClient);
        for (StopImage image : images) {
            if (containerIds.containsKey(image.getName())) {
                getLog().info("Stopping container " + image.getName() + ".");

                String containerId = containerIds.get(image.getName());
                try {
                    try {
                        dockerClient.stopContainerCmd(containerId).withTimeout(2).exec();
                    } catch (NotModifiedException e) {
                        // Container already stopped.
                    }
                    dockerClient.removeContainerCmd(containerIds.get(image.getName())).withForce(true).exec();
                } catch (InternalServerErrorException e) {
                    if (e.getMessage().contains("Driver devicemapper failed to remove root filesystem")) {
                        //noinspection StatementWithEmptyBody
                        if (getStartedContainerIds(dockerClient).get(image.getName()) == null) {
                            // This issue is really annoying
                        }
                    }
                }
            }
        }
    }

    private Map<String, String> getStartedContainerIds(DockerClient dockerClient) {
        Map<String, String> containersIds = new HashMap<>();

        Pattern pattern = Pattern.compile("^/" + prefix + "-([a-z-]+)$");
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container x : containers) {
            for (String y : x.getNames()) {
                Matcher matcher = pattern.matcher(y);
                if (matcher.matches()) {
                    containersIds.put(matcher.group(1), x.getId());
                    break;
                }
            }
        }

        return containersIds;
    }
}
