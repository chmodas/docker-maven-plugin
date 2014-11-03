package com.github.chmodas.test.mojo.objects;

import com.github.chmodas.mojo.objects.StopImage;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class StopImageTest {
    @Test
    public void getNameWillThrowAnExceptionIfNameIsNull() throws Exception {
        try {
            new StopImage().getName();
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("image.name must not be null")));
        }
    }
}
