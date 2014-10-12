package com.github.chmodas.test.mojo;


import com.github.chmodas.mojo.StartDockerMojo;
import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.command.InspectContainerResponse;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StartDockerMojoTest extends BaseTest {
    private StartDockerMojo mojo;

    private StartDockerMojo getMojo() throws Exception {
        return (StartDockerMojo) lookupMojo("start", pomFile);
    }

    public void testCanStartContainers() throws Exception {
        mojo = getMojo();

        List<Image> images = new ArrayList<>();

        final Image img = new Image();
        img.setName("boohoo");
        img.setRegistry("busybox");
        img.setTag("latest");
        img.setCommand("sleep 999");

        images.add(img);
        setVariableValueToObject(mojo, "images", images);

        mojo.execute();

        String containerId = getContainerIdByName("boohoo");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getState().isRunning(), is(equalTo(true)));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(response.getName(), is(equalTo("/chmodas-test-boohoo")));
        assertThat(response.getConfig().getCmd().length, is(equalTo(2)));
        assertThat(response.getConfig().getCmd()[0], is(equalTo("sleep")));
        assertThat(response.getConfig().getCmd()[1], is(equalTo("999")));
    }
}
