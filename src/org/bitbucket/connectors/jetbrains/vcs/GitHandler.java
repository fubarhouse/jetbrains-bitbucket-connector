package org.bitbucket.connectors.jetbrains.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitVcs;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;

import java.io.File;
import java.io.IOException;

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

    public static boolean isGitUrl(String url) {
        return url != null && url.endsWith(".git");
    }

    public boolean push(Project project, VirtualFile root, String repositoryUrl) {
        return false;
    }

    public void setRepositoryDefaultUrl(VirtualFile root, String repositoryUrl) throws IOException {
    }

    public boolean ensureUnderVcs(Project project, VirtualFile root) {
        return false;
    }

    public AbstractVcs getVcs(Project project) {
        return GitVcs.getInstance(project);
    }
}
