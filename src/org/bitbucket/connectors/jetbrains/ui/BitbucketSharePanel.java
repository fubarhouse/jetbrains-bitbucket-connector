package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * User: leha2000
 * Date: Apr 16, 2011
 * Time: 10:59:20 AM
 */
public class BitbucketSharePanel {

    private JPanel myPanel;

    private JTextField myNameTextField;
    private JCheckBox myPrivateCheckBox;
    private JTextPane myDescriptionTextPane;
    private JCheckBox mySshRepositoryAccessCheckBox;

    public BitbucketSharePanel(final BitbucketShareDialog dialog) {
        myNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            protected void textChanged(DocumentEvent documentEvent) {
                dialog.updateOkButton();
            }
        });
        myNameTextField.setPreferredSize(new Dimension(myNameTextField.getFontMetrics(myNameTextField.getFont()).charWidth('W') * 30, -1));
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public JComponent getPreferredFocusComponent() {
        return myNameTextField;
    }

    public String getRepositoryName() {
        return myNameTextField.getText().trim();
    }

    public void setRepositoryName(String name) {
        myNameTextField.setText(name);
    }

    public boolean isPrivate() {
        return myPrivateCheckBox.isSelected();
    }

    public boolean isSshRepositoryAccess() {
        return mySshRepositoryAccessCheckBox.isSelected();
    }

    public String getDescription() {
        return myDescriptionTextPane.getText().trim();
    }

    public void setCanCreatePrivate(boolean canCreatePrivate) {
        if (!canCreatePrivate) {
            myPrivateCheckBox.setEnabled(false);
            myPrivateCheckBox.setToolTipText("Your account doesn't support private repositories");
        }
    }

}
