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

    public void testCanStartContainer() throws Exception {
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

    public void testCanStartMultipleContainers() throws Exception {
        mojo = getMojo();

        List<Image> images = new ArrayList<>();

        final Image img1 = new Image();
        img1.setName("boohoo");
        img1.setRegistry("busybox");
        img1.setTag("latest");
        img1.setCommand("sleep 999");

        final Image img2 = new Image();
        img2.setName("woohoo");
        img2.setRegistry("busybox");
        img2.setTag("latest");
        img2.setCommand("sleep 999");

        images.add(img1);
        images.add(img2);

        setVariableValueToObject(mojo, "images", images);

        mojo.execute();

        String container1Id = getContainerIdByName("boohoo");
        assertThat(container1Id, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(container1Id).exec();
        assertThat(response.getState().isRunning(), is(equalTo(true)));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(response.getName(), is(equalTo("/chmodas-test-boohoo")));
        assertThat(response.getConfig().getCmd().length, is(equalTo(2)));
        assertThat(response.getConfig().getCmd()[0], is(equalTo("sleep")));
        assertThat(response.getConfig().getCmd()[1], is(equalTo("999")));


        String container2Id = getContainerIdByName("woohoo");
        assertThat(container2Id, is(not(nullValue())));

        response = dockerClient.inspectContainerCmd(container2Id).exec();
        assertThat(response.getState().isRunning(), is(equalTo(true)));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(response.getName(), is(equalTo("/chmodas-test-woohoo")));
        assertThat(response.getConfig().getCmd().length, is(equalTo(2)));
        assertThat(response.getConfig().getCmd()[0], is(equalTo("sleep")));
        assertThat(response.getConfig().getCmd()[1], is(equalTo("999")));
    }
}
