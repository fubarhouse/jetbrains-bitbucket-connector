package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ListCellRendererWrapper;
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
import java.util.Collections;
import java.util.List;

public class BitbucketCloneProjectPanel {

    private JPanel myPanel;

    private JComboBox mySelectRepositoryComboBox;
    private JTextField myRepositoryUrlField;
    private TextFieldWithBrowseButton myTextFieldWithBrowseButton;
    private JTextField myProjectNameText;
    private JRadioButton myRepositoryListRadioButton;
    private JRadioButton myRepositoryUrlRadioButton;

    private BitbucketCloneProjectDialog myDialog;

    public BitbucketCloneProjectPanel(BitbucketCloneProjectDialog dialog) {
        myDialog = dialog;
        mySelectRepositoryComboBox.setRenderer(new ListCellRendererWrapper<RepositoryInfo>() {
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
        myRepositoryUrlField.getDocument().addDocumentListener(listener);
        myTextFieldWithBrowseButton.getChildComponent().getDocument().addDocumentListener(listener);
        myTextFieldWithBrowseButton.setTextFieldPreferredWidth(50);

        myTextFieldWithBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseFolder();
            }
        });

        myRepositoryListRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repositoryTypeChanged(true);
                myDialog.updateOkButton();
            }
        });
        myRepositoryUrlRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repositoryTypeChanged(false);
                myDialog.updateOkButton();
            }
        });

        myRepositoryListRadioButton.setSelected(true);
        repositoryTypeChanged(true);
    }

    private void repositoryTypeChanged(boolean selection) {
        mySelectRepositoryComboBox.setEnabled(selection);
        myRepositoryUrlField.setEnabled(!selection);
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

        VirtualFile[] files = FileChooser.chooseFiles(fileChooserDescriptor, myPanel, null, preselectedFolder);
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

    public boolean isRepositoryUrl() {
        return myRepositoryUrlRadioButton.isSelected();
    }

    public RepositoryInfo getSelectedRepository() {
        return myRepositoryListRadioButton.isSelected() ? (RepositoryInfo) mySelectRepositoryComboBox.getModel().getSelectedItem() : null;
    }

    public String getSelectedPath() {
        return myTextFieldWithBrowseButton.getText();
    }

    public String getProjectName() {
        return myProjectNameText.getText();
    }

    public void setAvailableRepos(List<RepositoryInfo> repos) {
        Collections.sort(repos);

        mySelectRepositoryComboBox.setModel(new DefaultComboBoxModel(ArrayUtil.toObjectArray(repos)));
        RepositoryInfo preselectedRepository = (RepositoryInfo) mySelectRepositoryComboBox.getSelectedItem();
        if (preselectedRepository != null) {
            myProjectNameText.setText(preselectedRepository.getName());
        }
    }

    public void setSelectedPath(String path) {
        myTextFieldWithBrowseButton.setText(path);
    }

    public String getRepositoryUrl() {
        return myRepositoryUrlRadioButton.isSelected() ? myRepositoryUrlField.getText() : null;
    }
}
