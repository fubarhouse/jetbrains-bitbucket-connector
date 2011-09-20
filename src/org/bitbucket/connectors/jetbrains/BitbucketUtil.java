package org.bitbucket.connectors.jetbrains;


import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Consumer;
import com.intellij.util.SystemProperties;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;
import org.bitbucket.connectors.jetbrains.ui.BitbucketLoginDialog;
import org.bitbucket.connectors.jetbrains.ui.HtmlMessageDialog;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.command.HgPushCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class BitbucketUtil {
    public static final String BITBUCKET = "Bitbucket";
    public static final String BITBUCKET_DN = "bitbucket.org";
    public static final Icon ICON = IconLoader.getIcon("res/bitbucket.png", BitbucketUtil.class);

    private static HttpClient getClient(String login, String password) {
        HttpClient client = new HttpClient();
        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(login, password);
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, cred);

        return client;
    }

    public static Element request(String username, String password, String url, boolean post, Map<String, String> params) throws IOException, JDOMException {
        url = "https://api." + BITBUCKET_DN + "/1.0" + url + "?format=xml";

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

    /**
     * Returns list of repositories for logged user, or null if the login is cancelled.
     *
     * @param project project
     * @param ownOnly return own repositories only if true
     * @return list of repositories, or null on cancel
     */
    public static List<RepositoryInfo> getRepositories(Project project, final boolean ownOnly) {
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        boolean logged;
        try {
            logged = executeWithProgressSynchronously(project, new Computable<Boolean>() {
                public Boolean compute() {
                    ProgressManager.getInstance().getProgressIndicator().setText(BitbucketBundle.message("logging-bitbucket"));
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
                    ProgressManager.getInstance().getProgressIndicator().setText(BitbucketBundle.message("getting-repositories-list"));
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
                    ProgressManager.getInstance().getProgressIndicator().setText(BitbucketBundle.message("trying-login-bibucket"));
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
            Element element = request(login, password, "/emails/", false, null);
            List<Element> children = element.getChildren();
            return children.size() > 0 && children.get(0).getChildren().size() > 0;
        }
        catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public static boolean sshEnabled(final Project project, final String login, final String password) {
        return executeWithProgressSynchronously(project, new Computable<Boolean>() {
            public Boolean compute() {
                return sshEnabled(login, password);
            }
        });
    }

    private static boolean sshEnabled(final String login, final String password) {
        try {
            Element element = request(login, password, "/ssh-keys/", false, null);
            List<Element> children = element.getChildren();
            return children.size() > 0 && children.get(0).getChild("key") != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean addSshKey(final Project project, final String login, final String password) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                return file.isDirectory() || super.isFileVisible(file, showHiddenFiles);
            }
        };
        descriptor.setIsTreeRootVisible(true);
        String home = SystemProperties.getUserHome();
        VirtualFile root = null;
        if (home != null) {
            root = VirtualFileManager.getInstance().findFileByUrl("file://" + home);
            if (root != null) {
                VirtualFile ssh = root.findChild(".ssh"); // id_dsa.pub
                if (ssh == null) {
                    ssh = root.findChild("Application Data/SSH/UserKeys");
                }
                if (ssh != null) {
                    root = ssh;
                }
            }
        }
        descriptor.setTitle("Please choose SSH key");

        FileChooser.chooseFilesWithSlideEffect(descriptor, project, root, new Consumer< VirtualFile[]>() {
            public void consume(VirtualFile[] virtualFiles) {
                for (VirtualFile f: virtualFiles) {
                    try {
                        final String key = VfsUtil.loadText(f);
                        executeWithProgressSynchronously(project, new Computable<Boolean>() {
                            public Boolean compute() {
                                return addSshKey(login, password, key);
                            }
                        });
                    } catch (IOException e1) {
                        Messages.showErrorDialog("Can't read SSH key: " + f.getPath(), "SSH key");
                    }
                }
            }
        });
        return sshEnabled(login, password);
    }

    private static boolean addSshKey(final String login, final String password, final String key) {
        try {
            Element element = request(login, password, "/ssh-keys/", true, Collections.singletonMap("key", key));
            return element.getChildren("key").size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static <T> T executeWithProgressSynchronously(final Project project, final Computable<T> computable) throws CancelledException {
        final Ref<T> result = new Ref<T>();
        ProgressManager.getInstance().run(new Task.Modal(project, BitbucketBundle.message("access-bitbucket"), true) {
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                result.set(computable.compute());
            }

            @Override
            public void onCancel() {
                throw new CancelledException();
            }
        });
        return result.get();
    }

    public static void executeWithProgressSynchronously(final Project project, String title, final Runnable runnable) throws CancelledException {
        ProgressManager.getInstance().run(new Task.Modal(project, title, true) {
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                runnable.run();
            }

            @Override
            public void onCancel() {
                throw new CancelledException();
            }
        });
    }

    public static void share(final Project project, final VirtualFile root, final String name, final String description) {
        final RepositoryInfo[] repo = new RepositoryInfo[1];
        executeWithProgressSynchronously(project, BitbucketBundle.message("push-bitbucket", name), new Runnable() {
            public void run() {
                BitbucketSettings settings = BitbucketSettings.getInstance();
                RepositoryInfo repository = createBitbucketRepository(settings.getLogin(), settings.getPassword(), name, description, true);
                if (repository != null) {
                    try {
                        String repositoryUrl = repository.getCheckoutUrl();
                        new HgPushCommand(project, root, repositoryUrl).execute(new HgCommandResultHandler() {
                            public void process(@Nullable HgCommandResult result) {
                            }
                        });
                        setRepositoryDefaultPath(root, repositoryUrl);
                        repo[0] = repository;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        RepositoryInfo repository = repo[0];
        if (repository != null) {
            try {
                HtmlMessageDialog dialog = new HtmlMessageDialog(project, BitbucketBundle.message("project-shared", name, repository.getCheckoutUrl()), BitbucketBundle.message("share-project-on-bitbucket"));
                dialog.show();
            } catch (URIException e) {
                Messages.showErrorDialog(project, e.getMessage(), BitbucketBundle.message("url-encode-err"));
            }
        }
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
}
