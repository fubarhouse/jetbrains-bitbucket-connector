package org.bitbucket.connectors.jetbrains;

import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.util.SystemProperties;
import org.apache.commons.httpclient.URIException;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;
import org.bitbucket.connectors.jetbrains.ui.BitbucketCloneProjectDialog;
import org.bitbucket.connectors.jetbrains.vcs.GitHandler;
import org.bitbucket.connectors.jetbrains.vcs.HgHandler;
import org.bitbucket.connectors.jetbrains.vcs.VcsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
        String lastProjectLocation = GeneralSettings.getInstance().getLastProjectCreationLocation();
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
            File destFolder = new File(folder, projectName);
            if (!destFolder.exists()) {
                if (!destFolder.mkdir()) {
                    return;
                }
            } else if (!destFolder.isDirectory()) {
                return;
            }
        }

        RepositoryInfo repository = checkoutDialog.getSelectedRepository();

        try {
            String repositoryUrl;
            boolean isGit;
            if (repository != null) {
                repositoryUrl = repository.getCheckoutUrl();
                isGit = repository.isGit();
            } else {
                repositoryUrl = checkoutDialog.getRepositoryUrl();
                if (BitbucketUtil.isHttpUrl(repositoryUrl)) {
                    repositoryUrl = RepositoryInfo.addPassword(repositoryUrl, false);
                }
                isGit = GitHandler.isGitRepository(repositoryUrl);
            }
            final VcsHandler vcsHandler = isGit ? new GitHandler() : new HgHandler();
            vcsHandler.checkout(project, repositoryUrl, listener, checkoutDialog.getSelectedPath(), projectName);
        } catch (URIException e) {
            Messages.showErrorDialog(project, e.getMessage(), BitbucketBundle.message("url-encode-err"));
        }
    }

    public String getVcsName() {
        return BitbucketUtil.BITBUCKET;
    }
}
