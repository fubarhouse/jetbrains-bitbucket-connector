package org.bitbucket.connectors.jetbrains;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

/**
 * User: leha2000
 * Date: Oct 18, 2011
 * Time: 10:19:57 AM
 */
public class ProjectUtil {

    public static List<VirtualFile> getSourceFolders(Project project, VirtualFile root) {
        List<VirtualFile> result = new ArrayList<VirtualFile>();
        for (VirtualFile src: ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            result.add(src);
        }
        return result;
    }
}
