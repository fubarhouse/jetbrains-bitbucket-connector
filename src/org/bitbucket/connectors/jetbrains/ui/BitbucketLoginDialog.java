package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.bitbucket.connectors.jetbrains.BitbucketSettings;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;

import javax.swing.*;

/**
 * User: leha2000
 * Date: Apr 15, 2011
 * Time: 10:12:26 AM
 */
public class BitbucketLoginDialog extends DialogWrapper {

    private BitbucketLoginPanel myPanel;
    private Project myProject;

    public BitbucketLoginDialog(Project project) {
        super(project, true);
        myProject = project;

        myPanel = new BitbucketLoginPanel(this);

        BitbucketSettings settings = BitbucketSettings.getInstance();
        myPanel.setLogin(settings.getLogin());
        myPanel.setPassword(settings.getPassword());

        setTitle("Login to Bitbucket");
        setOKButtonText("Login");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getPanel();
    }

    protected Action[] createActions() {
      return new Action[] {getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myPanel.getPreferrableFocusComponent();
    }

    @Override
    protected void doOKAction() {
      String login = myPanel.getLogin();
      String password = myPanel.getPassword();
      if (BitbucketUtil.checkCredentials(myProject, login, password)) {
        BitbucketSettings settings = BitbucketSettings.getInstance();
        settings.setLogin(login);
        settings.setPassword(password);
        super.doOKAction();
      } else {
        setErrorText("Cannot login with given credentials");
      }
    }

    public void clearErrors() {
      setErrorText(null);
    }
}
