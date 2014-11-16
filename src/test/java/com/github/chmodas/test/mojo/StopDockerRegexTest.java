package com.github.chmodas.test.mojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.chmodas.mojo.StopDockerMojo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;

/**
 * @author mcoffey
 */
public class StopDockerRegexTest {

    private static final String PREFIX = "chmodas";
    private static final String ALPHA_IMAGE_NAME = "dockerstuff";
    private static final String ALPHA_NUMERIC_IMAGE_NAME = "dockerstuff1";
    // Not sure why a leading forward slash required for the match
    private static final String ALPHA_CONTAINER_NAME = "/" + PREFIX + "-" + ALPHA_IMAGE_NAME;
    private static final String ALPHA_NUMERIC_CONTAINER_NAME = "/" + PREFIX + "-" + ALPHA_NUMERIC_IMAGE_NAME;
    private static final String STUB_CONTAINER_ID = "containerid";

    // Object under test
    @Tested StopDockerMojo tested;

    // Field values injected into the object under test
    @Injectable String prefix = PREFIX;

    @Test
    public void testGetStartedContainerIdsWithAlphaName(@Mocked final DockerClient dockerClientMock,
                                                        @Mocked final ListContainersCmd listContainersCmdStub) {
        // Define fake object behaviour
        final List<Container> containerStubs = createContainerStubs(ALPHA_CONTAINER_NAME, STUB_CONTAINER_ID);
        
        new Expectations() {{
                dockerClientMock.listContainersCmd(); result = listContainersCmdStub;
                listContainersCmdStub.withShowAll(anyBoolean); result = listContainersCmdStub;
                listContainersCmdStub.exec(); result = containerStubs;
        }};
        
        // Run the test
        Map<String, String> result = Deencapsulation.invoke(tested, "getStartedContainerIds", dockerClientMock);
        
        // Verify results
        assertThat("Assert that exactly one container will be stopped", result.size(), is(1));
        assertThat("Assert that the container id to stop is " + STUB_CONTAINER_ID, result.get(ALPHA_IMAGE_NAME), is(STUB_CONTAINER_ID));
        assertThat("Assert that the container name to stop " + ALPHA_IMAGE_NAME, result.keySet().iterator().next(), is(ALPHA_IMAGE_NAME));
    }

    @Test
    public void testGetStartedContainerIdsWithAlphaNumericName(@Mocked final DockerClient dockerClientMock,
                                                               @Mocked final ListContainersCmd listContainersCmdStub) {
        // Define fake object behaviour
        final List<Container> containerStubs = createContainerStubs(ALPHA_NUMERIC_CONTAINER_NAME, STUB_CONTAINER_ID);
        
        new Expectations() {{
                dockerClientMock.listContainersCmd(); result = listContainersCmdStub;
                listContainersCmdStub.withShowAll(anyBoolean); result = listContainersCmdStub;
                listContainersCmdStub.exec(); result = containerStubs;
        }};
        
        // Run the test
        Map<String, String> result = Deencapsulation.invoke(tested, "getStartedContainerIds",  dockerClientMock);
        
        // Verify results
        assertThat("Assert that exactly one container will be stopped", result.size(), is(1));
        assertThat("Assert that the container id to stop is " + STUB_CONTAINER_ID, result.get(ALPHA_NUMERIC_IMAGE_NAME), is(STUB_CONTAINER_ID));
        assertThat("Assert that the container name to stop " + ALPHA_NUMERIC_IMAGE_NAME, result.keySet().iterator().next(), is(ALPHA_NUMERIC_IMAGE_NAME));
    }

    @SuppressWarnings("serial")
    private List<Container> createContainerStubs(final String containerName, final String containerId) {
        return new ArrayList<Container>() {{
                add(new Container() {

                    @Override
                    public String[] getNames() {
                        return new String[] {containerName};
                    }
                    public String getId() {
                        return containerId;
                    };
                });
            }};
    }
}