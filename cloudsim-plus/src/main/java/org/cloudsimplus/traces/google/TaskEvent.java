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

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;

import java.util.function.Function;

/**
 * A data class to store the attributes to create a {@link Cloudlet},
 * according to the data read from a line inside a "task events" trace file.
 * In order to create such Cloudlets, the {@link GoogleTaskEventsTraceReader} requires
 * the developer to provide a {@link Function}
 * that creates Cloudlets according to the developer needs.
 *
 * <p>The {@link GoogleTaskEventsTraceReader} cannot create the Cloudlets itself
 * by hardcoding some simulation specific parameters such as the {@link UtilizationModel}
 * or cloudlet length. This way, it request a {@link Function} implemented
 * by the developer using the {@link GoogleTaskEventsTraceReader} class
 * that has the custom logic to create Cloudlets.
 * However, this developer's {@link Function} needs to receive
 * the task parameters read from the trace file such as
 * CPU, RAM and disk requirements and priority.
 * To avoid passing so many parameters to the developer's
 * Function, an instance of this class that wraps all these
 * parameters is used instead.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 4.0.0
 */
public final class TaskEvent {
    private int jobId;
    private int taskIndex;
    private int priority;
    private double cpuCoresPercent;
    private double ramPercent;
    private double diskSpacePercent;
    private int machineId;
    private String userName;
    private double timestamp;

    public int getPriority() {
        return priority;
    }

    protected TaskEvent setPriority(final int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Gets the maximum number of CPU cores
     * the task is permitted to use (in percentage from 0 to 1).
     * This percentage value can be used to compute the number of {@link Pe}s
     * the Cloudlet will require, based on the number of PEs of the Vm where the Cloudlet will be executed.
     *
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#RESOURCE_REQUEST_FOR_CPU_CORES
     */
    public double getCpuCoresPercent() {
        return cpuCoresPercent;
    }

    protected TaskEvent setCpuCoresPercent(final double cpuCoresPercent) {
        this.cpuCoresPercent = cpuCoresPercent;
        return this;
    }

    /**
     * Gets the maximum amount of RAM
     * the task is permitted to use (in percentage from 0 to 1).
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#RESOURCE_REQUEST_FOR_RAM
     */
    public double getRamPercent() {
        return ramPercent;
    }

    protected TaskEvent setRamPercent(final double ramPercent) {
        this.ramPercent = ramPercent;
        return this;
    }

    /**
     * Gets the maximum amount of local disk space
     * the task is permitted to use (in percentage from 0 to 1).
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#RESOURCE_REQUEST_FOR_LOCAL_DISK_SPACE
     */
    public double getDiskSpacePercent() {
        return diskSpacePercent;
    }

    /**
     * Gets the machineID that indicates the machine onto which the task was scheduled.
     * If the field is empty, -1 is returned instead.
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#MACHINE_ID
     */
    public int getMachineId() {
        return machineId;
    }

    protected TaskEvent setDiskSpacePercent(final double diskSpacePercent) {
        this.diskSpacePercent = diskSpacePercent;
        return this;
    }

    /**
     * Gets the hashed username provided as an opaque base64-encoded string that can be tested for equality.
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#USERNAME
     */
    public String getUserName() {
        return userName;
    }

    protected TaskEvent setMachineId(final int machineId) {
        this.machineId = machineId;
        return this;
    }

    /**
     * Gets the id of the job this task belongs to.
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#JOB_ID
     */
    public int getJobId() {
        return jobId;
    }

    protected TaskEvent setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Gets the task index within the job.
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#TASK_INDEX
     */
    public int getTaskIndex() {
        return taskIndex;
    }

    protected TaskEvent setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * An unique ID to be used to identify created Cloudlets.
     * The ID is composed of the {@link #getJobId() Job ID} concatenated with the {@link #getTaskIndex() Task Index}.
     * @return
     */
    public int getUniqueTaskId(){
        return Integer.valueOf(String.format("%d%d", jobId, taskIndex));
    }

    protected TaskEvent setTaskIndex(int taskIndex) {
        this.taskIndex = taskIndex;
        return this;
    }

    /**
     * Gets the time the event happened (converted to seconds).
     * @return
     * @see GoogleTaskEventsTraceReader.FieldIndex#TIMESTAMP
     */
    public double getTimestamp() {
        return timestamp;
    }

    protected TaskEvent setTimestamp(final double timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
