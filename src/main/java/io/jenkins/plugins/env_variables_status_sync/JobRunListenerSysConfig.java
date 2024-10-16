package io.jenkins.plugins.env_variables_status_sync;

import static io.jenkins.plugins.env_variables_status_sync.enums.Constants.*;
import static io.jenkins.plugins.env_variables_status_sync.utils.Utils.encoderPassword;

import hudson.BulkChange;
import hudson.Extension;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.Messages;
import io.jenkins.plugins.env_variables_status_sync.enums.HttpMethod;
import io.jenkins.plugins.env_variables_status_sync.model.HttpHeader;
import java.io.IOException;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Author: kun.tang@daocloud.io
 * Date:2024/9/14
 * Time:11:41
 */
@Getter
@Extension
@ToString
public class JobRunListenerSysConfig extends GlobalConfiguration {

    public JobRunListenerSysConfig() {
        load();
    }

    private String requestUrl;

    private List<HttpHeader> httpHeaders;

    private HttpMethod requestMethod;

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        try (BulkChange bc = new BulkChange(this)) {
            requestUrl = json.getString(FORM_KEY_REQUEST_URL);
            setRequestUrl(requestUrl);
            httpHeaders = req.bindJSONToList(HttpHeader.class, json.get(FORM_KEY_REQUEST_HEADERS));
            setHttpHeaders(httpHeaders);
            String httpMethod = json.getString(FORM_KEY_REQUEST_METHOD);
            setRequestMethod(HttpMethod.valueOf(httpMethod));
            bc.commit();
        } catch (IOException e) {
            throw new IllegalArgumentException(Messages.JobRunListenerSysConfig_abort_errors());
        }
        return true;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
        save();
    }

    public void setHttpHeaders(List<HttpHeader> httpHeaders) {
        encoderPassword(httpHeaders);
        this.httpHeaders = httpHeaders;
        save();
    }

    public void setRequestMethod(HttpMethod requestMethod) {
        this.requestMethod = requestMethod;
        save();
    }

    public ListBoxModel doFillHttpMethodItems() {
        ListBoxModel items = new ListBoxModel();
        if (requestMethod == null) {
            requestMethod = HttpMethod.POST; // 默认选择 POST 方法
        }
        for (HttpMethod method : HttpMethod.values()) {
            boolean isSelected = method.name().equals(requestMethod.name());
            items.add(new ListBoxModel.Option(method.name(), method.name(), isSelected));
        }
        return items;
    }
}
