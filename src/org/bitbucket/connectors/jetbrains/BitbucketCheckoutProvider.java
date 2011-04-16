package org.bitbucket.connectors.jetbrains;

import com.intellij.ide.GeneralSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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
import org.bitbucket.connectors.jetbrains.ui.BitbucketCloneProjectDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgCloneCommand;
import org.zmlx.hg4idea.command.HgCommandResult;
import org.zmlx.hg4idea.command.HgUrl;
import org.zmlx.hg4idea.provider.HgCheckoutProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BitbucketCheckoutProvider implements CheckoutProvider {
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {
        List<RepositoryInfo> availableRepos = BitbucketUtil.getRepositories(project, false);

        if (availableRepos.isEmpty()) {
            Messages.showErrorDialog(project, "You don't have any repository available on Bitbucket.\nOnly your own or shared repositories can be cloned.", "Cannot clone");
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

        RepositoryInfo repository = checkoutDialog.getSelectedRepository();
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

        String repositoryUrl = repository.getCheckoutUrl();
        repositoryUrl = BitbucketUtil.addCredentials(repositoryUrl);

        checkout(project, repositoryUrl, folder.getPath(), listener);
    }

    private void checkout(final Project project, final String repositoryUrl, final String folder, final Listener listener) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });

        new Task.Backgroundable(project, "Checkout...", true) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                HgCloneCommand cmd = new HgCloneCommand(project);
                cmd.setRepositoryURL(repositoryUrl);
                cmd.setDirectory(folder);

                try {
                    HgCommandResult result = cmd.execute();

                    if (result == null) {
                        error(project, "Clone failed", "Clone failed due to unknown error");
                    } else if (result.getExitValue() != 0) {
                        error(project, "Clone failed", "Clone from " + repositoryUrl + " failed.<br>" + result.getRawError());
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

    private static void error(Project project, String title, String msg) {
        Notifications.Bus.notify(new Notification(HgVcs.NOTIFICATION_GROUP_ID, title, msg, NotificationType.ERROR), project);
    }

    public String getVcsName() {
        return "Bitbucket";
    }
}
