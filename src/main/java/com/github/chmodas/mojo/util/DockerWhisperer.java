package com.github.chmodas.mojo.util;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Container;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The whisperer does the talkin'.
 */
public class DockerWhisperer {
    private final DockerClient dockerClient;
    private final String prefix;
    private MavenProject mavenProject;

    public DockerWhisperer(DockerClient dockerClient, MavenProject mavenProject, String prefix) {
        this.dockerClient = dockerClient;
        this.mavenProject = mavenProject;
        this.prefix = prefix;
    }

    public DockerWhisperer(DockerClient dockerClient, String prefix) {
        this.dockerClient = dockerClient;
        this.prefix = prefix;
    }

    public void startContainers(List<Image> images, Boolean pullImages) throws MojoExecutionException {
        List<Whisper> whispers = new ArrayList<>();

        /**
         * Clean start.
         */
        PortMapping.Used.reset();
        ContainerLinks.Used.reset();

        /**
         * Perform some sanitation.
         */
        for (Image image : images) {
            Whisper whisper = new Whisper();

            whisper.setName(prefix, image.getName());
            whisper.setImage(image.getRepository(), image.getTag());
            whisper.setCommand(image.getCommand());
            whisper.setDataVolumes(image.getVolumes());
            whisper.setPortMapping(image.getPorts());
            whisper.setContainerLinks(image.getName(), prefix, image.getLinks());
            whisper.setWait(image.getWait());

            whispers.add(whisper);
        }
        ContainerLinks.Used.verify();

        /**
         * Pull the images if necessary.
         */
        if (pullImages != null && pullImages) {
            pullImages(images);
        }

        /**
         * Ready to start the containers.
         */
        for (Whisper x : whispers) {
            CreateContainerCmd createContainerCmd = dockerClient
                    .createContainerCmd(x.getImage())
                    .withName(x.getName())
                    .withExposedPorts(x.getPortMapping().getExposedPorts())
                    .withVolumes(x.getDataVolumes().getVolumes())
                    .withCmd(x.getCommand());

            CreateContainerResponse container = createContainerCmd.exec();

            StartContainerCmd startContainerCmd = dockerClient
                    .startContainerCmd(container.getId())
                    .withPortBindings(x.getPortMapping().getPortsBinding())
                    .withBinds(x.getDataVolumes().getBinds())
                    .withLinks(x.getContainerLinks().getLinks());

            startContainerCmd.exec();

            for (Map.Entry<String, String> entry : x.getPortMapping().getDynamicPortsBinding(dockerClient, container.getId()).entrySet()) {
                mavenProject.getProperties().setProperty(entry.getKey(), entry.getValue());
            }

            if (x.getWait() > 0) {
                try {
                    Thread.sleep(x.getWait() * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    void pullImages(List<Image> images) throws MojoExecutionException {
        for (Image x : images) {
            InputStream stream = dockerClient.pullImageCmd(x.getRepository()).withTag(x.getTag()).exec();
            String response = asString(stream);

            if (!response.contains("Download complete")) {
                throw new MojoExecutionException("Could not download image '" + x.getRepository() + ":" + x.getTag() + "'");
            }
        }
    }

    String asString(InputStream response) {
        try {
            StringWriter logwriter = new StringWriter();
            LineIterator itr = IOUtils.lineIterator(response, "UTF-8");
            while (itr.hasNext()) {
                String line = itr.next();
                logwriter.write(line + (itr.hasNext() ? "\n" : ""));
            }
            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    /**
     * Stop the containers gracefully.
     *
     * @param images List of images
     */
    public void stopContainers(List<Image> images) {
        Map<String, String> containerIds = getStartedContainerIds();
        for (Image x : images) {
            if (containerIds.containsKey(x.getName())) {
                String containerId = containerIds.get(x.getName());
                try {
                    try {
                        dockerClient.stopContainerCmd(containerId).withTimeout(2).exec();
                    } catch (NotModifiedException e) {
                        // Container already stopped.
                    }
                    dockerClient.removeContainerCmd(containerIds.get(x.getName())).withForce(true).exec();
                } catch (InternalServerErrorException e) {
                    if (e.getMessage().contains("Driver devicemapper failed to remove root filesystem")) {
                        //noinspection StatementWithEmptyBody
                        if (getStartedContainerIds().get(x.getName()) == null) {
                            // This issue is really annoying
                        }
                    }
                }
            }
        }
    }

    private Map<String, String> getStartedContainerIds() {
        Map<String, String> containersIds = new HashMap<>();

        Pattern pattern = Pattern.compile("^/" + prefix + "-([a-z-]+)$");
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container x : containers) {
            for (String y : x.getNames()) {
                Matcher matcher = pattern.matcher(y);
                if (matcher.matches()) {
                    containersIds.put(matcher.group(1), x.getId());
                    break;
                }
            }
        }

        return containersIds;
    }
}
