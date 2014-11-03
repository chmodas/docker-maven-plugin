package com.github.chmodas.test.mojo.objects;

import com.github.chmodas.mojo.objects.StartImage;
import com.github.chmodas.mojo.util.DataVolumes;
import com.github.chmodas.mojo.util.PortMapping;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class StartImageTest {
    @Test
    public void getNameWillThrowAnExceptionIfNameIsNull() throws Exception {
        try {
            new StartImage().getName();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.name must not be null")));
        }
    }

    @Test
    public void getNameWillTrimAndLowercaseTheName() throws Exception {
        StartImage image = new StartImage();
        image.setName(" Name ");
        assertThat(image.getName(), is(equalTo("name")));
    }

    @Test
    public void getRepositoryWillThrownAnExceptionIfRepositoryIsNull() throws Exception {
        try {
            new StartImage().getRepository();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.repository must not be null")));
        }
    }

    @Test
    public void getRepositoryWillReturnTheExpectedResult() throws Exception {
        String repository = "registry.example.com/repository";
        StartImage image = new StartImage();
        image.setRepository(repository);
        assertThat(image.getRepository(), is(equalTo(repository)));
    }

    @Test
    public void getTagWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getTag(), is(equalTo("latest")));
        image.setTag("tagged");
        assertThat(image.getTag(), is(equalTo("tagged")));
    }

    @Test
    public void getCommandWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getCommand(), is(equalTo(new String[0])));
        image.setCommand("sleep 123");
        assertThat(image.getCommand(), is(equalTo(new String[]{"sleep", "123"})));
    }

    @Test
    public void getHostnameWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getHostname(), is(equalTo(null)));
        image.setHostname(" example.com ");
        assertThat(image.getHostname(), is(equalTo("example.com")));
    }

    @Test
    public void getDataVolumesWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getDataVolumes(), is(instanceOf(DataVolumes.class)));
    }

    @Test
    public void getPortMappingWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getPortMapping(), is(instanceOf(PortMapping.class)));
    }

    @Test
    public void getWaitWillReturnTheExpectedResult() throws Exception {
        StartImage image = new StartImage();
        assertThat(image.getWait(), is(equalTo(null)));

        image = new StartImage();
        image.setWait(666);
        assertThat(image.getWait(), is(equalTo(666 * 1000)));
    }
}
