package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: leha2000
 * Date: Apr 7, 2011
 * Time: 2:03:43 PM
 */
public class BitbucketSettingsPanel {

    private JPanel myPane;

    private JTextField myLoginTextField;
    private JPasswordField myPasswordField;

    private JButton myTestButton;

    public BitbucketSettingsPanel() {
        myTestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean result = BitbucketUtil.checkCredentials(ProjectManager.getInstance().getDefaultProject(), getLogin(), getPassword());
                Messages.showInfoMessage(result ? "Connection successful" : "Cannot login to Bitbucket", result ? "Success" : "Failure");
            }
        });
    }

    public JComponent getPanel() {
        return myPane;
    }

    public void setLogin(String login) {
        myLoginTextField.setText(login);
    }

    public void setPassword(String password) {
        myPasswordField.setText(password);
    }

    public String getLogin() {
        return myLoginTextField.getText().trim();
    }

    public String getPassword() {
        return String.valueOf(myPasswordField.getPassword());
    }
}
