package org.bitbucket.connectors.jetbrains.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

/**
 * User: leha2000
 * Date: Oct 14, 2011
 * Time: 11:34:00 AM
 */
public interface VcsHandler {
    void checkout(Project project, String repositoryUrl, CheckoutProvider.Listener listener, String destFolder, String projectName);

    boolean push(Project project, VirtualFile root, String repositoryUrl);

    void setRepositoryDefaultUrl(Project project, VirtualFile root, String repositoryUrl) throws IOException;

    VirtualFile getRepositoryRoot(VirtualFile folder);

    boolean initRepository(Project project, VirtualFile root);

    AbstractVcs getVcs(Project project);
}
