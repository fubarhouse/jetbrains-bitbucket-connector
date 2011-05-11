package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ArrayUtil;
import org.bitbucket.connectors.jetbrains.RepositoryInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class BitbucketCloneProjectPanel {

    private JPanel myPanel;

    private JComboBox mySelectRepositoryComboBox;
    private TextFieldWithBrowseButton myTextFieldWithBrowseButton;
    private JTextField myProjectNameText;

    private BitbucketCloneProjectDialog myDialog;

    public BitbucketCloneProjectPanel(BitbucketCloneProjectDialog dialog) {
        myDialog = dialog;
        mySelectRepositoryComboBox.setRenderer(new ListCellRendererWrapper<RepositoryInfo>(mySelectRepositoryComboBox.getRenderer()) {
            public void customize(JList list, RepositoryInfo value, int index, boolean selected, boolean cellHasFocus) {
                setText(value.getOwner() + "/" + value.getName());
            }
        });
        mySelectRepositoryComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                RepositoryInfo repositoryInfo = (RepositoryInfo) e.getItem();
                if (repositoryInfo != null) {
                    myProjectNameText.setText(repositoryInfo.getName());
                    myDialog.updateOkButton();
                }
            }
        });

        DocumentListener listener = new DocumentAdapter() {
            protected void textChanged(DocumentEvent documentEvent) {
                myDialog.updateOkButton();
            }
        };
        myProjectNameText.getDocument().addDocumentListener(listener);
        myTextFieldWithBrowseButton.getChildComponent().getDocument().addDocumentListener(listener);
        myTextFieldWithBrowseButton.setTextFieldPreferredWidth(50);

        myTextFieldWithBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseFolder();
            }
        });
    }

    private void chooseFolder() {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            public String getName(VirtualFile virtualFile) {
                return virtualFile.getName();
            }

            @Nullable
            public String getComment(VirtualFile virtualFile) {
                return virtualFile.getPresentableUrl();
            }
        };
        fileChooserDescriptor.setTitle("Select project destination folder");

        String preselectedFolderPath = myTextFieldWithBrowseButton.getText();
        VirtualFile preselectedFolder = LocalFileSystem.getInstance().findFileByPath(preselectedFolderPath);

        VirtualFile[] files = FileChooser.chooseFiles(myPanel, fileChooserDescriptor, preselectedFolder);
        if (files.length > 0) {
            myTextFieldWithBrowseButton.setText(files[0].getPath());
        }
    }

    public JComponent getPanel() {
        return myPanel;
    }

    public JComponent getPreferrableFocusComponent() {
        return mySelectRepositoryComboBox;
    }

    public RepositoryInfo getSelectedRepository() {
        return (RepositoryInfo) mySelectRepositoryComboBox.getModel().getSelectedItem();
    }

    public String getSelectedPath() {
        return myTextFieldWithBrowseButton.getText();
    }

    public String getProjectName() {
        return myProjectNameText.getText();
    }

    public void setAvailableRepos(List<RepositoryInfo> repos) {
        mySelectRepositoryComboBox.setModel(new DefaultComboBoxModel(ArrayUtil.toObjectArray(repos)));
        RepositoryInfo preselectedRepository = (RepositoryInfo) mySelectRepositoryComboBox.getSelectedItem();
        if (preselectedRepository != null) {
            myProjectNameText.setText(preselectedRepository.getName());
        }
    }

    public void setSelectedPath(String path) {
        myTextFieldWithBrowseButton.setText(path);
    }
}
