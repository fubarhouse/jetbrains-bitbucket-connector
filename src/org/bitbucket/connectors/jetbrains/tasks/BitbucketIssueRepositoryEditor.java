package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import org.bitbucket.connectors.jetbrains.BitbucketSettings;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.bitbucket.connectors.jetbrains.RepositoryInfo;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;

public class BitbucketIssueRepositoryEditor extends BaseRepositoryEditor<BitbucketIssueRepository> {
    private static final Logger log =
            Logger.getInstance("#org.bitbucket.connectors.jetbrains.tasks.BitbucketIssueRepository");

    private final JComboBox mySelectRepositoryComboBox;

    public BitbucketIssueRepositoryEditor(final Project project, final BitbucketIssueRepository repository,
                                          Consumer<BitbucketIssueRepository> changeListener) {
        super(project, repository, changeListener);

        myUrlLabel.setVisible(false);
        myURLText.setVisible(false);

        myPasswordLabel.setVisible(false);
        myPasswordText.setVisible(false);

        myUserNameText.setEditable(false);
        myUserNameText.setText(BitbucketSettings.getInstance().getLogin());

        java.util.List<RepositoryInfo> repos = BitbucketUtil.getRepositories(project, false);
        if (repos == null || repos.size() == 0) {
            myCustomPanel.add(new JLabel(BitbucketBundle.message("no-repos-available")), BorderLayout.CENTER);
            mySelectRepositoryComboBox = null;
            return;
        }

        Collections.sort(repos);
        mySelectRepositoryComboBox = new JComboBox(ArrayUtil.toObjectArray(repos));

        mySelectRepositoryComboBox
                .setRenderer(new ListCellRendererWrapper<RepositoryInfo>(mySelectRepositoryComboBox.getRenderer()) {
                    public void customize(JList list, RepositoryInfo value, int index, boolean selected,
                                          boolean cellHasFocus) {
                        if (value == null) return;
                        setText(value.getOwner() + "/" + value.getName());
                    }
                });
        mySelectRepositoryComboBox.setSelectedItem(repository.getRepositoryInfo());

        mySelectRepositoryComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                RepositoryInfo repositoryInfo = (RepositoryInfo) e.getItem();
                myURLText.setText((repositoryInfo != null ? createRepositoryUrl(repositoryInfo) : ""));
                apply();
            }
        });

        myCustomPanel.add(mySelectRepositoryComboBox, BorderLayout.CENTER);
        myCustomLabel.add(new JLabel(BitbucketBundle.message("select-task-repository"), SwingConstants.RIGHT) {
            @Override
            public Dimension getPreferredSize() {
                final Dimension oldSize = super.getPreferredSize();
                final Dimension size = mySelectRepositoryComboBox.getPreferredSize();
                return new Dimension(oldSize.width, size.height);
            }
        }, BorderLayout.CENTER);
    }

    private String createRepositoryUrl(RepositoryInfo repositoryInfo) {
        return BitbucketUtil.API_URL_BASE + "/" + "repositories" + "/" + repositoryInfo.getOwner() + "/" +
        repositoryInfo.getSlug();
    }

    @Override
    public void apply() {
        if (mySelectRepositoryComboBox != null)
            myRepository.setRepositoryInfo((RepositoryInfo) mySelectRepositoryComboBox.getSelectedItem());
        super.apply();
    }
}
