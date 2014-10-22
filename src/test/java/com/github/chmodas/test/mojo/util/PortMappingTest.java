package com.github.chmodas.test.mojo.util;

import com.github.chmodas.mojo.util.PortMapping;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;


public class PortMappingTest {
    @Before
    public void resetPortMappingUsed() throws Exception {
        PortMapping.Used.reset();
    }

    @Test
    public void willThrowExceptionForUnsupportedFormat() throws Exception {
        try {
            List<String> ports = new ArrayList<String>() {{
                add("80");
            }};
            new PortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Invalid port mapping '80'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }

        try {
            List<String> ports = new ArrayList<String>() {{
                add("80:abc");
            }};
            new PortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Invalid port mapping '80:abc'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }

        try {
            List<String> ports = new ArrayList<String>() {{
                add("80:");
            }};
            new PortMapping(ports);
            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo("Invalid port mapping '80:'. Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).")));
        }
    }

    @Test
    public void willThrowExceptionForAlreadyUsedHostPort() throws Exception {
        try {
            new PortMapping(new ArrayList<String>() {{
                add("80:80");
                add("443:443");
            }});
            new PortMapping(new ArrayList<String>() {{
                add("80:80");
            }});

            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo(
                    "The port '80' is specified for use in multiple containers. " +
                    "One cannot run multiple containers that use the same port on the same host.")));
        }
    }

    @Test
    public void willThrowExceptionForAlreadyUsedVariableName() throws Exception {
        try {
            new PortMapping(new ArrayList<String>() {{
                add("http_port:80");
                add("https_port:443");
            }});
            new PortMapping(new ArrayList<String>() {{
                add("http_port:80");
            }});

            fail("MojoExecutionException not thrown.");
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), is(equalTo(
                    "The variable 'http_port' is specified for us in multiple containers. " +
                    "One cannot use the same variable for multiple ports.")));
        }
    }

    @Test
    public void canGetTheExposedPorts() throws Exception {
        PortMapping portMapping = new PortMapping(new ArrayList<String>());
        assertThat(portMapping.getExposedPorts(), is(equalTo(new ExposedPort[0])));

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("80:80");
                add("443:443");
            }});
            assertThat(portMapping.getExposedPorts(), is(not(equalTo(null))));
            assertThat(portMapping.getExposedPorts().length, is(equalTo(2)));
            assertThat(portMapping.getExposedPorts()[0], is(equalTo(ExposedPort.tcp(80))));
            assertThat(portMapping.getExposedPorts()[1], is(equalTo(ExposedPort.tcp(443))));
        } catch (Exception e) {
            throw e;
        }

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("http_port:80");
                add("https_port:443");
            }});
            assertThat(portMapping.getExposedPorts(), is(not(equalTo(null))));
            assertThat(portMapping.getExposedPorts().length, is(equalTo(2)));
            assertThat(portMapping.getExposedPorts()[0], is(equalTo(ExposedPort.tcp(80))));
            assertThat(portMapping.getExposedPorts()[1], is(equalTo(ExposedPort.tcp(443))));
        } catch (Exception e) {
            throw e;
        }

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("8080:80");
                add("alternative_https_port:443");
            }});
            assertThat(portMapping.getExposedPorts(), is(not(equalTo(null))));
            assertThat(portMapping.getExposedPorts().length, is(equalTo(2)));
            assertThat(portMapping.getExposedPorts()[0], is(equalTo(ExposedPort.tcp(80))));
            assertThat(portMapping.getExposedPorts()[1], is(equalTo(ExposedPort.tcp(443))));
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void canGetThePortsBinding() throws Exception {
        PortMapping portMapping = new PortMapping(new ArrayList<String>());
        assertThat(portMapping.getPortsBinding().getBindings(), is(equalTo(Collections.EMPTY_MAP)));

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("80:80");
                add("443:443");
            }});
            assertThat(portMapping.getPortsBinding().getBindings(), is(not(equalTo(Collections.EMPTY_MAP))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(80)), is(equalTo(Ports.Binding(80))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(443)), is(equalTo(Ports.Binding(443))));
        } catch (Exception e) {
            throw e;
        }

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("http_port:80");
                add("https_port:443");
            }});
            assertThat(portMapping.getPortsBinding().getBindings(), is(not(equalTo(Collections.EMPTY_MAP))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(80)), is(equalTo(Ports.Binding(0))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(443)), is(equalTo(Ports.Binding(0))));
        } catch (Exception e) {
            throw e;
        }

        try {
            portMapping = new PortMapping(new ArrayList<String>() {{
                add("8080:80");
                add("alternative_https_port:443");
            }});
            assertThat(portMapping.getPortsBinding().getBindings(), is(not(equalTo(Collections.EMPTY_MAP))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(80)), is(equalTo(Ports.Binding(8080))));
            assertThat(portMapping.getPortsBinding().getBindings().get(ExposedPort.tcp(443)), is(equalTo(Ports.Binding(0))));
        } catch (Exception e) {
            throw e;
        }
    }
}
