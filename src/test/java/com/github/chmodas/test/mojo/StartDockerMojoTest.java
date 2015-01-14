package com.github.chmodas.test.mojo;

import com.github.chmodas.mojo.StartDockerMojo;
import com.github.chmodas.mojo.objects.StartImage;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.VolumeBind;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StartDockerMojoTest extends BaseTest {
    private StartDockerMojo mojo;

    public void setUp() throws Exception {
        super.setUp();

        mojo = (StartDockerMojo) lookupMojo("start", pomFile);
    }

    private List<StartImage> genImages(String... names) {
        return genImages(true, names);
    }

    private List<StartImage> genImages(Boolean setCommand, String... names) {
        List<StartImage> images = new ArrayList<>();
        for (String name : names) {
            StartImage image = new StartImage();
            image.setName(name);
            image.setRepository("busybox");
            image.setTag("latest");
            if (setCommand) {
                image.setCommand("sleep 666");
            }
            images.add(image);
        }
        return images;
    }

    public void testThatCanDownloadMissingImage() throws Exception {
        for (Image image : dockerClient.listImagesCmd().exec()) {
            if (image.getRepoTags()[0].equals("busybox:latest")) {
                try {
                    dockerClient.removeImageCmd(image.getId()).withForce(true).exec();
                } catch (InternalServerErrorException e) {
                    if (e.getMessage().contains("Driver devicemapper failed to remove root filesystem")) {
                        // Known Docker "issue", in fact the device gets removed. Can just ignore it here.
                    } else {
                        throw new InternalServerErrorException(e);
                    }
                }
            }
        }

        setVariableValueToObject(mojo, "images", genImages("one"));
        setVariableValueToObject(mojo, "pullImages", true);
        try {
            mojo.execute();
        } catch (MojoFailureException e) {
            if (!e.getMessage().contains("No such image: busybox:latest")) {
                throw e;
            }
        }

        Boolean imageIsFound = false;
        for (Image image : dockerClient.listImagesCmd().exec()) {
            if (image.getRepoTags()[0].equals("busybox:latest")) {
                imageIsFound = true;
            }
        }

        assertThat(imageIsFound, is(equalTo(true)));
    }

    public void testThatExceptionWillBeThrownIfTheBareMinimumParametersAreNotProvided() throws Exception {
        try {
            List<StartImage> images = genImages(false, "one");
            images.get(0).setName(null);
            setVariableValueToObject(mojo, "images", images);
            mojo.execute();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.name must not be null")));
        }

        try {
            List<StartImage> images = genImages("one");
            images.get(0).setRepository(null);
            setVariableValueToObject(mojo, "images", images);
            mojo.execute();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.repository must not be null")));
        }
    }

    public void testThatCanStartContainerWithBareMinimumParameters() throws Exception {
        MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");

        assertThat(project.getProperties().getProperty("one.container.id"), is(nullValue()));

        setVariableValueToObject(mojo, "images", genImages(false, "one"));
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(not(nullValue())));

        /**
         * The Docker Remote API does not start the containers detached and the busybox
         * one starts with /bin/sh, thus we expect it to be stopped with exit status 0.
         */
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getState().isRunning(), is(equalTo(false)));
        assertThat(response.getState().getExitCode(), is(equalTo(0)));
        assertThat(response.getName(), is(equalTo("/chmodas-test-one")));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(project.getProperties().getProperty("one.container.id"), is(not(nullValue())));
    }

    public void testThatCanStartContainerWithCommandSpecified() throws Exception {
        List<StartImage> images = genImages("one");
        images.get(0).setCommand("sleep 666");

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getState().isRunning(), is(equalTo(true)));
        assertThat(response.getName(), is(equalTo("/chmodas-test-one")));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(response.getConfig().getCmd(), is(equalTo(new String[]{"sleep", "666"})));
    }

    public void testThatCanStartContainerWithHostnameSpecified() throws Exception {
        List<StartImage> images = genImages("one");
        images.get(0).setCommand("sleep 666");
        images.get(0).setHostname("example.com");

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getState().isRunning(), is(equalTo(true)));
        assertThat(response.getName(), is(equalTo("/chmodas-test-one")));
        assertThat(response.getConfig().getImage(), is(equalTo("busybox:latest")));
        assertThat(response.getConfig().getHostName(), is(equalTo("example.com")));
    }

    public void testThatCanStartContainerWithVolumes() throws Exception {
        List<StartImage> images = genImages("one");
        images.get(0).setCommand("sleep 666");
        images.get(0).setVolumes(new ArrayList<String>() {{
            add("/volume1");
            add("/host:/volume2");
        }});

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
        VolumeBind[] volumeBinds = inspectContainerResponse.getVolumes();
        assertThat(volumeBinds.length, is(equalTo(2)));
        List<String> hostPaths = new ArrayList<>();
        List<String> containerPaths = new ArrayList<>();
        for (VolumeBind bind : volumeBinds) {
            // If no host directory was given replace with /null
            String hostPath = bind.getHostPath();
            if (hostPath.contains("docker")) {
                hostPath = "/null";
            }
            hostPaths.add(hostPath);
            containerPaths.add(bind.getContainerPath());
        }
        assertThat(hostPaths, contains("/null", "/host"));
        assertThat(containerPaths, contains("/volume1", "/volume2"));
    }

    public void testThatAnExceptionIsThrowWhenThePortMappingEntriesDoNotMatchTheSupportedFormat() throws Exception {
        try {
            List<StartImage> images = genImages("one");
            images.get(0).setPorts(new ArrayList<String>() {{
                add("80");
            }});

            setVariableValueToObject(mojo, "images", images);
            mojo.execute();

            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Invalid port mapping '80'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }
    }

    public void testThatAnExceptionIsThrownWhenTheSameHostPortIsUsedMultipleTimes() throws Exception {
        try {
            List<StartImage> images = genImages("one", "two");
            List<String> ports = Arrays.asList("80:80");
            images.get(0).setPorts(ports);
            images.get(1).setPorts(ports);

            setVariableValueToObject(mojo, "images", images);
            mojo.execute();

            fail("MojoExceutionException not thrown");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("The port '80' is specified for use in multiple containers. One cannot run multiple containers that use the same port on the same host.")));
        }
    }

    public void testThatCanStartContainerWithMappedPorts() throws Exception {
        List<StartImage> images = genImages("one");
        List<String> ports = Arrays.asList("80:80", "443:443");
        images.get(0).setPorts(ports);

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(not(nullValue())));

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(response.getHostConfig().getPortBindings().getBindings().get(ExposedPort.tcp(80)), is(equalTo(Ports.Binding(80))));
        assertThat(response.getHostConfig().getPortBindings().getBindings().get(ExposedPort.tcp(443)), is(equalTo(Ports.Binding(443))));
    }

    public void testThatCanStartContainersWithDynamicMappedPortsToProjectVariables() throws Exception {
        MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");
        assertThat(project.getProperties().getProperty("http_port"), is(nullValue()));
        assertThat(project.getProperties().getProperty("https_port"), is(nullValue()));

        List<StartImage> images = genImages("one");
        List<String> ports = Arrays.asList("http_port:80", "https_port:443");
        images.get(0).setCommand("sleep 666");
        images.get(0).setPorts(ports);

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        assertThat(project.getProperties().getProperty("http_port"), is(notNullValue()));
        assertTrue(project.getProperties().getProperty("http_port").contains("49"));
        assertTrue(project.getProperties().getProperty("http_port").length() == 5);

        assertThat(project.getProperties().getProperty("https_port"), is(notNullValue()));
        assertTrue(project.getProperties().getProperty("https_port").contains("49"));
        assertTrue(project.getProperties().getProperty("https_port").length() == 5);
    }

    public void testThatAnExceptionIsThrownWhenContainerAliasIsAlreadyInUse() throws Exception {
        try {
            List<StartImage> images = genImages("one");
            images.get(0).setLinks(Arrays.asList("container:alias", "container:alias"));

            setVariableValueToObject(mojo, "images", images);
            mojo.execute();

            fail("MojoExecutionException is not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("A link for container 'container' with alias 'alias' exists already.")));
        }
    }

    public void testThatAnExceptionIsThrownWhenContainerLinkIsImpossible() throws Exception {
        try {
            List<StartImage> images = genImages("one");
            images.get(0).setLinks(Arrays.asList("container:alias"));

            setVariableValueToObject(mojo, "images", images);
            mojo.execute();

            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Container 'container' does not exist, cannot link to it.")));
        }
    }

    public void testCanLinkContainers() throws Exception {
        List<StartImage> images = genImages("one", "two");
        images.get(0).setCommand("sleep 666");
        images.get(1).setCommand("sleep 666");
        images.get(1).setLinks(new ArrayList<String>() {{
            add("one:one");
        }});

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerIdOne = getContainerIdByName("one");
        assertThat(containerIdOne, is(not(nullValue())));
        String containerIdTwo = getContainerIdByName("two");
        assertThat(containerIdTwo, is(not(nullValue())));

        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerIdTwo).exec();
        assertThat(inspectContainerResponse.getHostConfig().getLinks(), is(notNullValue()));
        assertThat(inspectContainerResponse.getHostConfig().getLinks(), equalTo(new String[]{"/chmodas-test-one:/chmodas-test-two/one"}));
    }

    public void testThatCanStartContainerWithEnv() throws Exception {
        List<StartImage> images = genImages("one");
        images.get(0).setEnv(new HashMap<String, String>() {{
            put("VARIABLE", "success");
        }});

        setVariableValueToObject(mojo, "images", images);
        mojo.execute();

        String containerId = getContainerIdByName("one");
        assertThat(containerId, is(notNullValue()));

        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
        assertThat(
                Arrays.asList(inspectContainerResponse.getConfig().getEnv()),
                containsInAnyOrder("VARIABLE=success", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"));
    }
}
