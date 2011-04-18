package org.bitbucket.connectors.jetbrains;


import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Icons;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.bitbucket.connectors.jetbrains.ui.BitbucketLoginDialog;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.zmlx.hg4idea.command.HgCommandResult;
import org.zmlx.hg4idea.command.HgPushCommand;
import org.zmlx.hg4idea.command.HgUrl;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitbucketUtil {

    public static final String BITBUCKET = "bitbucket.org";
    public static final Icon ICON = Icons.WEB_ICON;

    private static HttpClient getClient(String login, String password) {
        HttpClient client = new HttpClient();
        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(login, password);
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, cred);

        return client;
    }

    public static Element request(String username, String password, String url, boolean post, Map<String, String> params) throws IOException, JDOMException {
        url = "https://api." + BITBUCKET + "/1.0" + url + "?format=xml";

        HttpClient client = getClient(username, password);
        HttpMethod res;
        if (post) {
            res = new PostMethod(url);
            if (params != null) {
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry: params.entrySet()) {
                    pairs.add(new NameValuePair(entry.getKey(), entry.getValue()));
                }
                NameValuePair[] arr = new NameValuePair[pairs.size()];
                pairs.toArray(arr);
                ((PostMethod) res).setRequestBody(arr);
            }
        } else {
            res = new GetMethod(url);
        }
        client.executeMethod(res);
        InputStream stream = res.getResponseBodyAsStream();
        return new SAXBuilder(false).build(stream).getRootElement();
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
            Element element = request(username, password, "/user/repositories/", false, null);
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
            Element element = request(login, password, "/users/" + login, false, null);
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

    public static void executeWithProgressSynchronously(final Project project, final Runnable runnable) throws CancelledException {
        ProgressManager.getInstance().run(new Task.Modal(project, "Access to Bitbucket", true) {
            public void run(@NotNull ProgressIndicator indicator) {
                runnable.run();
            }

            @Override
            public void onCancel() {
                throw new CancelledException();
            }
        });
    }

    public static void share(final Project project, final VirtualFile root, final String name, final String description) {
        executeWithProgressSynchronously(project, new Runnable() {
            public void run() {
                BitbucketSettings settings = BitbucketSettings.getInstance();
                RepositoryInfo repository = createBitbucketRepository(settings.getLogin(), settings.getPassword(), name, description, true);
                if (repository != null) {
                    String repositoryUrl = repository.getCheckoutUrl(false);
                    try {
                        HgCommandResult result = new HgPushCommand(project, root, addCredentials(repositoryUrl)).execute();
                        setRepositoryDefaultPath(root, repositoryUrl);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Messages.showInfoMessage(project, "The project has been shared on the Bitbucket.", "Bitbucket share project");
    }

    private static void setRepositoryDefaultPath(VirtualFile root, String url) throws IOException {
        File hgrc = new File(new File(root.getPath(), ".hg"), "hgrc");
        if (!hgrc.exists()) {
            hgrc.createNewFile();
        }

        List<String> lines = new ArrayList<String>();
        List<String> pathLines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(hgrc));
        try {
            String line;
            boolean paths = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[")) {
                    paths = "[paths]".equals(line);
                    if (!paths) {
                        lines.add(line);
                    }
                    continue;
                }

                if (paths) {
                    if (!"default".equals(getKey(line))) {
                        pathLines.add(line);
                    }
                } else {
                    lines.add(line);
                }
            }
        } finally {
            reader.close();
        }

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(hgrc, false)));
        try {
            writer.println("[paths]");
            writer.println("default=" + url);

            for (String line: pathLines) {
                writer.println(line);
            }

            for (String line: lines) {
                writer.println(line);
            }
        } finally {
            writer.close();
        }
    }

    private static String getKey(String s) {
        String[] parts = s.split("=");
        return parts.length > 0 ? parts[0].trim() : null;
    }

    private static RepositoryInfo createBitbucketRepository(String login, String password, String name, String description, boolean isPrivate) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", name);
        params.put("description", description);
        if (isPrivate) {
            params.put("is_private", "True");
        }

        try {
            Element element = request(login, password, "/repositories/", true, params);
            return new RepositoryInfo(element);
        } catch (Exception e) {
            return null;
        }
    }

    public static String addCredentials(String repositoryUrl) {
        HgUrl url;
        try {
            url = new HgUrl(repositoryUrl);
            BitbucketSettings settings = BitbucketSettings.getInstance();
            url.setUsername(settings.getLogin());
            url.setPassword(settings.getPassword());
            return url.asString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
