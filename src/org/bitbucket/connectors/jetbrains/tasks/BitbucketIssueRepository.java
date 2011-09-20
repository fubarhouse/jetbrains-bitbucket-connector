package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.bitbucket.connectors.jetbrains.BitbucketSettings;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.bitbucket.connectors.jetbrains.RepositoryInfo;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;
import java.util.List;


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

    public BitbucketIssueRepository(BitbucketIssueRepository other) {
        super(other);
        setRepositoryInfo(other.getRepositoryInfo());
    }


    @Override
    public Task[] getIssues(@Nullable String query, int max, long since) throws Exception {
        log.info("getIssues: " + query);

        @SuppressWarnings({"unchecked"}) List<Object> children = getIssues(query);
        List<Task> taskList = ContainerUtil.mapNotNull(children, new NullableFunction<Object, Task>() {
            public Task fun(Object o) {
                final Task issue = createIssue((Element) o);
                log.info("Loaded Issue: " + issue);
                return issue;
            }
        });

        return taskList.toArray(new Task[taskList.size()]);
    }

    // https://api.bitbucket.org/1.0/repositories/sylvanaar2/lua-for-idea/issues/?format=xml&status=new&status=open
    // sylvanaar2/lua-for-idea/issues?status=new&status=open  status=new&status=open
    private List getIssues(String query) throws Exception {
        final BitbucketSettings settings = BitbucketSettings.getInstance();

        String url = "/repositories/" + myRepositoryInfo.getOwner() + "/" + myRepositoryInfo.getSlug() + "/issues";

        log.info("getIssues for : " + url);
        String queryParameters = "status=new&status=open";

        Element element =
                BitbucketUtil.request(settings.getLogin(), settings.getPassword(), url, false, null, queryParameters);

        return element.getChild("issues").getChildren("resource");
    }

    @Nullable
    private Task createIssue(final Element element) {
        final String id = element.getChildText("local_id");
        if (id == null) {
            return null;
        }
        final String summary = element.getChildText("title");
        if (summary == null) {
            return null;
        }
        final boolean isClosed = !"open".equals(element.getChildText("status"));
        final String description = element.getChildText("content");
//        final Ref<Date> created = new Ref<Date>();
//        try {
//            created.set(PivotalTrackerRepository.parseDate(element, "created-on"));
//        } catch (ParseException e) {
//            log.warn(e);
//        }

        final String repoName = myRepositoryInfo.getName();
        final String author = myRepositoryInfo.getOwner();

        final String kind = element.getChild("metadata").getChildText("kind");
        final TaskType taskType =
                kind.equals("bug") ? TaskType.BUG : (kind.equals("enhancement") ? TaskType.FEATURE : TaskType.OTHER);

        return new Task() {
            @Override
            public boolean isIssue() {
                return true;
            }

            //https://bitbucket.org/sylvanaar2/lua-for-idea/issue/80
            @Override
            public String getIssueUrl() {
                final String id = getRealId(getId());
                return id != null ? getUrl() + "/" + author + "/" + repoName + "/issue/" + id : null;
            }

            @NotNull
            @Override
            public String getId() {
                return repoName + "-" + id;
            }

            @NotNull
            @Override
            public String getSummary() {
                return summary;
            }

            public String getDescription() {
                return description;
            }

            @NotNull
            @Override
            public Comment[] getComments() {
                return new Comment[0];
            }

            @Override
            public Icon getIcon() {
                return BitbucketUtil.ICON;
            }

            @NotNull
            @Override
            public TaskType getType() {
                return taskType;
            }

            @Override
            public Date getUpdated() {
                return null;
            }

            @Override
            public Date getCreated() {
                return null; //created.get();
            }

            @Override
            public boolean isClosed() {
                return isClosed;
            }

            @Override
            public TaskRepository getRepository() {
                return BitbucketIssueRepository.this;
            }

            @Override
            public String getPresentableName() {
                return getId() + ": " + getSummary();
            }
        };
    }

    @Override
    public Task findTask(String s) throws Exception {
        return null;
    }

    @Override
    public BaseRepository clone() {
        return new BitbucketIssueRepository(this);
    }

    @Nullable
    private String getRealId(String id) {
        final String start = myRepositoryInfo.getName() + "-";
        return id.startsWith(start) ? id.substring(start.length()) : null;
    }
}
