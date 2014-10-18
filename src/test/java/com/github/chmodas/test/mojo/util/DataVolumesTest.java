package com.github.chmodas.test.mojo.util;


import com.github.chmodas.mojo.util.DataVolumes;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DataVolumesTest {
    @Test
    public void canCheckIfThereAreNewVolumesThatHaveTobeCreatedInsideTheContainer() throws Exception {
        List<String> volumesFromMojo = new ArrayList<>();
        DataVolumes dataVolumes = new DataVolumes("boohoo", volumesFromMojo);
        assertThat(dataVolumes.hasVolumes(), is(equalTo(false)));

        volumesFromMojo.add("/volume");
        dataVolumes = new DataVolumes("boohoo", volumesFromMojo);
        assertThat(dataVolumes.hasVolumes(), is(equalTo(true)));
    }

    @Test
    public void canAddEntriesForNewVolumesToBeCreatedInsideTheContainer() throws Exception {
        List<String> volumesFromMojo = new ArrayList<>();
        DataVolumes dataVolumes = new DataVolumes("boohoo", volumesFromMojo);
        assertThat(dataVolumes.hasVolumes(), is(equalTo(false)));
        assertThat(dataVolumes.getBinds(), is(equalTo(null)));

        volumesFromMojo.add("/volume");
        dataVolumes = new DataVolumes("boohoo", volumesFromMojo);
        assertThat(dataVolumes.hasVolumes(), is(equalTo(true)));
        assertThat(dataVolumes.getVolumes().length, is(equalTo(1)));
        assertThat(dataVolumes.getVolumes()[0], is(equalTo(new Volume("/volume"))));

        volumesFromMojo.add("/volume2");
        dataVolumes = new DataVolumes("boohoo", volumesFromMojo);
        assertThat(dataVolumes.hasVolumes(), is(equalTo(true)));
        assertThat(dataVolumes.getVolumes().length, is(equalTo(2)));
        assertThat(dataVolumes.getVolumes()[0], is(equalTo(new Volume("/volume"))));
        assertThat(dataVolumes.getVolumes()[1], is(equalTo(new Volume("/volume2"))));
    }

    @Test
    public void canCheckIfThereIsHostFileOrDirectoryThatHasToBeMountedInsideTheContainer() throws Exception {
        List<String> volumes = new ArrayList<>();
        DataVolumes dataVolumes = new DataVolumes("boohoo", volumes);
        assertThat(dataVolumes.hasBinds(), is(equalTo(false)));

        volumes.add("/host:/volume");
        dataVolumes = new DataVolumes("boohoo", volumes);
        assertThat(dataVolumes.hasBinds(), is(equalTo(true)));
    }

    @Test
    public void canAddEntriesForMoutingHostFileOrDirectoryInsideTheContainer() throws Exception {
        List<String> volumes = new ArrayList<>();
        DataVolumes dataVolumes = new DataVolumes("boohoo", volumes);
        assertThat(dataVolumes.hasBinds(), is(equalTo(false)));
        assertThat(dataVolumes.getBinds(), is(equalTo(null)));

        volumes.add("/host:/volume");
        dataVolumes = new DataVolumes("boohoo", volumes);
        assertThat(dataVolumes.hasBinds(), is(equalTo(true)));
        assertThat(dataVolumes.getBinds(), is(not(equalTo(null))));
        assertThat(dataVolumes.getBinds().length, is(equalTo(1)));
        assertThat(dataVolumes.getBinds()[0], is(equalTo(new Bind("/host", new Volume("/volume")))));

        volumes.add("/host2:/volume2");
        dataVolumes = new DataVolumes("boohoo", volumes);
        assertThat(dataVolumes.hasBinds(), is(equalTo(true)));
        assertThat(dataVolumes.getBinds(), is(not(equalTo(null))));
        assertThat(dataVolumes.getBinds().length, is(equalTo(2)));
        assertThat(dataVolumes.getBinds()[0], is(equalTo(new Bind("/host", new Volume("/volume")))));
        assertThat(dataVolumes.getBinds()[1], is(equalTo(new Bind("/host2", new Volume("/volume2")))));
    }
}
