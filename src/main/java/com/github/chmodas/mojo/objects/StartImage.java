package com.github.chmodas.mojo.objects;

import com.github.chmodas.mojo.util.ContainerLinks;
import com.github.chmodas.mojo.util.DataVolumes;
import com.github.chmodas.mojo.util.PortMapping;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

public class StartImage extends AbstractImage {
    private String repository;
    private String tag = "latest";
    private String command;
    private String hostname;
    private List<String> volumes;
    private List<String> ports;
    private List<String> links;
    private Integer wait;

    private DataVolumes dataVolumes;
    private PortMapping portMapping;
    private ContainerLinks containerLinks;

    public StartImage() {}

    // TODO: better validation
    public String getRepository() throws MojoExecutionException {
        if (repository == null) {
            throw new MojoExecutionException("image.repository must not be null");
        }
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String[] getCommand() {
        if (command != null) {
            return command.split(" ");
        } else {
            return new String[0];
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHostname() {
        if (hostname != null) {
            return hostname.trim();
        }
        return null;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public DataVolumes getDataVolumes() {
        if (dataVolumes == null) {
            dataVolumes = new DataVolumes(volumes);
        }
        return dataVolumes;
    }

    public void setVolumes(List<String> volumes) {
        this.volumes = volumes;
    }

    public PortMapping getPortMapping() throws MojoExecutionException {
        if (portMapping == null) {
            portMapping = new PortMapping(ports);
        }
        return portMapping;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public ContainerLinks getContainerLinks(String name, String prefix) throws MojoExecutionException {
        if (containerLinks == null) {
            containerLinks = new ContainerLinks(name, prefix, links);
        }
        return containerLinks;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }


    public Integer getWait() {
        if (wait != null) {
            return wait * 1000;
        }
        return null;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }
}
