package com.github.chmodas;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;

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

    public DockerWhisperer(DockerClient dockerClient, String prefix) {
        this.dockerClient = dockerClient;
        this.prefix = prefix;

    }

    void sanitizeImageRegistry(Image image) throws MojoExecutionException {
        if (image.getRegistry() == null) {
            throw new MojoExecutionException("image.registry must not be null.");
        }
    }

    Map<String, Map<Integer, Integer>> sanitizePortMapping(Image image, Map<String, Map<Integer, Integer>> portMapping) throws MojoExecutionException {
        if (image.getPorts() != null && image.getPorts().size() > 0) {
            for (String port : image.getPorts()) {
                try {
                    String[] ps = port.split(":", 2);
                    if (ps.length != 2) {
                        throw new MojoExecutionException("Invalid port mapping '" + port + "'." +
                                                         "Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).");
                    }

                    Integer hostPort = Integer.parseInt(ps[0]);
                    for (Map<Integer, Integer> ports : portMapping.values()) {
                        if (ports.containsKey(hostPort)) {
                            throw new MojoExecutionException("The port '" + hostPort + "' is specified for use in multiple containers." +
                                                             "One cannot run multiple containers that use the same port on the same host.");
                        }
                    }
                    Integer exposedPort = Integer.parseInt(ps[1]);

                    Map<Integer, Integer> exposedToHostPort;
                    if (!portMapping.containsKey(image.getName())) {
                        exposedToHostPort = new HashMap<>();
                    } else {
                        exposedToHostPort = portMapping.get(image.getName());
                    }
                    exposedToHostPort.put(hostPort, exposedPort);
                    portMapping.put(image.getName(), exposedToHostPort);

                } catch (NumberFormatException e) {
                    throw new MojoExecutionException("Invalid port mapping '" + port + "'." +
                                                     "Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).");
                }
            }
        }

        return portMapping;
    }

    public void startContainers(List<Image> images, Boolean pullImages) throws MojoExecutionException {
        /**
         * Perform some sanitation.
         */
        Map<String, Map<Integer, Integer>> containersPortMapping = new HashMap<>();
        for (Image image : images) {
            sanitizeImageRegistry(image);
            containersPortMapping = sanitizePortMapping(image, containersPortMapping);
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
            String image = x.getRegistry() + ":" + x.getTag();

            Ports portBindings = new Ports();
            List<ExposedPort> exposedPorts = new ArrayList<>();

            Map<Integer, Integer> portMapping = containersPortMapping.get(x.getName());
            if (portMapping != null && !portMapping.isEmpty()) {

                for (Map.Entry<Integer, Integer> entry : portMapping.entrySet()) {
                    ExposedPort exposedPort = ExposedPort.tcp(entry.getValue());
                    exposedPorts.add(exposedPort);
                    portBindings.bind(exposedPort, Ports.Binding(entry.getKey()));
                }
            }

            CreateContainerResponse container = dockerClient
                    .createContainerCmd(image)
                    .withName(name)
                    .withCmd(x.getCommand().split(" "))
                    .withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]))
                    .exec();

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
     * @param images
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
