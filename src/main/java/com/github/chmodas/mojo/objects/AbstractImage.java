package com.github.chmodas.mojo.objects;

import org.apache.maven.plugin.MojoExecutionException;

abstract class AbstractImage {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    // TODO: better name validation
    public String getName() throws MojoExecutionException {
        if (name == null) {
            throw new MojoExecutionException("image.name must not be null");
        }
        return name.trim().toLowerCase();
    }
}
