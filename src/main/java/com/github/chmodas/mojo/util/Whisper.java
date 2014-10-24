package com.github.chmodas.mojo.util;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * An Image becomes a whisper.
 */
public class Whisper {
    private String name;
    private String image;
    private String repository;
    private String tag;
    private String[] command;
    private DataVolumes dataVolumes;
    private PortMapping portMapping;
    private ContainerLinks containerLinks;

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
        this.repository = repository;
        this.tag = tag;
    }

    public String getRepository() {
        return repository;
    }

    public String getTag() {
        return tag;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String command) {
        if (command != null) {
            this.command = command.split(" ");
        } else {
            this.command = new String[0];
        }
    }

    public DataVolumes getDataVolumes() {
        return this.dataVolumes;
    }

    public void setDataVolumes(List<String> volumes) {
        this.dataVolumes = new DataVolumes(volumes);
    }

    public void setPortMapping(List<String> ports) throws MojoExecutionException {
        this.portMapping = new PortMapping(ports);
    }

    public PortMapping getPortMapping() {
        return portMapping;
    }

    public ContainerLinks getContainerLinks() {
        return containerLinks;
    }

    public void setContainerLinks(String containerName, String prefix, List<String> links) throws MojoExecutionException {
        this.containerLinks = new ContainerLinks(containerName, prefix, links);
    }
}
