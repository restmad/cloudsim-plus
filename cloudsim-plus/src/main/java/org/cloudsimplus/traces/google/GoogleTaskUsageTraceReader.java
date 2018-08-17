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
package org.cloudsimplus.traces.google;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudsimplus.listeners.EventInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * Process "task usage" trace files from
 * <a href="https://github.com/google/cluster-data/blob/master/ClusterData2011_2.md">Google Cluster Data</a>
 * to change the resource utilization of {@link Cloudlet}s.
 * The trace files are the ones inside the task_usage sub-directory of downloaded Google traces.
 * The instructions to download the traces are provided in the link above.
 *
 * <p>A spreadsheet that makes it easier to understand the trace files structure is provided
 * in docs/google-cluster-data-samples.xlsx</p>
 *
 * <p>The documentation for fields and values were obtained from the Google Cluster trace documentation in the link above.
 * It's strongly recommended to read such a documentation before trying to use this class.</p>
 *
 * @see #process()
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 4.0.0
 */
public final class GoogleTaskUsageTraceReader extends GoogleTraceReaderAbstract<Cloudlet> {
    private final List<Cloudlet> createdCloudlets;
    private final List<CloudSimEvent> cloudletUsageChangeEvents;

    /**
     * The index of each field in the trace file.
     */
    public enum FieldIndex implements TraceField<GoogleTaskUsageTraceReader> {
        /**
         * 0: The index of the field containing the time the event happened (stored in microsecond
         * but converted to seconds when read from the file).
         */
        START_TIME{
            /**
             * Gets the timestamp converted to seconds.
             * @param reader the reader for the trace file
             * @return
             */
            @Override
            public Double getValue(final GoogleTaskUsageTraceReader reader) {
                return Conversion.microToSeconds(reader.getFieldIntValue(this));
            }
        },

        /**
         * 2: The index of the field containing the id of the job this task belongs to.
         */
        JOB_ID{
            @Override
            public Integer getValue(final GoogleTaskUsageTraceReader reader) {
                return reader.getFieldIntValue(this);
            }
        },

        /**
         * 3: The index of the field containing the task index within the job.
         */
        TASK_INDEX{
            @Override
            public Integer getValue(final GoogleTaskUsageTraceReader reader) {
                return reader.getFieldIntValue(this);
            }
        },

        /**
         * 4: The index of the field containing the machineID.
         * If the field is present, indicates the machine onto which the task was scheduled,
         * otherwise, the reader will return -1 as default value.
         */
        MACHINE_ID{
            @Override
            public Integer getValue(final GoogleTaskUsageTraceReader reader) {
                return reader.getFieldIntValue(this, -1);
            }
        }
    }

    private CloudSim simulation;

    /**
     * Gets a {@link GoogleTaskUsageTraceReader} instance to read a "task usage" trace file
     * inside the <b>application's resource directory</b>.
     *
     * @param simulation the simulation instance that the created tasks and brokers will belong to.
     * @param filePath the workload trace <b>relative file name</b> in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @throws IllegalArgumentException when the trace file name is null or empty
     * @throws UncheckedIOException     when the file cannot be accessed (such as when it doesn't exist)
     */
    public static GoogleTaskUsageTraceReader getInstance(
        final CloudSim simulation,
        final String filePath)
    {
        final InputStream reader = ResourceLoader.getInputStream(GoogleTaskUsageTraceReader.class, filePath);
        return new GoogleTaskUsageTraceReader(simulation, filePath, reader);
    }

    /**
     * Instantiates a {@link GoogleTaskUsageTraceReader} to read a "task usage" trace file.
     *
     * @param simulation the simulation instance that the created tasks and brokers will belong to.
     * @param filePath               the workload trace <b>relative file name</b> in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @throws IllegalArgumentException when the trace file name is null or empty
     * @throws UncheckedIOException     when the file cannot be accessed (such as when it doesn't exist)
     */
    public GoogleTaskUsageTraceReader(
        final CloudSim simulation,
        final String filePath) throws FileNotFoundException
    {
        this(simulation, filePath, new FileInputStream(filePath));
    }

    /**
     * Instantiates a {@link GoogleTaskUsageTraceReader} to read a "task usage" from a given InputStream.
     *
     * @param simulation the simulation instance that the created tasks and brokers will belong to.
     * @param filePath               the workload trace <b>relative file name</b> in one of the following formats: <i>ASCII text, zip, gz.</i>
     * @param reader                 a {@link InputStream} object to read the file
     * @throws IllegalArgumentException when the trace file name is null or empty
     * @throws UncheckedIOException     when the file cannot be accessed (such as when it doesn't exist)
     */
    private GoogleTaskUsageTraceReader(
        final CloudSim simulation,
        final String filePath,
        final InputStream reader)
    {
        super(filePath, reader);
        this.simulation = requireNonNull(simulation);
        cloudletUsageChangeEvents = new ArrayList<>();
        createdCloudlets = new ArrayList<>();
    }

    /**
     * Process the {@link #getFilePath() trace file} request to change the resource usage of {@link Cloudlet}s
     * as described in the file. It returns the List of all processed {@link Cloudlet}s.
     *
     * @return the List of all processed {@link Cloudlet}s
     */
    @Override
    public List<Cloudlet> process() {
        return super.process();
    }

    /**
     * There is not pre-process for this implementation.
     */
    @Override
    protected void preProcess(){/**/}

    @Override
    protected void postProcess(){
        simulation.addOnSimulationStartListener(this::onSimulationStart);
    }

    /**
     * Adds an event listener that is notified when the simulation starts,
     * so that the messages to change Cloudlet resource usage are sent.
     *
     * @param info the simulation start event information
     */
    private void onSimulationStart(final EventInfo info) {
        cloudletUsageChangeEvents.forEach(evt -> evt.getSource().schedule(evt));
    }

    @Override
    protected boolean processParsedLineInternal() {
        return false;
    }

    protected TaskEvent createTaskEventFromTraceLine() {
        return null;
    }

    /**
     * Send a message to the broker to request change in a Cloudlet status,
     * using some tags from {@link CloudSimTags} such as {@link CloudSimTags#CLOUDLET_READY}.
     * @param cloudletLookupFunction a {@link BiFunction} that receives the broker to find the Cloudlet into
     *                               and the unique ID of the Cloudlet (task),
     *                               so that the Cloudlet status can be changed
     * @param tag a tag from the {@link CloudSimTags} used to send a message to request the Cloudlet status change,
     *            such as {@link CloudSimTags#CLOUDLET_FINISH}
     * @return true if the request was created, false otherwise
     */
    protected boolean requestCloudletUsageChange(
        final BiFunction<DatacenterBroker, Integer, Optional<Cloudlet>> cloudletLookupFunction,
        final int tag)
    {
        return false;
    }

    /**
     * Adds the events to request to change the status and attributes of a Cloudlet to the
     * list of events to send to the Cloudlet's broker.
     *
     * @param statusChangeSimEvt the {@link CloudSimEvent} to be sent requesting the change in a Cloudlet's status,
     *                           where the data of the event is the Cloudlet to be changed.
     * @param taskEvent the task event read from the trace file, containing
     *                  the status and the attributes to change in the Cloudlet
     * @return
     */
    private Cloudlet addCloudletUsageChangeEvents(final CloudSimEvent statusChangeSimEvt, final TaskEvent taskEvent){
        return null;
    }

}
