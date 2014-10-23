package com.github.chmodas.mojo.util;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * An Image becomes a whisper.
 */
public class Whisper {
    private String name;
    private String image;
    private DataVolumes dataVolumes;
    private PortMapping portMapping;

    public Whisper() {
    }

    public String getName() {
        return name;
    }

    public void setName(String prefix, String name) throws MojoExecutionException {
        if (name == null) {
            throw new MojoExecutionException("image.name must not be null");
        }

        this.name = prefix + "-" + name.trim().toLowerCase();
    }

    public String getImage() {
        return image;
    }

    public void setImage(String repository, String tag) throws MojoExecutionException {
        // TODO: better validation

        if (repository == null) {
            throw new MojoExecutionException("image.repository must not be null");
        }

        if (tag == null) {
            tag = "latest";
        }

        this.image = repository + ":" + tag;
    }

    public DataVolumes getDataVolumes() {
        return this.dataVolumes;
    }

    public void setDataVolumes(List<String> volumes) {
        if (volumes != null) {
            this.dataVolumes = new DataVolumes(volumes);
        }
    }

    public void setPortMapping(List<String> ports) throws MojoExecutionException {
        if (ports != null) {
            this.portMapping = new PortMapping(ports);
        }
    }
}
