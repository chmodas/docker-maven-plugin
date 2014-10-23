package com.github.chmodas.test.mojo;


import com.github.chmodas.mojo.StopDockerMojo;
import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StopDockerMojoTest extends BaseTest {
    private StopDockerMojo mojo;

    private StopDockerMojo getMojo() throws Exception {
        return (StopDockerMojo) lookupMojo("stop", pomFile);
    }

    public void testCanStopContainers() throws Exception {
        CreateContainerResponse createContainerResponse = dockerClient
                .createContainerCmd("busybox:latest")
                .withName("chmodas-test-boohoo")
                .withCmd("sleep 999".split(" "))
                .exec();
        dockerClient.startContainerCmd(createContainerResponse.getId()).exec();

        assertThat(getContainerIdByName("boohoo"), is(not(nullValue())));
        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(getContainerIdByName("boohoo")).exec();
        assertThat(containerResponse.getState().isRunning(), is(equalTo(true)));

        mojo = getMojo();

        List<Image> images = new ArrayList<>();
        final Image img = new Image();
        img.setName("boohoo");
        img.setRepository("busybox");
        img.setTag("latest");
        img.setCommand("sleep 999");
        images.add(img);
        setVariableValueToObject(mojo, "images", images);

        mojo.execute();

        assertThat(getContainerIdByName("boohoo"), is(nullValue()));
    }
}
