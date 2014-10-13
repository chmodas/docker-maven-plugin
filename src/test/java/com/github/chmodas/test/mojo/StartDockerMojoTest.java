package com.github.chmodas.test.mojo;


import com.github.chmodas.mojo.StartDockerMojo;
import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StartDockerMojoTest extends BaseTest {
    private StartDockerMojo mojo;

    private StartDockerMojo getMojo() throws Exception {
        return (StartDockerMojo) lookupMojo("start", pomFile);
    }

    private Image genImageObj(String name) {
        Image image = new Image();
        image.setName(name);
        image.setRegistry("busybox");
        image.setTag("latest");
        image.setCommand("sleep 999");
        return image;
    }

    public void testThatAnExceptionIsThrownIfThereIsNoRegistryEntry() throws Exception {
        mojo = getMojo();

        List<Image> images = new ArrayList<>();
        final Image image = new Image();
        image.setName("boohoo");
        image.setTag("latest");
        image.setCommand("sleep 999");
        images.add(image);
        setVariableValueToObject(mojo, "images", images);

        try {
            mojo.execute();
            fail("MojoExecutionException is not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.registry must not be null.")));
        }
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

    public void testThatAnExceptionIsThrowWhenThePortMappingEntriesDoNotMatchTheSupportedFormat() throws Exception {
        mojo = getMojo();

        List<Image> images = new ArrayList<>();
        Image image = genImageObj("boohoo");
        image.setPorts(new ArrayList<String>() {{
            add("80");
        }});
        images.add(image);
        setVariableValueToObject(mojo, "images", images);

        try {
            mojo.execute();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Invalid port mapping '80'.Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }
    }

    public void testCanBindPorts() throws Exception {
        mojo = getMojo();

        List<Image> images = new ArrayList<>();
        Image image = genImageObj("boohoo");
        image.setPorts(new ArrayList<String>() {{
            add("80:80");
            add("443:443");
        }});
        images.add(image);
        setVariableValueToObject(mojo, "images", images);

        mojo.execute();

        String containerId = getContainerIdByName("boohoo");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getHostConfig().getPortBindings().getBindings().get(ExposedPort.tcp(80)), is(equalTo(Ports.Binding(80))));
        assertThat(response.getHostConfig().getPortBindings().getBindings().get(ExposedPort.tcp(443)), is(equalTo(Ports.Binding(443))));
    }
}
