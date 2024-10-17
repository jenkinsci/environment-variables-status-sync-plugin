package io.jenkins.plugins.env_variables_status_sync.utils;

import static io.jenkins.plugins.env_variables_status_sync.utils.Utils.decoderPassword;
import static io.jenkins.plugins.env_variables_status_sync.utils.Utils.encoderPassword;

import com.alibaba.fastjson2.JSONObject;
import hudson.ProxyConfiguration;
import io.jenkins.plugins.env_variables_status_sync.JobRunListenerSysConfig;
import io.jenkins.plugins.env_variables_status_sync.enums.HttpMethod;
import io.jenkins.plugins.env_variables_status_sync.model.HttpHeader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

/**
 * Author: kun.tang@daocloud.io
 * Date:2024/9/18
 * Time:17:20
 */
@Slf4j
public class HttpClient {

    public static void executeRequest(Map<String, String> requestMap) throws Exception {
        var sysConfig = GlobalConfiguration.all().get(JobRunListenerSysConfig.class);
        assert sysConfig != null;
        String url = sysConfig.getRequestUrl();
        var method = sysConfig.getRequestMethod();
        OkHttpClient client = createHttpClientWithProxy();
        RequestBody requestBody = null;
        if (method == HttpMethod.GET) {
            String params = convertMapToRequestParam(requestMap);
            url += "?" + params;
        } else {
            var requestParams = JSONObject.from(requestMap);
            requestBody = RequestBody.create(requestParams.toJSONString(), MediaType.parse("application/json"));
        }
        decoderPassword(sysConfig.getHttpHeaders());
        Request request = buildRequest(url, method, sysConfig.getHttpHeaders(), requestBody);
        assert request != null;
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Job Environment Variables Status Sync send msg success : {}", response.body());
            } else {
                log.error("Job Environment Variables Status Sync send msg failed : {}", response.code());
            }
        } catch (Exception e) {
            log.error("Job Environment Variables Status Sync requestMap : {} ,sysConfig : {} ,error : {}", requestMap, sysConfig,e.getMessage());
        }

        encoderPassword(sysConfig.getHttpHeaders());
    }

    private static Request buildRequest(
            String url, HttpMethod method, List<HttpHeader> headers, RequestBody requestBody) {
        if (null != url && !url.isEmpty()) {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            // 根据 HttpMethod 枚举设置请求方式
            switch (method) {
                case GET:
                    requestBuilder.get();
                    break;
                case POST:
                    requestBuilder.post(requestBody);
                    break;
                case PUT:
                    requestBuilder.put(requestBody);
                    break;
                case DELETE:
                    if (requestBody != null) {
                        requestBuilder.delete(requestBody);
                    } else {
                        requestBuilder.delete();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown HTTP method : " + method);
            }
            if (null != headers && !headers.isEmpty()) {
                for (HttpHeader header : headers) {
                    if (null != header.getHeaderKey() && null != header.getHeaderValue()) {
                        requestBuilder.addHeader(header.getHeaderKey(), header.getHeaderValue());
                    }
                }
            }
            return requestBuilder.build();
        }
        return null;
    }

    private static String convertMapToRequestParam(Map<String, String> requestMap) throws UnsupportedEncodingException {
        StringBuilder requestParams = new StringBuilder();

        Set<Map.Entry<String, String>> entrySet = requestMap.entrySet();
        boolean isFirst = true;

        for (Map.Entry<String, String> entry : entrySet) {
            if (!isFirst) {
                requestParams.append("&");
            } else {
                isFirst = false;
            }
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);

            requestParams.append(key).append("=").append(value);
        }
        return requestParams.toString();
    }

    private static OkHttpClient createHttpClientWithProxy() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 获取 Jenkins 的代理配置
        ProxyConfiguration proxyConfig = Jenkins.get().proxy;
        if (proxyConfig != null && proxyConfig.name != null) {
            // 配置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyConfig.name, proxyConfig.port));
            builder.proxy(proxy);

            // 如果代理需要身份验证，配置认证信息
            if (proxyConfig.getUserName() != null) {
                Authenticator proxyAuthenticator = (route, response) -> {
                    String credential =
                            Credentials.basic(proxyConfig.getUserName(), proxyConfig.getPassword());
                    return response.request()
                            .newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
                builder.proxyAuthenticator(proxyAuthenticator);
            }
        }

        return builder.build();
    }
}
