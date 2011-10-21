package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
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
    private JTextPane mySignupPane;
    private JButton myAddSSHKeyButton;

    public BitbucketSettingsPanel() {
        String msg = BitbucketBundle.message("signup-on-bitbucket", "https://bitbucket.org/account/signup/");
        mySignupPane.setText(msg);
        mySignupPane.setBackground(myPane.getBackground());
        mySignupPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.launchBrowser(e.getURL().toExternalForm());
                }
            }
        });

        myTestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean result = BitbucketUtil.checkCredentials(ProjectManager.getInstance().getDefaultProject(), getLogin(), getPassword());
                Messages.showInfoMessage(
                        result ? BitbucketBundle.message("connection-success") : BitbucketBundle.message("cannot-login"),
                        result ? BitbucketBundle.message("success") : BitbucketBundle.message("failure"));
            }
        });

        myAddSSHKeyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BitbucketUtil.addSshKey(ProjectManager.getInstance().getDefaultProject(), myPane, getLogin(), getPassword());
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
