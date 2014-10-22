package com.github.chmodas.mojo.util;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Container;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void startContainers(List<Image> images, Boolean pullImages) throws MojoExecutionException {
        List<Whisper> whispers = new ArrayList<>();

        /**
         * Start with clean lists.
         */
        PortMapping.Used.reset();

        /**
         * Perform some sanitation.
         */
        for (Image image : images) {
            Whisper whisper = new Whisper();

            whisper.setName(prefix, image.getName());
            whisper.setRepository(image.getRepository());

            whispers.add(whisper);
        }

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

            PortMapping portMapping = new PortMapping(x.getPorts());

            DataVolumes dataVolumes = new DataVolumes(name, x.getVolumes());

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image).withName(name);
            if (x.getCommand() != null) {
                createContainerCmd.withCmd(x.getCommand().split(" "));
            }

            createContainerCmd.withExposedPorts(portMapping.getExposedPorts());

            if (dataVolumes.hasVolumes()) {
                createContainerCmd.withVolumes(dataVolumes.getVolumes());
            }
            CreateContainerResponse container = createContainerCmd.exec();

            StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(container.getId())
                                                              .withPortBindings(portMapping.getPortsBinding());

            if (dataVolumes.hasBinds()) {
                startContainerCmd.withBinds(dataVolumes.getBinds());
            }

            startContainerCmd.exec();

            for (Map.Entry<String, String> entry : portMapping.getDynamicPortsBinding(dockerClient, container.getId()).entrySet()) {
                mavenProject.getProperties().setProperty(entry.getKey(), entry.getValue());
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
