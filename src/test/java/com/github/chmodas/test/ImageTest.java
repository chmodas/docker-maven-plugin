package com.github.chmodas.test;

import com.github.chmodas.mojo.objects.Image;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ImageTest {
    @Test
    public void canGetTheDefaultTag() {
        Image image = new Image();
        assertThat(image.getTag(), is(equalTo("latest")));
    }
}
