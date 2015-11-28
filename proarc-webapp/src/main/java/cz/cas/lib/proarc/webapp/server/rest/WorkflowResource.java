/*
 * Copyright (C) 2015 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.webapp.server.rest;

import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.config.CatalogConfiguration;
import cz.cas.lib.proarc.common.workflow.WorkflowManager;
import cz.cas.lib.proarc.common.workflow.model.Job;
import cz.cas.lib.proarc.common.workflow.model.JobFilter;
import cz.cas.lib.proarc.common.workflow.model.JobView;
import cz.cas.lib.proarc.common.workflow.model.Material;
import cz.cas.lib.proarc.common.workflow.model.MaterialFilter;
import cz.cas.lib.proarc.common.workflow.model.MaterialView;
import cz.cas.lib.proarc.common.workflow.model.Task;
import cz.cas.lib.proarc.common.workflow.model.TaskFilter;
import cz.cas.lib.proarc.common.workflow.model.TaskParameterFilter;
import cz.cas.lib.proarc.common.workflow.model.TaskParameterView;
import cz.cas.lib.proarc.common.workflow.model.TaskView;
import cz.cas.lib.proarc.common.workflow.model.WorkflowModelConsts;
import cz.cas.lib.proarc.common.workflow.profile.JobDefinition;
import cz.cas.lib.proarc.common.workflow.profile.JobDefinitionView;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowDefinition;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowProfileConsts;
import cz.cas.lib.proarc.common.workflow.profile.WorkflowProfiles;
import cz.cas.lib.proarc.webapp.shared.rest.WorkflowResourceApi;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * It allows to manage workflow remotely.
 *
 * @author Jan Pokorsky
 */
@Path(WorkflowResourceApi.PATH)
public class WorkflowResource {

    private static final Logger LOG = Logger.getLogger(WorkflowResource.class.getName());

    private final SessionContext session;
    private final HttpHeaders httpHeaders;
    private final WorkflowManager workflowManager;
    private final WorkflowProfiles workflowProfiles;
    private final AppConfiguration appConfig;

