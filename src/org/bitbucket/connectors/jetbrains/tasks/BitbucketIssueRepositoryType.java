package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jon S Akhtar
 * Date: 9/4/11
 * Time: 3:46 PM
 */
public class BitbucketIssueRepositoryType extends BaseRepositoryType<BitbucketIssueRepository> {
    @NotNull
    @Override
    public String getName() {
        return BitbucketUtil.BITBUCKET;
    }

    @Override
    public Icon getIcon() {
        return BitbucketUtil.ICON;
    }

    @NotNull
    @Override
    public TaskRepository createRepository() {
        return new BitbucketIssueRepository(this);
    }

    @Override
    public Class<BitbucketIssueRepository> getRepositoryClass() {
        return BitbucketIssueRepository.class;
    }

    @NotNull
    @Override
    public TaskRepositoryEditor createEditor(BitbucketIssueRepository repository, Project project,
                                             Consumer<BitbucketIssueRepository> changeListener) {
        return new BitbucketIssueRepositoryEditor(project, repository, changeListener);
    }

    @Override
    public boolean isSupported(int feature) {
        if (feature == BASIC_HTTP_AUTHORIZATION)
            return false;

        return super.isSupported(feature);
    }
}
