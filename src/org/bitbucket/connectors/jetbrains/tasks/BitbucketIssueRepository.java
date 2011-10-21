package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.pivotal.PivotalTrackerRepository;
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
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;


@Tag("Bitbucket")
public class BitbucketIssueRepository extends BaseRepositoryImpl {
    private static final Logger log =
            Logger.getInstance("#org.bitbucket.connectors.jetbrains.tasks.BitbucketIssueRepository");


    public BitbucketIssueRepository() {
        super();
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        setRepositoryInfo(BitbucketUtil.getRepository(settings.getLogin(), settings.getPassword(), myRepoName));
    }

    protected BitbucketIssueRepository(TaskRepositoryType type) {
        super(type);
    }

    public BitbucketIssueRepository(BitbucketIssueRepository other) {
        super(other);
        setRepositoryInfo(other.getRepositoryInfo());
    }

    protected String myRepoName = null;

    @Tag("Repo")
    public String getRepositoryName() {
        return myRepoName;
    }

    public void setRepositoryName(String s) {
        myRepoName = s;
    }

    public RepositoryInfo getRepositoryInfo() {
        return myRepositoryInfo;
    }

    public void setRepositoryInfo(RepositoryInfo info) {
        this.myRepositoryInfo = info;
        myUsername = BitbucketSettings.getInstance().getLogin();
        myPassword = BitbucketSettings.getInstance().getPassword();
        myRepoName = info == null ? null : info.getSlug();
    }

    private RepositoryInfo myRepositoryInfo = null;


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

    TaskState mapState(String state) {
        if (state.equals("new")) return TaskState.SUBMITTED;
        if (state.equals("open")) return TaskState.OPEN;
        return TaskState.RESOLVED;
    }


    @Nullable
    private Task createIssue(final Element element) {
        log.info(element.getText());

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
        final Ref<Date> created = new Ref<Date>();
        try {
            created.set(PivotalTrackerRepository.parseDate(element, "created_on"));
        } catch (ParseException e) {
            log.warn(e);
        }

        final String repoName = myRepositoryInfo.getName();
        final String author = myRepositoryInfo.getOwner();
        final String repoSlug = myRepositoryInfo.getSlug();

        final String kind = element.getChild("metadata").getChildText("kind");
        final TaskType taskType =
                kind.equals("bug") ? TaskType.BUG : (kind.equals("enhancement") ? TaskType.FEATURE : TaskType.OTHER);

        final String state = element.getChildText("status");
        final TaskState taskState = mapState(state);

        final String issueUrl = element.getChildText("resource_uri");


        return new Task() {
            final String ID = id;

            @Override
            public boolean isIssue() {
                return true;
            }

            //https://bitbucket.org/sylvanaar2/lua-for-idea/issue/80
            @Override
            public String getIssueUrl() {
                return "https://api." + BitbucketUtil.BITBUCKET_DN + issueUrl;
            }

            @NotNull
            @Override
            public String getId() {
                return repoSlug+":"+id;
            }

            @NotNull
            @Override
            public String getSummary() {
                return summary;
            }

            public String getDescription() {
                return description;
            }

            @Override
            public TaskState getState() {
                return taskState;
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
                return created.get();
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
                return MessageFormat.format("{0}:{1} {2}", repoName, id, getSummary());
            }
        };
    }

    @Override
    public Task findTask(String s) throws Exception {
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        String[] parts = s.split(":");
        String url = "/repositories/" + parts[0] + "/repositories/" + parts[1];


        Element element =
                BitbucketUtil.request(settings.getLogin(), settings.getPassword(), url, false, null);

        if (element == null)
            return null;
        
        return createIssue(element);
    }

    @Override
    public BaseRepository clone() {
        return new BitbucketIssueRepository(this);
    }

    @Nullable
    private String getRealId(String id) {
        final String start = myRepositoryInfo.getSlug() + ":";
        return id.startsWith(start) ? id.substring(start.length()) : null;
    }
}
