package com.github.chmodas;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerWhisperer {
    private final DockerClient dockerClient;
    private final String prefix;

    public DockerWhisperer(DockerClient dockerClient, String prefix) {
        this.dockerClient = dockerClient;
        this.prefix = prefix;

    }

    public void startContainers(List<Image> images, Boolean pullImages) {
        if (pullImages != null && pullImages) {
            pullImages(images);
        }

        for (Image x : images) {
            String name = prefix + "-" + x.getName();
            String image = x.getRegistry() + ":" + x.getTag();

            CreateContainerResponse container = dockerClient
                    .createContainerCmd(image)
                    .withName(name)
                    .withCmd(x.getCommand().split(" "))
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

        }
    }

    void pullImages(List<Image> images) {
        for (Image x : images) {
            dockerClient.pullImageCmd(x.getRegistry()).withTag(x.getTag()).exec();
        }
    }

    /**
     * Forcefully remove the specified containers.
     * TODO: parametize this, might have cases where you want them running after the tests are finished
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
