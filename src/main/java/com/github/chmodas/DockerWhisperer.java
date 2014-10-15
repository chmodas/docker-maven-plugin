package com.github.chmodas;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The whisperer does the talkin'.
 */
public class DockerWhisperer {
    private final DockerClient dockerClient;
    private final String prefix;
    private MavenProject mavenProject;

    public DockerWhisperer(DockerClient dockerClient, MavenProject mavenProject, String prefix) {
        this.dockerClient = dockerClient;
        this.mavenProject = mavenProject;
        this.prefix = prefix;
    }

    public DockerWhisperer(DockerClient dockerClient, String prefix) {
        this.dockerClient = dockerClient;
        this.prefix = prefix;
    }

    void sanitizeImageRegistry(Image image) throws MojoExecutionException {
        if (image.getRegistry() == null) {
            throw new MojoExecutionException("image.registry must not be null.");
        }
    }

    public void startContainers(List<Image> images, Boolean pullImages) throws MojoExecutionException {
        /**
         * Perform some sanitation.
         */
        for (Image image : images) {
            sanitizeImageRegistry(image);
        }

        PortMapping mappedPorts = new PortMapping(dockerClient, images);

        /**
         * Pull the images if necessary.
         */
        if (pullImages != null && pullImages) {
            pullImages(images);
        }

        /**
         * Ready to start the containers.
         */
        for (Image x : images) {
            String name = prefix + "-" + x.getName();
            String image = x.getRegistry() + ":" + x.getTag();

            Ports portBindings = new Ports();
            List<ExposedPort> exposedPorts = new ArrayList<>();

            List<Entry<Integer, Integer>> staticPortMapping = mappedPorts.getStaticPortsMap(x.getName());
            if (staticPortMapping != null) {
                for (Entry<Integer, Integer> entry : staticPortMapping) {
                    ExposedPort exposedPort = ExposedPort.tcp(entry.getValue());
                    exposedPorts.add(exposedPort);
                    portBindings.bind(exposedPort, Ports.Binding(entry.getKey()));
                }
            }

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image).withName(name);
            if (x.getCommand() != null) {
                createContainerCmd.withCmd(x.getCommand().split(" "));
            }
            if (exposedPorts.size() > 0) {
                createContainerCmd.withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));
            }
            CreateContainerResponse container = createContainerCmd.exec();

            dockerClient.startContainerCmd(container.getId())
                        .withPortBindings(portBindings)
                        .exec();
        }
    }

    void pullImages(List<Image> images) {
        for (Image x : images) {
            dockerClient.pullImageCmd(x.getRegistry()).withTag(x.getTag()).exec();
        }
    }

    /**
     * Forcefully remove the specified containers.
     * TODO: parametize this, might have cases where you want them running after the tests are finished,
     * or might not want to force the removal
     *
     * @param images List of images
     */
    public void stopContainers(List<Image> images) {
        Map<String, String> containerIds = getStartedContainerIds();

        for (Image x : images) {
            if (containerIds.containsKey(x.getName())) {
                dockerClient.removeContainerCmd(containerIds.get(x.getName())).withForce(true).exec();
            }
        }
    }

    private Map<String, String> getStartedContainerIds() {
        Map<String, String> containersIds = new HashMap<>();

        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container x : containers) {
            if (x.getNames()[0].contains("/" + prefix + "-")) {
                String nameWithoutPrefix = x.getNames()[0].replace("/" + prefix + "-", "");
                containersIds.put(nameWithoutPrefix, x.getId());
            }
        }

        return containersIds;
    }
}
