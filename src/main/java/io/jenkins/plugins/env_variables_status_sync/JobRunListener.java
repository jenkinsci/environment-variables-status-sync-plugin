package io.jenkins.plugins.env_variables_status_sync;

import static io.jenkins.plugins.env_variables_status_sync.utils.Utils.getEnvVars;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.jenkins.plugins.env_variables_status_sync.enums.JobStatus;
import io.jenkins.plugins.env_variables_status_sync.utils.HttpClient;

/**
 * Author: kun.tang@daocloud.io
 * Date:2024/9/13
 * Time:18:30
 */
@Extension
public class JobRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onCompleted(Run<?, ?> run, @NonNull TaskListener listener) {
        sendStatus(run, listener, JobStatus.COMPLETE);
    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        sendStatus(run, listener, JobStatus.START);
    }

    private static void sendStatus(Run<?, ?> run, TaskListener listener, JobStatus jobStatus) {
        try {
            EnvVars vars = getEnvVars(run, listener, jobStatus);
            HttpClient.executeRequest(vars);
        } catch (Exception e) {
            listener.getLogger().println("Job Environment Variables Status Sync error:" + e.getMessage());
        }
    }
}
