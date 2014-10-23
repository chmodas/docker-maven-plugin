package com.github.chmodas.test.mojo.util;

import com.github.chmodas.mojo.util.Whisper;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class WhisperTest {
    @Test
    public void canSetName() throws Exception {
        String prefix = "chmodas-test";
        try {
            new Whisper().setName(prefix, null);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.name must not be null")));
        }

        Whisper whisper = new Whisper();

        whisper.setName(prefix, "Boo");
        assertThat(whisper.getName(), is(equalTo("chmodas-test-boo")));

        whisper.setName(prefix, " Moo ");
        assertThat(whisper.getName(), is(equalTo("chmodas-test-moo")));
    }

    @Test
    public void catSetImage() throws Exception {
        try {
            new Whisper().setImage(null, null);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.repository must not be null")));
        }

        Whisper whisper = new Whisper();
        whisper.setImage("docker.example.com/repo", null);
        assertThat(whisper.getImage(), is(equalTo("docker.example.com/repo:latest")));

        whisper = new Whisper();
        whisper.setImage("docker.example.com/repo", "1.2.3");
        assertThat(whisper.getImage(), is(equalTo("docker.example.com/repo:1.2.3")));
    }

    @Test
    public void canSetDataVolumes() throws Exception {
        Whisper whisper = new Whisper();

        List<String> volumes = new ArrayList<String>() {{
            add("/volume");
            add("/host:/container");
        }};
        whisper.setDataVolumes(volumes);
        assertThat(whisper.getDataVolumes().getVolumes(), is(equalTo(new Volume[]{new Volume("/volume"), new Volume("/container")})));
        assertThat(whisper.getDataVolumes().getBinds(), is(equalTo(new Bind[]{new Bind("/host", new Volume("/container"))})));
    }

    @Test
    public void setPortMapping() throws Exception {
        try {
            List<String> ports = new ArrayList<String>() {{
                add("80");
            }};
            new Whisper().setPortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo(
                    "Invalid port mapping '80'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }

        try {
            List<String> ports = new ArrayList<String>() {{
                add("80:abc");
            }};
            new Whisper().setPortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo(
                    "Invalid port mapping '80:abc'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }

        try {
            List<String> ports = new ArrayList<String>() {{
                add("80:");
            }};
            new Whisper().setPortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo(
                    "Invalid port mapping '80:'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }
    }
}
