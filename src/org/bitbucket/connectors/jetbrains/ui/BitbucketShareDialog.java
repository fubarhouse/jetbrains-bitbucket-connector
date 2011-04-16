package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;

import javax.swing.*;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User: leha2000
 * Date: Apr 16, 2011
 * Time: 11:07:02 AM
 */
public class BitbucketShareDialog extends DialogWrapper {

    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

    private Set<String> myRepositories;
    private BitbucketSharePanel myPanel;

    public BitbucketShareDialog(Project project, Set<String> existingRepositories, boolean canCreatePrivate) {
        super(project);

        myPanel = new BitbucketSharePanel(this);
        myRepositories = existingRepositories;
        init();

        setTitle("Share project on Bitbucket");
        setOKButtonText("Share");
        myPanel.setRepositoryName(project.getName());
        myPanel.setCanCreatePrivate(canCreatePrivate);
        init();
        updateOkButton();
    }

    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    protected String getHelpId() {
        return "bitbucket.share";
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPanel.getPreferredFocusComponent();
    }

    public void updateOkButton() {
        final String repositoryName = getRepositoryName();
        if (StringUtil.isEmpty(repositoryName)) {
            setErrorText("No repository name selected");
            setOKActionEnabled(false);
            return;
        }
        if (myRepositories.contains(repositoryName)) {
            setErrorText("Repository with selected name already exists");
            setOKActionEnabled(false);
            return;
        }
        if (!REPO_NAME_PATTERN.matcher(repositoryName).matches()) {
            setErrorText("Invalid repository name. Name should consist of letters, numbers, dashes and underscores");
            setOKActionEnabled(false);
            return;
        }
        setErrorText(null);
        setOKActionEnabled(true);
    }

    public String getRepositoryName() {
        return myPanel.getRepositoryName();
    }

    public boolean isPrivate() {
        return myPanel.isPrivate();
    }

    public String getDescription() {
        return myPanel.getDescription();
    }
}
