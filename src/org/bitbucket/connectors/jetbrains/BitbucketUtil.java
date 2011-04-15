package org.bitbucket.connectors.jetbrains;


import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bitbucket.connectors.jetbrains.ui.BitbucketLoginDialog;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BitbucketUtil {

    public static final String BITBUCKET = "bitbucket.org";

    private static HttpClient getClient(String login, String password) {
        HttpClient client = new HttpClient();
        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(login, password);
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, cred);

        return client;
    }

    public static HttpMethod request(String username, String password, String url) throws IOException {
        HttpClient client = getClient(username, password);
        GetMethod get = new GetMethod("https://api." + BITBUCKET + "/1.0" + url + "?format=xml");
        client.executeMethod(get);
        return get;
    }

    public static List<RepositoryInfo> getRepositories(Project project, final boolean ownOnly) {
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        boolean logged;
        try {
            logged = executeWithProgressSynchronously(project, new Computable<Boolean>() {
                public Boolean compute() {
                    ProgressManager.getInstance().getProgressIndicator().setText("Login to Bitbucket...");
                    return testConnection(settings.getLogin(), settings.getPassword());
                }
            });
        } catch (CancelledException e) {
            return null;
        }

        if (!logged) {
            BitbucketLoginDialog dialog = new BitbucketLoginDialog(project);
            dialog.show();
            if (!dialog.isOK()) {
                return null;
            }
        }

        try {
            return executeWithProgressSynchronously(project, new Computable<List<RepositoryInfo>>() {
                public List<RepositoryInfo> compute() {
                    ProgressManager.getInstance().getProgressIndicator().setText("Getting list of repositories...");
                    return getRepositories(settings.getLogin(), settings.getPassword(), ownOnly);
                }
            });
        } catch (CancelledException e) {
            return null;
        }
    }

    private static List<RepositoryInfo> getRepositories(String username, String password, boolean ownOnly) {
        List<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>();
        try {
            HttpMethod method = request(username, password, "/user/repositories/");

            final Element element = new SAXBuilder(false).build(method.getResponseBodyAsStream()).getRootElement();
            for (Element res : (List<Element>) element.getChildren("resource")) {
                RepositoryInfo info = new RepositoryInfo(res);
                if (!ownOnly || info.getOwner().equals(username)) {
                    repositories.add(info);
                }
            }
            return repositories;
        } catch (Exception e) {
            // ignore
        }
        return repositories;
    }

    public static class CancelledException extends RuntimeException {
    }

    public static boolean checkCredentials(Project project, final String login, final String password) {
        if (login == null && password == null && areCredentialsEmpty()) {
            return false;
        }
        try {
            return executeWithProgressSynchronously(project, new Computable<Boolean>() {
                public Boolean compute() {
                    ProgressManager.getInstance().getProgressIndicator().setText("Trying to login to Bitbucket");
                    if (login != null && password != null) {
                        return testConnection(login, password);
                    }
                    BitbucketSettings settings = BitbucketSettings.getInstance();
                    return testConnection(settings.getLogin(), settings.getPassword());
                }
            });
        }
        catch (CancelledException e) {
            return false;
        }

    }

    public static boolean areCredentialsEmpty() {
        BitbucketSettings settings = BitbucketSettings.getInstance();
        return StringUtil.isEmptyOrSpaces(settings.getLogin()) || StringUtil.isEmptyOrSpaces(settings.getPassword());
    }

    public static boolean testConnection(final String login, final String password) {
        try {
            HttpMethod method = request(login, password, "/users/" + login);
            InputStream stream = method.getResponseBodyAsStream();
            Element element = new SAXBuilder(false).build(stream).getRootElement();
            return "response".equals(element.getName()) && element.getChild("repositories") != null;
        }
        catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public static <T> T executeWithProgressSynchronously(final Project project, final Computable<T> computable) throws CancelledException {
        final Ref<T> result = new Ref<T>();
        ProgressManager.getInstance().run(new Task.Modal(project, "Access to Bitbucket", true) {
            public void run(@NotNull ProgressIndicator indicator) {
                result.set(computable.compute());
            }

            @Override
            public void onCancel() {
                throw new CancelledException();
            }
        });
        return result.get();
    }
}
