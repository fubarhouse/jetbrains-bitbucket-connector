package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * User: leha2000
 * Date: Sep 22, 2011
 * Time: 11:14:26 AM
 */
abstract class BitbucketDialog extends DialogWrapper {

    private Project myProject;
    private DialogWrapperAction myOkAction = new EnableSshAction();

    protected BitbucketDialog(Project project) {
        super(project, true);
        myProject = project;
    }

    protected Action[] createActions() {
        return new Action[]{myOkAction, getCancelAction()};
    }

    @Override
    protected void setOKActionEnabled(boolean isEnabled) {
        myOkAction.setEnabled(isEnabled);
    }

    @Override
    public boolean isOKActionEnabled() {
        return myOkAction.isEnabled();
    }

    @Override
    protected Action getOKAction() {
        return myOkAction;
    }

    public abstract boolean isUseSsh();

    protected class EnableSshAction extends DialogWrapperAction {

        private EnableSshAction() {
            super(CommonBundle.getOkButtonText());
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        protected void doAction(ActionEvent e) {
            if (isUseSsh()) {
                if (!BitbucketUtil.enableSsh(myProject, BitbucketDialog.this.getContentPane())) {
                    return;
                }
            }
            doOKAction();
        }
    }

}
