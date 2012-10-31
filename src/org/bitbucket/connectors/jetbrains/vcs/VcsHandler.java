package org.bitbucket.connectors.jetbrains.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

/**
 * User: leha2000
 * Date: Oct 14, 2011
 * Time: 11:34:00 AM
 */
public interface VcsHandler {
    boolean checkout(Project project, String folder, String repositoryUrl);

    boolean push(Project project, VirtualFile root, String repositoryUrl);

    void setRepositoryDefaultUrl(Project project, VirtualFile root, String repositoryUrl) throws IOException;

    VirtualFile getRepositoryRoot(VirtualFile folder);

    boolean initRepository(Project project, VirtualFile root);

    AbstractVcs getVcs(Project project);
}
