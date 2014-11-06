package com.github.chmodas.mojo;

import com.github.chmodas.mojo.objects.StartImage;
import com.github.chmodas.mojo.util.ContainerLinks;
import com.github.chmodas.mojo.util.PortMapping;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartDockerMojo extends AbstractDockerMojo {
    /**
     * Docker images configuration.
     */
    @Parameter(property = "docker.images")
    private List<StartImage> images;

    /**
     * Docker will auto pull any configured image. Set this to false to prevent that.
     */
    @Parameter(property = "docker.pullImages", defaultValue = "true")
    protected Boolean pullImages;

    @Override
    public void executeMojo(DockerClient dockerClient) throws MojoExecutionException, MojoFailureException {
        /**
         * Pull the images if necessary.
         */
        if (pullImages != null && pullImages) {
            pullImages(dockerClient, images);
        }

        /**
         * Clean start.
         */
        PortMapping.Used.reset();
        ContainerLinks.Used.reset();

        /**
         * Ready to start the containers.
         */
        Map<StartImage, StartContainerCmd> imageStartContainerCmdMap = new LinkedHashMap<>();
        for (StartImage image : images) {
            CreateContainerCmd createContainerCmd = dockerClient
                    .createContainerCmd(image.getRepository() + ":" + image.getTag())
                    .withName(getPrefixed(image.getName()))
                    .withCmd(image.getCommand())
                    .withHostName(image.getHostname())
                    .withVolumes(image.getDataVolumes().getVolumes())
                    .withExposedPorts(image.getPortMapping().getExposedPorts());

            StartContainerCmd startContainerCmd = dockerClient
                    .startContainerCmd(createContainerCmd.exec().getId())
                    .withBinds(image.getDataVolumes().getBinds())
                    .withPortBindings(image.getPortMapping().getPortsBinding())
                    .withLinks(image.getContainerLinks(image.getName(), prefix).getLinks());

            imageStartContainerCmdMap.put(image, startContainerCmd);
        }
        ContainerLinks.Used.verify();

        for (Map.Entry<StartImage, StartContainerCmd> entry : imageStartContainerCmdMap.entrySet()) {
            StartImage image = entry.getKey();
            StartContainerCmd cmd = entry.getValue();

            cmd.exec();

            for (Map.Entry<String, String> portMap : image.getPortMapping().getDynamicPortsBinding(dockerClient, cmd.getContainerId()).entrySet()) {
                project.getProperties().setProperty(portMap.getKey(), portMap.getValue());
            }

            if (image.getWait() != null) {
                try {
                    Thread.sleep(image.getWait());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            project.getProperties().setProperty(image.getName() + ".container.id", cmd.getContainerId());
        }
    }

    private void pullImages(DockerClient dockerClient, List<StartImage> images) throws MojoExecutionException {
        for (StartImage x : images) {
            InputStream stream = dockerClient.pullImageCmd(x.getRepository()).withTag(x.getTag()).exec();
            String response = asString(stream);

            if (!response.contains("Download complete") && !response.contains("Image is up to date")) {
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
}
