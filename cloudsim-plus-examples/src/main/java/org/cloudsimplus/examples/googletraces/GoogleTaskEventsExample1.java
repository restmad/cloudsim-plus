/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples.googletraces;

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.TextTableColumn;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;
import org.cloudsimplus.traces.google.TaskEvent;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An example showing how to create Cloudlets (tasks) from a Google Task Events Trace
 * using a {@link GoogleTaskEventsTraceReader}.
 * The trace is located in resources/workload/google-traces/task-events-sample-1.csv.
 * Each line in the trace defines the scheduling of tasks (Cloudlets) inside a Datacenter.
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 4.0.0
 *
 * @todo A joint example that creates Hosts and Cloudlets from trace files will be useful.
 *
 * @todo See https://github.com/manoelcampos/cloudsim-plus/issues/151
 * @todo {@link CloudSimTags#CLOUDLET_FAIL} events aren't been processed.
 * @todo It has to be checked how to make the Cloudlet to be executed in the Host specified in the trace file.
 * @todo It has to be checked the task-usage trace files too.
 */
public class GoogleTaskEventsExample1 {
    private static final int HOSTS = 10;
    private static final int VMS = 8;
    private static final int HOST_PES = 8;
    private static final long HOST_RAM = 2048; //in Megabytes
    private static final long HOST_BW = 10000; //in Megabits/s
    private static final long HOST_STORAGE = 1000000; //in Megabytes
    private static final double HOST_MIPS = 1000;

    /**
     * Defines a negative length for Cloudlets created from the Google Task Events Trace file
     * so that they can run indefinitely until a {@link CloudSimTags#CLOUDLET_FINISH}
     * event is received by the {@link DatacenterBroker}.
     * Check out {@link Cloudlet#setLength(long)} for details.
     */
    private static final int  CLOUDLET_LENGTH = -10000;
    private static final long VM_PES = 4;
    private static final long VM_RAM = 500; //in Megabytes
    private static final long VM_BW = 100; //in Megabits/s
    private static final long VM_SIZE = 1000; //in Megabytes

    private final CloudSim simulation;
    private Collection<DatacenterBroker> brokers;
    private Datacenter datacenter;
    private List<Cloudlet> cloudlets;

    public static void main(String[] args) {
        new GoogleTaskEventsExample1();
    }

    private GoogleTaskEventsExample1() {
        Log.setLevel(Level.TRACE);

        simulation = new CloudSim();
        datacenter = createDatacenter();

        createCloudletsAndBrokersFromTraceFile();
        brokers.forEach(broker -> broker.submitVmList(createVms()));

        simulation.start();

        brokers.forEach(this::printCloudlets);
    }

    private void printCloudlets(final DatacenterBroker broker) {
        final String username = broker.getName().replace("Broker_", "");
        final List<Cloudlet> list = broker.getCloudletFinishedList();
        list.sort(Comparator.comparingInt(Cloudlet::getId));
        new CloudletsTableBuilder(list)
                .addColumn(0, new TextTableColumn("Job", "ID"), Cloudlet::getJobId)
                .addColumn(7, new TextTableColumn("VM Size", "MB"), this::getVmSize)
                .addColumn(8, new TextTableColumn("Cloudlet Size", "MB"), this::getCloudletSizeInMB)
                .addColumn(10, new TextTableColumn("Waiting Time", "Seconds").setFormat("%.0f"), Cloudlet::getWaitingTime)
                .setTitle("Simulation results for Broker representing the username " + username)
                .build();
    }

    private long getVmSize(final Cloudlet cloudlet) {
        return cloudlet.getVm().getStorage().getCapacity();
    }

    private long getCloudletSizeInMB(final Cloudlet cloudlet) {
        return (long)Conversion.bytesToMegaBytes(cloudlet.getFileSize());
    }

    private Datacenter createDatacenter() {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }

        return new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
    }

    private Host createHost() {
        final List<Pe> peList = createPesList(HOST_PES);
        final ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
        final ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
        final VmScheduler vmScheduler = new VmSchedulerTimeShared();
        final Host host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        host
            .setRamProvisioner(ramProvisioner)
            .setBwProvisioner(bwProvisioner)
            .setVmScheduler(vmScheduler);
        return host;
    }

    private List<Pe> createPesList(final int count) {
        final List<Pe> cpuCoresList = new ArrayList<>(count);
        for(int i = 0; i < count; i++){
            cpuCoresList.add(new PeSimple(HOST_MIPS, new PeProvisionerSimple()));
        }

        return cpuCoresList;
    }

    private List<Vm> createVms() {
        return IntStream.range(0, VMS).mapToObj(this::createVm).collect(Collectors.toList());
    }

    private Vm createVm(final int id) {
        return new VmSimple(1000, VM_PES)
            .setRam(VM_RAM).setBw(VM_BW).setSize(VM_SIZE)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());
    }

    /**
     * Creates a list of Cloudlets from a "task events" Google Cluster Data trace file.
     * The brokers that own each Cloudlet are defined by the username field
     * in the file. For each distinct username, a broker is created
     * and its name is defined based on the username.
     *
     * <p>
     * A {@link GoogleTaskEventsTraceReader} instance is used to read the file.
     * It requires a {@link Function}
     * that will be called internally to actually create the Cloudlets.
     * This function is the {@link #createCloudlet(TaskEvent)}.*
     * </p>
     */
    private void createCloudletsAndBrokersFromTraceFile() {
        final GoogleTaskEventsTraceReader reader =
            GoogleTaskEventsTraceReader.getInstance(
                simulation,
                "workload/google-traces/task-events-sample-1.csv",
                this::createCloudlet);

        /*The created Cloudlets are automatically submitted to their respective brokers,
        so you don't have to submit them manually.*/
        cloudlets = reader.process();
        brokers = reader.getBrokers();
    }

    /**
     * A method that is used to actually create each Cloudlet defined as a task in the
     * trace file.
     * The researcher can write his/her own code inside this method to define
     * how he/she wants to create the Cloudlet based on the trace data.
     *
     * @param event an object containing the trace line read, used to create the Cloudlet.
     * @return
     */
    private Cloudlet createCloudlet(final TaskEvent event) {
        /*
        The trace doesn't define the actual number of CPU cores (PEs) a Cloudlet will require,
        but just a percentage of the number of CPU cores that is required.
        This way, we have to compute the actual number of cores.
        This is different from the CPU UtilizationModel, which is defined
        in the "task usage" trace files.
        While such files are not processed, the CPU utilization is defined as full (100% of utilization).
        */
        final long pesNumber = (long) Math.ceil(event.getMaxCpuCoresPercent() * VM_PES);
        final UtilizationModel utilizationCpu = new UtilizationModelFull();

        final UtilizationModel utilizationRam = new UtilizationModelDynamic(event.getMaxRamPercent());
        final long sizeInBytes = (long) Math.ceil(Conversion.megaBytesToBytes(event.getMaxDiskSpacePercent() * VM_SIZE));
        return new CloudletSimple(CLOUDLET_LENGTH, pesNumber)
            .setFileSize(sizeInBytes)
            .setOutputSize(sizeInBytes)
            .setUtilizationModelBw(new UtilizationModelFull())
            .setUtilizationModelCpu(utilizationCpu)
            .setUtilizationModelRam(utilizationRam);
    }

}
