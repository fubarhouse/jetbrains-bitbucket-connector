package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * User: leha2000
 * Date: Apr 15, 2011
 * Time: 10:08:24 AM
 */
public class BitbucketLoginPanel {

    private JPanel myPane;

    private JTextField myLoginTextField;
    private JPasswordField myPasswordField;

    public BitbucketLoginPanel(final BitbucketLoginDialog dialog) {
        DocumentListener listener = new DocumentAdapter() {
            protected void textChanged(DocumentEvent documentEvent) {
                dialog.clearErrors();
            }
        };

        myLoginTextField.getDocument().addDocumentListener(listener);
        myPasswordField.getDocument().addDocumentListener(listener);
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

    public JComponent getPreferrableFocusComponent() {
        return myLoginTextField;
    }
}
