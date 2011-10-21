package org.bitbucket.connectors.jetbrains.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.GitCommand;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitSimpleHandler;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * User: leha2000
 * Date: Oct 14, 2011
 * Time: 11:35:12 AM
 */
public class GitHandler implements VcsHandler {

    public boolean checkout(Project project, String folder, String repositoryUrl) {
        GitSimpleHandler handler = new GitSimpleHandler(project, new File(folder), GitCommand.CLONE);
        handler.addParameters(repositoryUrl);
        handler.runInCurrentThread(null);
        return true;
    }

    public static boolean isGitRepository(String url) {
        return url != null && url.endsWith(".git");
    }

    public boolean push(Project project, VirtualFile root, String repositoryUrl) {
        if (BitbucketUtil.isHttpUrl(repositoryUrl)) {
            if (!disableHttpSslCheck(project, root)) {
                return false;
            }
        }

        GitLineHandler handler = new GitLineHandler(project, root, GitCommand.PUSH);
        handler.addParameters("origin", "master");
        Collection<VcsException> err = GitHandlerUtil.doSynchronouslyWithExceptions(handler);
        return err.isEmpty();
    }

    private boolean disableHttpSslCheck(Project project, VirtualFile root) {
        return execute(project, root, GitCommand.CONFIG, "--global", "http.sslverify", "false");
    }

    public void setRepositoryDefaultUrl(Project project, VirtualFile root, String repositoryUrl) throws IOException {
        execute(project, root, GitCommand.REMOTE, "rm", "origin");
        execute(project, root, GitCommand.REMOTE, "add", "origin", repositoryUrl);
    }

    public VirtualFile getRepositoryRoot(VirtualFile folder) {
        return folder != null ? GitUtil.getGitRootOrNull(new File(folder.getPath())) : null;
    }

    public boolean initRepository(Project project, VirtualFile root) {
        if (!execute(project, root, GitCommand.INIT)) {
            return false;
        }
        GitLineHandler h = new GitLineHandler(project, root, GitCommand.ADD);
        h.addParameters(".");
        GitHandlerUtil.doSynchronously(h, BitbucketBundle.message("create-local-repository"), BitbucketBundle.message("share-project-on-bitbucket"));
        if (!h.errors().isEmpty()) {
            return false;
        }

        h = new GitLineHandler(project, root, GitCommand.COMMIT);
        h.addParameters("-m", BitbucketBundle.message("initial-rev-msg"));
        GitHandlerUtil.doSynchronously(h, BitbucketBundle.message("create-local-repository"), BitbucketBundle.message("share-project-on-bitbucket"));
        return h.errors().isEmpty();
    }

    public AbstractVcs getVcs(Project project) {
        return GitVcs.getInstance(project);
    }

    private static boolean execute(Project project, VirtualFile root, GitCommand cmd, String... params) {
        GitSimpleHandler handler = new GitSimpleHandler(project, root, cmd);
        handler.addParameters(params);
        handler.setSilent(true);
        handler.setNoSSH(true);
        try {
            handler.run();
            if (handler.getExitCode() != 0) {
                return false;
            }
        } catch (VcsException e) {
            return false;
        }
        return true;
    }
}
