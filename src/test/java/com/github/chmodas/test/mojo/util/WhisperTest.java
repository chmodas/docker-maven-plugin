package com.github.chmodas.test.mojo.util;

import com.github.chmodas.mojo.util.DataVolumes;
import com.github.chmodas.mojo.util.PortMapping;
import com.github.chmodas.mojo.util.Whisper;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
        assertThat(whisper.getImage(), is(equalTo(null)));
        assertThat(whisper.getRepository(), is(equalTo(null)));
        assertThat(whisper.getTag(), is(equalTo(null)));


        whisper = new Whisper();
        whisper.setImage("docker.example.com/repo", null);
        assertThat(whisper.getImage(), is(equalTo("docker.example.com/repo:latest")));
        assertThat(whisper.getRepository(), is(equalTo("docker.example.com/repo")));
        assertThat(whisper.getTag(), is(equalTo("latest")));

        whisper = new Whisper();
        whisper.setImage("docker.example.com/repo", "1.2.3");
        assertThat(whisper.getImage(), is(equalTo("docker.example.com/repo:1.2.3")));
        assertThat(whisper.getRepository(), is(equalTo("docker.example.com/repo")));
        assertThat(whisper.getTag(), is(equalTo("1.2.3")));
    }

    @Test
    public void canSetDataVolumes() throws Exception {
        Whisper whisper = new Whisper();
        assertThat(whisper.getDataVolumes(), is(equalTo(null)));

        whisper.setDataVolumes(new ArrayList<String>() {{
        }});
        assertThat(whisper.getDataVolumes(), is(instanceOf(DataVolumes.class)));
    }

    @Test
    public void canSetPortMapping() throws Exception {
        Whisper whisper = new Whisper();
        assertThat(whisper.getPortMapping(), is(equalTo(null)));

        whisper.setPortMapping(new ArrayList<String>() {{
        }});
        assertThat(whisper.getPortMapping(), is(instanceOf(PortMapping.class)));
    }
}
