package io.jenkins.plugins.env_variables_status_sync;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.WorkspaceListener;
import io.jenkins.plugins.env_variables_status_sync.enums.ConstantsEnums;
import io.jenkins.plugins.env_variables_status_sync.enums.JobStatus;
import io.jenkins.plugins.env_variables_status_sync.utils.HttpClient;

/**
 * Author: kun.tang@daocloud.io
 * Date:2024/10/16
 * Time:17:23
 */
@Extension
public class JobWorkspaceListener extends WorkspaceListener {

    @Override
    public void beforeUse(AbstractBuild b, FilePath workspace, BuildListener listener) {
        super.beforeUse(b, workspace, listener);
        try {
            EnvVars envVars = b.getEnvironment(listener);
            envVars.put(ConstantsEnums.JOB_EXECUTE_STATUS.getLowCase(), JobStatus.RUNNING.name());
            envVars.put(ConstantsEnums.BUILD_NUMBER.getLowCase(), b.getNumber() + "");
            envVars.put(ConstantsEnums.JOB_EXECUTE_STATUS.name(), JobStatus.RUNNING.name());
            HttpClient.executeRequest(envVars);
        } catch (Exception e) {
            listener.getLogger().println("Job Environment Variables Status Sync error:" + e.getMessage());
        }
    }
}
