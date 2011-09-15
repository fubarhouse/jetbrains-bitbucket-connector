package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.bitbucket.connectors.jetbrains.RepositoryInfo;

import javax.swing.*;
import java.util.List;

public class BitbucketCloneProjectDialog extends DialogWrapper {

    private BitbucketCloneProjectPanel myPanel;

    public BitbucketCloneProjectDialog(final Project project, final List<RepositoryInfo> repos) {
        super(project, true);
        myPanel = new BitbucketCloneProjectPanel(this);
        setTitle(BitbucketBundle.message("select-clone-repository"));
        setOKButtonText(BitbucketBundle.message("clone"));
        myPanel.setAvailableRepos(repos);
        init();
        setOKActionEnabled(false);
    }

    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getPanel();
    }

    @Override
    protected String getHelpId() {
        return "Bitbucket";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPanel.getPreferrableFocusComponent();
    }

    public void updateOkButton() {
        if (isRepositoryUrl()) {
            if (StringUtil.isEmpty(getRepositoryUrl())) {
                setErrorText(BitbucketBundle.message("no-repository-url"));
                setOKActionEnabled(false);
                return;
            }
        } else {
            if (getSelectedRepository() == null) {
                setErrorText(BitbucketBundle.message("no-repository-selected"));
                setOKActionEnabled(false);
                return;
            }
        }
        String path = getSelectedPath();
        if (path == null) {
            setErrorText(BitbucketBundle.message("select-clone-folder"));
            setOKActionEnabled(false);
            return;
        }
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null || !file.exists() || !file.isDirectory()) {
            setErrorText(BitbucketBundle.message("no-selected-folder"));
            setOKActionEnabled(false);
            return;
        }
        String projectName = getProjectName();
        if (file.findChild(projectName) != null) {
            setErrorText(BitbucketBundle.message("folder-exists", projectName));
            setOKActionEnabled(false);
            return;
        }

        setErrorText(null);
        setOKActionEnabled(true);
    }

    public String getProjectName() {
        return myPanel.getProjectName();
    }

    public RepositoryInfo getSelectedRepository() {
        return myPanel.getSelectedRepository();
    }

    public boolean isRepositoryUrl() {
        return myPanel.isRepositoryUrl();
    }

    public String getRepositoryUrl() {
        return myPanel.getRepositoryUrl();
    }

    public String getSelectedPath() {
        return myPanel.getSelectedPath();
    }

    public void setSelectedPath(final String path) {
        myPanel.setSelectedPath(path);
    }
}
