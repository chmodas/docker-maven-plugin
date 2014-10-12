package com.github.chmodas;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;

import java.util.List;

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
}
