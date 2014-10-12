package com.github.chmodas.test.mojo;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest extends AbstractMojoTestCase {
    protected final DockerClient dockerClient;
    protected final File pomFile;
    private final List<String> containerIds;

    public BaseTest() {
        pomFile = getPomFile();
        dockerClient = getDockerClient("http://localhost:4243");
        containerIds = new ArrayList<>();
    }

    private File getPomFile() {
        return getTestFile("src/test/resources/test-project/pom.xml");
    }

    @SuppressWarnings("SameParameterValue")
    private DockerClient getDockerClient(String url) {
        DockerClientConfig.DockerClientConfigBuilder builder = new DockerClientConfig.DockerClientConfigBuilder().withUri(url);
        return DockerClientBuilder.getInstance(builder.build()).build();
    }

    private List<Container> getContainerList() {
        return dockerClient.listContainersCmd().withShowAll(true).exec();
    }

    protected void setUp() throws Exception {
        super.setUp();

        /**
         * Get a list of the started containers, so we can clean up before
         * starting the next test.
         *
         * NOTE: starting a container that has the "test docker.prefix" in its name
         * while running the tests means that it will get removed.
         */
        for (Container x : getContainerList()) {
            containerIds.add(x.getId());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        /**
         * Forcefully remove containers that we feel should be removed.
         */
        for (Container x : getContainerList()) {
            if (!containerIds.contains(x.getId()) && x.getNames()[0].contains("/chmodas-test-")) {
                dockerClient.removeContainerCmd(x.getId()).withForce(true).exec();
                containerIds.remove(x.getId());
            }
        }
    }

    protected String getContainerIdByName(String name) {
        for (Container x : getContainerList()) {
            if (x.getNames()[0].equals("/chmodas-test-" + name)) {
                return x.getId();
            }
        }

        return null;
    }
}