    public WorkflowResource(
            @Context HttpHeaders httpHeaders,
            @Context HttpServletRequest httpRequest
    ) throws AppConfigurationException {
        this.session = SessionContext.from(httpRequest);
        this.httpHeaders = httpHeaders;
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowProfiles = WorkflowProfiles.getInstance();
        this.appConfig = AppConfigurationFactory.getInstance().defaultInstance();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<JobView> getJob(
            @QueryParam(WorkflowModelConsts.JOB_FILTER_ID) BigDecimal id,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_CREATED) List<String> created,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_LABEL) String label,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_MODIFIED) List<String> modified,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_PRIORITY) Integer priority,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_PROFILENAME) String profileName,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_STATE) Job.State state,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_OWNERID) BigDecimal userId,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_OFFSET) int startRow,
            @QueryParam(WorkflowModelConsts.JOB_FILTER_SORTBY) String sortBy
    ) {
        int pageSize = 100;
        JobFilter filter = new JobFilter();
        filter.setLocale(session.getLocale(httpHeaders));
        filter.setMaxCount(pageSize);
        filter.setOffset(startRow);
        filter.setSortBy(sortBy);

        filter.setId(id);
        filter.setCreated(created);
        filter.setLabel(label);
        filter.setModified(modified);
        filter.setPriority(priority);
        filter.setProfileName(profileName);
        filter.setState(state);
        filter.setUserId(userId);
        try {
            List<JobView> jobs = workflowManager.findJob(filter);
            int resultSize = jobs.size();
            int endRow = startRow + resultSize;
            int total = (resultSize != pageSize) ? endRow : endRow + 1;
            return new SmartGwtResponse<JobView>(
                    SmartGwtResponse.STATUS_SUCCESS, startRow, endRow, total, jobs);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return SmartGwtResponse.asError(ex.getMessage());
        }
    }

    /**
     * Creates a new workflow job.
     * @param profileName profile name
     * @param metadata MODS
     * @param catalogId catalog ID
     * @return the job
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<JobView> addJob(
            @FormParam(WorkflowResourceApi.NEWJOB_PROFILE) String profileName,
            @FormParam(WorkflowResourceApi.NEWJOB_METADATA) String metadata,
            @FormParam(WorkflowResourceApi.NEWJOB_CATALOGID) String catalogId
    ) {
        if (metadata == null) {
            return SmartGwtResponse.asError(WorkflowResourceApi.NEWJOB_METADATA + " - missing value! ");
        }
        CatalogConfiguration catalog = appConfig.getCatalogs().findConfiguration(catalogId);
        if (catalog == null) {
            return SmartGwtResponse.asError(WorkflowResourceApi.NEWJOB_CATALOGID + " - invalid value! " + catalogId);
        }
        WorkflowDefinition profiles = workflowProfiles.getProfiles();
        if (profiles == null) {
            return profileError();
        }
        JobDefinition profile = workflowProfiles.getProfile(profiles, profileName);
        if (profile == null) {
            return SmartGwtResponse.asError(WorkflowResourceApi.NEWJOB_PROFILE + " - invalid value! " + profileName);
        }
        try {
            Job job = workflowManager.addJob(profile, metadata, catalog, session.getUser());
            JobFilter filter = new JobFilter();
            filter.setLocale(session.getLocale(httpHeaders));
            filter.setId(job.getId());
            List<JobView> views = workflowManager.findJob(filter);
            return new SmartGwtResponse<JobView>(views);
        } catch (Throwable ex) {
            LOG.log(Level.SEVERE,
                    WorkflowResourceApi.NEWJOB_PROFILE + ":" + profileName
                    + ", " + WorkflowResourceApi.NEWJOB_CATALOGID + ":" + catalogId
                    + ", " + WorkflowResourceApi.NEWJOB_METADATA + ":\n" + metadata,
                    ex);
            return SmartGwtResponse.asError(ex.getMessage());
        }
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<JobView> updateJob(
            @FormParam(WorkflowModelConsts.JOB_ID) BigDecimal id,
            @FormParam(WorkflowModelConsts.JOB_LABEL) String label,
            @FormParam(WorkflowModelConsts.JOB_NOTE) String note,
            @FormParam(WorkflowModelConsts.JOB_FINANCED) String financed,
            @FormParam(WorkflowModelConsts.JOB_OWNERID) BigDecimal userId,
            @FormParam(WorkflowModelConsts.JOB_PRIORITY) Integer priority,
            @FormParam(WorkflowModelConsts.JOB_STATE) Job.State state,
            @FormParam(WorkflowModelConsts.JOB_TIMESTAMP) long timestamp
    ) {
        if (id == null) {
            return SmartGwtResponse.asError("Missing job ID!");
        }
        WorkflowDefinition profiles = workflowProfiles.getProfiles();
        if (profiles == null) {
            return profileError();
        }
        Job job = workflowManager.getJob(id);
        if (job == null) {
            return SmartGwtResponse.asError("Unknown job ID: " + id);
        }
        job.setFinanced(financed);
        job.setLabel(label);
        job.setNote(note);
        job.setOwnerId(userId);
        if (priority != null) {
            job.setPriority(priority);
        }
        job.setState(state);
        job.setTimestamp(new Timestamp(timestamp));
        workflowManager.updateJob(job);

        JobFilter jobFilter = new JobFilter();
        jobFilter.setId(id);
        jobFilter.setLocale(session.getLocale(httpHeaders));
        List<JobView> result = workflowManager.findJob(jobFilter);
        return new SmartGwtResponse<JobView>(result);
    }

    @Path(WorkflowResourceApi.TASK_PATH)
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<TaskView> getTask(
            @QueryParam(WorkflowModelConsts.TASK_FILTER_CREATED) List<String> created,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_ID) BigDecimal id,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_JOBID) BigDecimal jobId,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_MODIFIED) List<String> modified,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_PRIORITY) Integer priority,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_PROFILENAME) List<String> profileName,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_STATE) Task.State state,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_OWNERID) BigDecimal userId,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_OFFSET) int startRow,
            @QueryParam(WorkflowModelConsts.TASK_FILTER_SORTBY) String sortBy
    ) {
        int pageSize = 100;
        TaskFilter filter = new TaskFilter();
        filter.setLocale(session.getLocale(httpHeaders));
        filter.setMaxCount(pageSize);
        filter.setOffset(startRow);
        filter.setSortBy(sortBy);

        filter.setCreated(created);
        filter.setId(id);
        filter.setJobId(jobId);
        filter.setModified(modified);
        filter.setPriority(priority);
        filter.setProfileName(profileName);
        filter.setState(state);
        filter.setUserId(userId);
        WorkflowDefinition workflow = workflowProfiles.getProfiles();
        if (workflow == null) {
            return profileError();
        }
        try {
            List<TaskView> tasks = workflowManager.tasks().findTask(filter, workflow);
            int resultSize = tasks.size();
            int endRow = startRow + resultSize;
            int total = (resultSize != pageSize) ? endRow : endRow + 1;
            return new SmartGwtResponse<TaskView>(
                    SmartGwtResponse.STATUS_SUCCESS, startRow, endRow, total, tasks);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return SmartGwtResponse.asError(ex.getMessage());
        }
    }

    @Path(WorkflowResourceApi.TASK_PATH)
    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<TaskView> addTask(
            @FormParam(WorkflowModelConsts.TASK_JOBID) BigDecimal jobId,
            @FormParam(WorkflowModelConsts.TASK_PROFILENAME) String taskName
    ) {
        if (jobId == null || taskName == null) {
            return SmartGwtResponse.asError("Invalid parameters!");
        }
        WorkflowDefinition workflow = workflowProfiles.getProfiles();
        if (workflow == null) {
            return profileError();
        }
        Task updatedTask = workflowManager.tasks().addTask(jobId, taskName, workflow, session.getUser());
        TaskFilter taskFilter = new TaskFilter();
        taskFilter.setId(updatedTask.getId());
        taskFilter.setLocale(session.getLocale(httpHeaders));
        List<TaskView> result = workflowManager.tasks().findTask(taskFilter, workflow);
        return new SmartGwtResponse<TaskView>(result);
    }

    @Path(WorkflowResourceApi.TASK_PATH)
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<TaskView> updateTask(TaskUpdate task) {
        if (task == null) {
            return SmartGwtResponse.asError("No task!");
        }
        WorkflowDefinition workflow = workflowProfiles.getProfiles();
        if (workflow == null) {
            return profileError();
        }
        Task updatedTask = workflowManager.tasks().updateTask(task, task.params, workflow);
        TaskFilter taskFilter = new TaskFilter();
        taskFilter.setId(updatedTask.getId());
        taskFilter.setLocale(session.getLocale(httpHeaders));
        List<TaskView> result = workflowManager.tasks().findTask(taskFilter, workflow);
        return new SmartGwtResponse<TaskView>(result);
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class TaskUpdate extends Task {
        @XmlElement(name = WorkflowModelConsts.TASK_PARAMETERS)
        public Map<String, Object> params;
    }

    @Path(WorkflowResourceApi.MATERIAL_PATH)
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<MaterialView> getMaterial(
            @QueryParam(WorkflowModelConsts.MATERIALFILTER_ID) BigDecimal id,
            @QueryParam(WorkflowModelConsts.MATERIALFILTER_JOBID) BigDecimal jobId,
            @QueryParam(WorkflowModelConsts.MATERIALFILTER_TASKID) BigDecimal taskId,
            @QueryParam(WorkflowModelConsts.MATERIALFILTER_OFFSET) int startRow,
            @QueryParam(WorkflowModelConsts.MATERIALFILTER_SORTBY) String sortBy
    ) {
        int pageSize = 100;
        MaterialFilter filter = new MaterialFilter();
        filter.setLocale(session.getLocale(httpHeaders));
        filter.setMaxCount(pageSize);
        filter.setOffset(startRow);
        filter.setSortBy(sortBy);

        filter.setId(id);
        filter.setJobId(jobId);
        filter.setTaskId(taskId);
        try {
            List<MaterialView> mvs = workflowManager.findMaterial(filter);
            int resultSize = mvs.size();
            int endRow = startRow + resultSize;
            int total = (resultSize != pageSize) ? endRow : endRow + 1;
            return new SmartGwtResponse<MaterialView>(
                    SmartGwtResponse.STATUS_SUCCESS, startRow, endRow, total, mvs);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return SmartGwtResponse.asError(ex.getMessage());
        }
    }

    @Path(WorkflowResourceApi.MATERIAL_PATH)
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<MaterialView> updateMaterial(
            MaterialView mv
    ) {
        if (mv == null || mv.getId() == null) {
            return SmartGwtResponse.asError("Invalid parameters!");
        }
        WorkflowDefinition workflow = workflowProfiles.getProfiles();
        if (workflow == null) {
            return profileError();
        }
        Material updateMaterial = workflowManager.updateMaterial(mv);
        MaterialFilter filter = new MaterialFilter();
        filter.setLocale(session.getLocale(httpHeaders));
        filter.setId(updateMaterial.getId());
        filter.setJobId(mv.getJobId());
        filter.setTaskId(mv.getTaskId());
        List<MaterialView> result = workflowManager.findMaterial(filter);
        return new SmartGwtResponse<MaterialView>(result);
    }

    @Path(WorkflowResourceApi.PARAMETER_PATH)
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public SmartGwtResponse<TaskParameterView> getParameter(
            @QueryParam(WorkflowModelConsts.PARAMETERPROFILE_TASKID) BigDecimal taskId
    ) {
        if (taskId == null) {
            return SmartGwtResponse.asError("taskId is required!");
        }
        int pageSize = 100;
        TaskParameterFilter filter = new TaskParameterFilter();
        filter.setLocale(session.getLocale(httpHeaders));
        filter.setMaxCount(pageSize);
        filter.setOffset(0);

        filter.setTaskId(taskId);
        try {
            List<TaskParameterView> params = workflowManager.findParameter(filter);
            return new SmartGwtResponse<TaskParameterView>(params);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return SmartGwtResponse.asError(ex.getMessage());
        }
    }

    /**
     * Gets workflow profiles defined with {@link WorkflowProfiles}
     * @param name a profile name filter
     * @param disabled an availability filter
     * @return the list of profiles
     */
    @Path(WorkflowResourceApi.PROFILE_PATH)
    @Produces({MediaType.APPLICATION_JSON})
    @GET
    public SmartGwtResponse<JobDefinitionView> getProfiles(
            @QueryParam(WorkflowProfileConsts.NAME) String name,
            @QueryParam(WorkflowProfileConsts.DISABLED) Boolean disabled
    ) {
        WorkflowDefinition workflowDefinition = workflowProfiles.getProfiles();
        if (workflowDefinition == null) {
            return profileError();
        }
        String lang = session.getLocale(httpHeaders).getLanguage();
        ArrayList<JobDefinitionView> profiles = new ArrayList<JobDefinitionView>();
        for (JobDefinition job : workflowDefinition.getJobs()) {
            if ((name == null || name.equals(job.getName()))
                    && (disabled == null || disabled == job.isDisabled())) {
                profiles.add(new JobDefinitionView(job, lang));
            }
        }
        return new SmartGwtResponse<JobDefinitionView>(profiles);
    }

    private static <T> SmartGwtResponse<T> profileError() {
        return SmartGwtResponse.asError("Invalid workflow.xml! Check server configuration.");
    }

}
