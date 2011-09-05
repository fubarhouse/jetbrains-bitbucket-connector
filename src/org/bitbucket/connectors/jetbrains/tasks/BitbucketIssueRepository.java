package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepositoryType;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.xmlb.annotations.Tag;
import org.bitbucket.connectors.jetbrains.RepositoryInfo;
import org.jetbrains.annotations.Nullable;


@Tag("Bitbucket")
public class BitbucketIssueRepository extends BaseRepositoryImpl {
    private static final Logger log =
            Logger.getInstance("#org.bitbucket.connectors.jetbrains.tasks.BitbucketIssueRepository");


    public RepositoryInfo getRepositoryInfo() {
        return myRepositoryInfo;
    }

    public void setRepositoryInfo(RepositoryInfo info) {
        this.myRepositoryInfo = info;
    }

    private RepositoryInfo myRepositoryInfo = null;

    protected BitbucketIssueRepository(TaskRepositoryType type) {
        super(type);
    }

    @Override
    public Task[] getIssues(@Nullable String s, int i, long l) throws Exception {
        return new Task[0];
    }

    @Override
    public Task findTask(String s) throws Exception {
        return null;
    }

    @Override
    public BaseRepository clone() {
        return new BitbucketIssueRepository(this.getRepositoryType());
    }

    
}
