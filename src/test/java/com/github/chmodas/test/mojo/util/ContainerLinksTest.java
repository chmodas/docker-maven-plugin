package com.github.chmodas.test.mojo.util;

import com.github.chmodas.mojo.util.ContainerLinks;
import com.github.chmodas.mojo.util.ContainerLinks.Used;
import com.github.dockerjava.api.model.Link;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ContainerLinksTest {
    @Before
    public void resetContainerLinksMatrix() {
        Used.reset();
    }

    @Test
    public void withThrowExceptionForAlreadyUsedAlias() throws Exception {
        try {
            List<String> links = new ArrayList<String>() {{
                add("container:alias");
                add("container:alias");
            }};
            Used.reset();
            new ContainerLinks("boohoo", "chmodas-test", links);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("A link for container 'container' with alias 'alias' exists already.")));
        }
    }

    @Test
    public void canGetTheLinks() throws Exception {
        List<String> links = new ArrayList<>();
        ContainerLinks containerLinks = new ContainerLinks("boohoo", "chmodas-test", links);
        assertThat(containerLinks.getLinks(), is(equalTo(new Link[0])));

        links.add("container:container");
        containerLinks = new ContainerLinks("boohoo", "chmodas-test", links);
        assertThat(containerLinks.getLinks(), is(equalTo(new Link[]{new Link("chmodas-test-container", "container")})));

        links.add("container2:container2");
        containerLinks = new ContainerLinks("boohoo", "chmodas-test", links);
        assertThat(containerLinks.getLinks(), is(equalTo(new Link[]{new Link("chmodas-test-container", "container"), new Link("chmodas-test-container2", "container2")})));
    }
}
