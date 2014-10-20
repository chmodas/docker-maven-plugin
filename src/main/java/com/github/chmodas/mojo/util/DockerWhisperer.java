package com.github.chmodas.mojo.util;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
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
        if (image.getRepository() == null) {
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
            String image = x.getRepository() + ":" + x.getTag();

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

            List<Entry<String, Integer>> dynamicPortMapping = mappedPorts.getDynamicPortsMap(x.getName());
            if (dynamicPortMapping != null) {
                for (Entry<String, Integer> entry : dynamicPortMapping) {
                    ExposedPort exposedPort = ExposedPort.tcp(entry.getValue());
                    exposedPorts.add(exposedPort);
                    portBindings.bind(exposedPort, Ports.Binding(0));
                }
            }

            DataVolumes dataVolumes = new DataVolumes(name, x.getVolumes());

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image).withName(name);
            if (x.getCommand() != null) {
                createContainerCmd.withCmd(x.getCommand().split(" "));
            }
            if (exposedPorts.size() > 0) {
                createContainerCmd.withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));
            }
            if (dataVolumes.hasVolumes()) {
                createContainerCmd.withVolumes(dataVolumes.getVolumes());
            }
            CreateContainerResponse container = createContainerCmd.exec();

            StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(container.getId())
                                                              .withPortBindings(portBindings);

            if (dataVolumes.hasBinds()) {
                startContainerCmd.withBinds(dataVolumes.getBinds());
            }

            startContainerCmd.exec();

            if (dynamicPortMapping != null) {
                for (Entry<String, Integer> entry : mappedPorts.getDynamicPortsForVariables(x.getName(), container.getId())) {
                    mavenProject.getProperties().setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }

    void pullImages(List<Image> images) {
        for (Image x : images) {
            dockerClient.pullImageCmd(x.getRepository()).withTag(x.getTag()).exec();
        }
    }

    /**
     * Stop the containers gracefully.
     *
     * @param images List of images
     */
    public void stopContainers(List<Image> images) {
        Map<String, String> containerIds = getStartedContainerIds();

        for (Image x : images) {
            if (containerIds.containsKey(x.getName())) {
                String containerId = containerIds.get(x.getName());
                try {
                    try {
                        dockerClient.stopContainerCmd(containerId).withTimeout(2).exec();
                    } catch (NotModifiedException e) {
                        // Container already stopped.
                    }
                    dockerClient.removeContainerCmd(containerIds.get(x.getName())).withForce(true).exec();
                } catch (InternalServerErrorException e) {
                    if (e.getMessage().contains("Driver devicemapper failed to remove root filesystem")) {
                        //noinspection StatementWithEmptyBody
                        if (getStartedContainerIds().get(x.getName()) == null) {
                            // This issue is really annoying
                        }
                    }
                }
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
