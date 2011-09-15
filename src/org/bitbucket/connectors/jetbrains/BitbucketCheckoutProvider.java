package org.bitbucket.connectors.jetbrains;

import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.util.SystemProperties;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.httpclient.URIException;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;
import org.bitbucket.connectors.jetbrains.ui.BitbucketCloneProjectDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.command.HgCloneCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.provider.HgCheckoutProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BitbucketCheckoutProvider implements CheckoutProvider {
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {
        List<RepositoryInfo> availableRepos = BitbucketUtil.getRepositories(project, false);
        if (availableRepos == null) {
            return;
        }

        if (availableRepos.isEmpty()) {
            Messages.showErrorDialog(project, BitbucketBundle.message("clone-err"), BitbucketBundle.message("cannot-clone"));
            return;
        }
        Collections.sort(availableRepos, new Comparator<RepositoryInfo>() {
            public int compare(final RepositoryInfo r1, final RepositoryInfo r2) {
                int res = r1.getOwner().compareTo(r2.getOwner());
                if (res != 0) {
                    return res;
                }
                return r1.getName().compareTo(r2.getName());
            }
        });

        String clonePath;
        String lastProjectLocation = GeneralSettings.getInstance().getLastProjectLocation();
        String userHome = SystemProperties.getUserHome();
        if (lastProjectLocation != null) {
            clonePath = lastProjectLocation.replace('/', File.separatorChar);
        } else {
            clonePath = userHome.replace('/', File.separatorChar) + File.separator +
                    ApplicationNamesInfo.getInstance().getLowercaseProductName() + "Projects";
        }
        File file = new File(clonePath);
        if (!file.exists() || !file.isDirectory()) {
            clonePath = userHome;
        }

        BitbucketCloneProjectDialog checkoutDialog = new BitbucketCloneProjectDialog(project, availableRepos);
        checkoutDialog.setSelectedPath(clonePath);
        checkoutDialog.show();
        if (!checkoutDialog.isOK()) {
            return;
        }

        File folder = new File(checkoutDialog.getSelectedPath());
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        String projectName = checkoutDialog.getProjectName();
        if (!StringUtil.isEmpty(projectName)) {
            folder = new File(folder, projectName);
            if (!folder.exists()) {
                if (!folder.mkdir()) {
                    return;
                }
            } else if (!folder.isDirectory()) {
                return;
            }
        }

        RepositoryInfo repository = checkoutDialog.getSelectedRepository();

        try {
            String repositoryUrl = repository != null ? repository.getCheckoutUrl() : checkoutDialog.getRepositoryUrl();
            BitbucketSettings settings = BitbucketSettings.getInstance();
            if (repository == null && repositoryUrl != null && repositoryUrl.startsWith("ssh:") && !BitbucketUtil.sshEnabled(project, settings.getLogin(), settings.getPassword())) {
                if (!BitbucketUtil.addSshKey(project, settings.getLogin(), settings.getPassword())) {
                    Messages.showErrorDialog(project, "Valid SSH key is required for SSH repository URL", "Checkout from Bitbucket");
                    return;
                }
            }
            checkout(project, repositoryUrl, folder.getPath(), listener);
        } catch (URIException e) {
            Messages.showErrorDialog(project, e.getMessage(), BitbucketBundle.message("url-encode-err"));
        }
    }

    private void checkout(final Project project, final String repositoryUrl, final String folder, final Listener listener) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });

        new Task.Backgroundable(project, BitbucketBundle.message("checkouting"), true) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                HgCloneCommand cmd = new HgCloneCommand(project);
                cmd.setRepositoryURL(repositoryUrl);
                cmd.setDirectory(folder);

                try {
                    HgCommandResult result = cmd.execute();

                    if (result == null) {
                        error(project, BitbucketBundle.message("clone-failed"), BitbucketBundle.message("clone-failed-unknown-err"));
                    } else if (result.getExitValue() != 0) {
                        error(project, BitbucketBundle.message("clone-failed"), BitbucketBundle.message("clone-failed-msg", repositoryUrl, result.getRawError() + "\n" + result.getRawOutput()));
                    } else {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            public void run() {
                                if (listener != null) {
                                    listener.directoryCheckedOut(new File(folder));
                                    listener.checkoutCompleted();
                                }
                            }
                        });
                    }
                } finally {
                    cleanupAuthDataFromHgrc(folder);
                }
            }
        }.queue();
    }

    private static void cleanupAuthDataFromHgrc(String folder) {
        try {
            for (Method method: HgCheckoutProvider.class.getDeclaredMethods()) {
                if ("cleanupAuthDataFromHgrc".equals(method.getName())) {
                    method.setAccessible(true);
                    method.invoke(null, folder);
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void error(final Project project, final String title, final String msg) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                Messages.showErrorDialog(project, msg, title);
            }
        });
    }

    public String getVcsName() {
        return "Bitbucket";
    }
}
