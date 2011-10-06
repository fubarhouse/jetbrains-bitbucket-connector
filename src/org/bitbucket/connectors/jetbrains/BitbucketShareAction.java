package org.bitbucket.connectors.jetbrains;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;
import org.bitbucket.connectors.jetbrains.ui.BitbucketShareDialog;
import org.jetbrains.annotations.NotNull;
import org.zmlx.hg4idea.util.HgUtil;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgAddCommand;
import org.zmlx.hg4idea.command.HgCommitCommand;
import org.zmlx.hg4idea.command.HgInitCommand;

import java.util.*;

/**
 * User: leha2000
 * Date: Apr 15, 2011
 * Time: 12:16:00 PM
 */
public class BitbucketShareAction extends DumbAwareAction {

    public BitbucketShareAction() {
        super(BitbucketBundle.message("share-on-bitbucket"), BitbucketBundle.message("share-on-bitbucket"), BitbucketUtil.ICON);
    }

    public void update(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        boolean enabled = project != null && !project.isDefault();
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });

        Project project = e.getData(PlatformDataKeys.PROJECT);
        Set<String> names = new HashSet<String>();
        for (RepositoryInfo r : BitbucketUtil.getRepositories(project, true)) {
            names.add(r.getName());
        }

        BitbucketShareDialog dialog = new BitbucketShareDialog(project, names, true);
        dialog.show();
        if (!dialog.isOK()) {
            return;
        }

        share(project, dialog.getRepositoryName(), dialog.getDescription(), dialog.isSshRepositoryAccess());
    }

    private void share(Project project, String name, String description, boolean ssh) {
        VirtualFile root = project.getBaseDir();
        if (!ensureUnderMercurial(project, root)) {
            return;
        }

        refreshAndConfigureVcsMappings(project, root, "");

        BitbucketUtil.share(project, root, name, description, ssh);
    }

    private boolean ensureUnderMercurial(final Project project, final VirtualFile root) {
        VirtualFile hgRoot = HgUtil.getHgRootOrNull(project, root);
        if (hgRoot == null) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                public void run() {
                    createMercurialRepository(project, root);
                }
            }, BitbucketBundle.message("create-local-repository"), true, project);
        } else {
            if (hgRoot != root) {
                return false;
            }
        }
        return true;
    }

    private boolean createMercurialRepository(Project project, VirtualFile root) {
        try {
            new HgInitCommand(project).execute(root, new Consumer<Boolean>() {
                public void consume(Boolean aBoolean) {
                }
            });
            new HgAddCommand(project).execute(getSourceFolders(project, root));
            new HgCommitCommand(project, root, BitbucketBundle.message("initial-rev-msg")).execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<VirtualFile> getSourceFolders(Project project, VirtualFile root) {
        List<VirtualFile> result = new ArrayList<VirtualFile>();
        for (VirtualFile src: ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            result.add(src);
        }
        return result;
    }

    public static void refreshAndConfigureVcsMappings(final Project project, final VirtualFile root, final String path) {
        root.refresh(false, false);
        ProjectLevelVcsManager vcs = ProjectLevelVcsManager.getInstance(project);
        final List<VcsDirectoryMapping> vcsDirectoryMappings = new ArrayList<VcsDirectoryMapping>(vcs.getDirectoryMappings());
        VcsDirectoryMapping mapping = new VcsDirectoryMapping(path, HgVcs.getInstance(project).getName());
        for (int i = 0; i < vcsDirectoryMappings.size(); i++) {
            VcsDirectoryMapping m = vcsDirectoryMappings.get(i);
            if (m.getDirectory().equals(path)) {
                if (m.getVcs().length() == 0) {
                    vcsDirectoryMappings.set(i, mapping);
                    mapping = null;
                    break;
                } else if (m.getVcs().equals(mapping.getVcs())) {
                    mapping = null;
                    break;
                }
            }
        }
        if (mapping != null) {
            vcsDirectoryMappings.add(mapping);
        }
        vcs.setDirectoryMappings(vcsDirectoryMappings);
        vcs.updateActiveVcss();
        refreshFiles(project, Collections.singleton(root));
    }

    public static void refreshFiles(@NotNull final Project project, @NotNull final Collection<VirtualFile> affectedFiles) {
        final VcsDirtyScopeManager dirty = VcsDirtyScopeManager.getInstance(project);
        for (VirtualFile file : affectedFiles) {
            if (!file.isValid()) {
                continue;
            }
            file.refresh(false, true);
            if (file.isDirectory()) {
                dirty.dirDirtyRecursively(file);
            } else {
                dirty.fileDirty(file);
            }
        }
    }

}
