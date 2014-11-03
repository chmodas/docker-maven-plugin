package com.github.chmodas.test.mojo;


import com.github.chmodas.mojo.StopDockerMojo;
import com.github.chmodas.mojo.objects.StopImage;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StopDockerMojoTest extends BaseTest {
    private StopDockerMojo mojo;

    public void setUp() throws Exception {
        super.setUp();

        mojo = (StopDockerMojo) lookupMojo("stop", pomFile);
    }

    private void startContainer() throws Exception {
        CreateContainerResponse createContainerResponse = dockerClient
                .createContainerCmd("busybox:latest")
                .withName("chmodas-test-container")
                .withCmd("sleep 999".split(" "))
                .exec();
        dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
    }

    public void testThatCanStopContainers() throws Exception {
        startContainer();

        assertThat(getContainerIdByName("container"), is(not(nullValue())));
        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(getContainerIdByName("container")).exec();
        assertThat(containerResponse.getState().isRunning(), is(equalTo(true)));

        List<StopImage> images = new ArrayList<>();
        final StopImage img = new StopImage();
        img.setName("container");
        images.add(img);

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        assertThat(getContainerIdByName("container"), is(nullValue()));
    }
}
