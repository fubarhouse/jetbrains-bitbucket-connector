package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.*;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.bitbucket.connectors.jetbrains.BitbucketSettings;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.*;


@Tag("Bitbucket")
public class BitbucketIssueRepository extends BaseRepositoryImpl {
    private static final Logger log =
            Logger.getInstance("#org.bitbucket.connectors.jetbrains.tasks.BitbucketIssueRepository");


    @SuppressWarnings({"UnusedDeclaration"})
    public BitbucketIssueRepository() {}

    protected BitbucketIssueRepository(BitbucketIssueRepositoryType type) {
        super(type);
    }

    public BitbucketIssueRepository(BitbucketIssueRepository other) {
        super(other);
        setRepositoryName(other.getRepositoryName());
        setRepositoryOwner(other.getRepositoryOwner());
    }

    protected String myRepoName = null;

    @Attribute("repo")
    public String getRepositoryName() {
        return myRepoName;
    }

    public void setRepositoryName(String s) {
        myRepoName = s;
    }

    protected String myRepoOwner = null;

    @Attribute("owner")
    public String getRepositoryOwner() {
        return myRepoOwner;
    }

    public void setRepositoryOwner(String s) {
        myRepoOwner = s;
    }

    @Override
    public Task[] getIssues(@Nullable String query, int max, long since) throws Exception {
        log.debug("getIssues: " + query);

        @SuppressWarnings({"unchecked"}) List<Object> children = getIssues(query);
        if (children == null) return Task.EMPTY_ARRAY;

        List<Task> taskList = ContainerUtil.mapNotNull(children, new NullableFunction<Object, Task>() {
            public Task fun(Object o) {
                final Task issue = createIssue((Element) o);
                log.debug("Loaded Issue: " + issue);
                return issue;
            }
        });

        return taskList.toArray(new Task[taskList.size()]);
    }

    // https://api.bitbucket.org/1.0/repositories/sylvanaar2/lua-for-idea/issues/?format=xml&status=new&status=open
    // sylvanaar2/lua-for-idea/issues?status=new&status=open  status=new&status=open
    private List getIssues(String query) throws Exception {
        log.debug(query);
        final BitbucketSettings settings = BitbucketSettings.getInstance();


        String url = "/repositories/" + getRepositoryOwner() + "/" + getRepositoryName() + "/issues";

        log.debug("getIssues for : " + url);
        String queryParameters = "status=new&status=open";

        Element element =
                BitbucketUtil.request(settings.getLogin(), settings.getPassword(), url, false, null, queryParameters);
        if (element == null) { return null; }

        return element.getChild("issues").getChildren("resource");
    }

    private Comment[] getIssueComments(String issueUrl) {
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        Element commentElement;

        try {
            commentElement = BitbucketUtil.request(settings.getLogin(), settings.getPassword(),
                    issueUrl + "/comments", false, null, null);
        } catch (Exception e) {
            log.error("Unable to load comments for issue " + issueUrl, e);
            return Comment.EMPTY_ARRAY;
        }

        List<Comment> comments = new ArrayList<Comment>();

        for(Element el : commentElement.getChildren("resource")) {
            String text = el.getChildText("content");
            String author = el.getChild("author_info").getChildText("username");
            Date updated = parseDate(el, "utc_updated_on");

            comments.add(new SimpleComment(updated, author, text));
        }

        Collections.sort(comments, new Comparator<Comment>() {
            @Override
            public int compare(Comment o1, Comment o2) {
                Date d1 = o1.getDate();
                Date d2 = o2.getDate();

                return (d1 != null) ? d1.compareTo(d2) : (d2 == null ? 0 : 1);
            }
        });

        return comments.toArray(new Comment[comments.size()]);
    }

    TaskState mapState(String state) {
        if (state.equals("new")) return TaskState.SUBMITTED;
        if (state.equals("open")) return TaskState.OPEN;
        return TaskState.RESOLVED;
    }


    @Nullable
    private Task createIssue(final Element element) {
        if (element != null) log.debug(element.getText());
        else {
            log.debug("NULL element");
            return null;
        }


        final String id = element.getChildText("local_id");
        if (id == null) {
            return null;
        }
        final String summary = element.getChildText("title");
        if (summary == null) {
            return null;
        }
        final String description = element.getChildText("content").replace("_", "&#95;");
        final Date created = parseDate(element, "created_on");

        final String kind = element.getChild("metadata").getChildText("kind");
        final TaskType taskType =
                kind.equals("bug") ? TaskType.BUG : (kind.equals("enhancement") ? TaskType.FEATURE : TaskType.OTHER);

        final String state = element.getChildText("status");
        final TaskState taskState = mapState(state);

        final boolean isClosed = taskState.equals(TaskState.RESOLVED);

        //  /1.0/repositories/sylvanaar2/lua-for-idea/issues/79
        final String issueUrl = element.getChildText("resource_uri").replace("/1.0/repositories", "").replace("/issues/", "/issue/");

        final Comment[] comments = getIssueComments(element.getChildText("resource_uri").replace("/1.0", ""));

        return new LocalTaskImpl(new Task() {

            public String getCustomIcon() {
                return null;
            }

            @Override
            public boolean isIssue() {
                return true;
            }

            //https://bitbucket.org/sylvanaar2/lua-for-idea/issue/80
            @Override
            public String getIssueUrl() {
                return "https://" + BitbucketUtil.BITBUCKET_DN + issueUrl;
            }

            @NotNull
            @Override
            public String getId() {
                return id;
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
            public Comment[] getComments() { return comments; }

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
                return created;
            }

            @Override
            public Date getCreated() {
                return created;
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
                return MessageFormat.format("#{0}: {1}", getId(), getSummary());
            }
        });
    }

    private static Date parseDate(final Element element, final String name) {
        String val = element.getChildText(name);
        if (val != null) {
            return TaskUtil.parseDate(val);
        } else {
            return null;
        }
    }

    //https://api.bitbucket.org/1.0/repositories/sylvanaar2/lua-for-idea/issues/1
    @Override
    public Task findTask(String s) throws Exception {
        log.debug("Find Task:" + s);
        final BitbucketSettings settings = BitbucketSettings.getInstance();

        String url = "/repositories/" + getRepositoryOwner() + "/" + getRepositoryName() + "/issues/" + s;

        Element element = BitbucketUtil.request(settings.getLogin(), settings.getPassword(), url, false, null);

        if (element == null) return null;

        return createIssue(element);
    }

    @Override
    public BaseRepository clone() {
        try {
            return new BitbucketIssueRepository(this);
        } catch (NoSuchMethodError err) {
            log.warn(err);

            // FIXME: failed to figure out the cause of NoSuchMethodError on PyCharm PC-133.881, performing manual clone
            // https://bitbucket.org/dmitry_cherkas/jetbrains-bitbucket-connector/issue/2
            BitbucketIssueRepository r = new BitbucketIssueRepository();

            r.setRepositoryName(this.getRepositoryName());
            r.setRepositoryOwner(this.getRepositoryOwner());

            r.setCommitMessageFormat(this.getCommitMessageFormat());
            r.setEncodedPassword(this.getEncodedPassword());
            r.setLoginAnonymously(this.isLoginAnonymously());
            r.setPassword(this.getPassword());
            r.setRepositoryType(this.getRepositoryType());
            r.setShared(this.isShared());
            r.setShouldFormatCommitMessage(this.isShouldFormatCommitMessage());
            r.setUrl(this.getUrl());
            r.setUseHttpAuthentication(this.isUseHttpAuthentication());
            r.setUseProxy(this.isUseProxy());
            r.setUsername(this.getUsername());

            return r;
        }
    }

    @Override
    public void setTaskState(Task task, TaskState state) throws Exception {
        String newState = null;
        switch (state) {
            case OPEN:
            case REOPENED:
                newState = "open";
                break;
            case RESOLVED:
                newState = "resolved";
                break;
            default:
                super.setTaskState(task, state);
                return;
        }
        final String url = "/repositories/" + getRepositoryOwner() + "/"
                + getRepositoryName() + "/issues/" + task.getId();
        final BitbucketSettings settings = BitbucketSettings.getInstance();
        Element request = BitbucketUtil.putRequest(settings.getLogin(), settings.getPassword(), url, "status=" + newState);
    }

    @Nullable
    public String extractId(String taskName) {
        return taskName;
    }

    @Override
    @Nullable
    public String getTaskComment(Task task) {
        return MessageFormat.format("#{0}: {1}", task.getId(), task.getSummary());
    }

    @Override
    public String toString() {
        return "BitbucketIssueRepository{" + "myRepoName='" + myRepoName + '\'' + ", myRepoOwner='" + myRepoOwner +
                '\'' + '}';
    }

    @Override
    public String getUrl() {
        return BitbucketUtil.API_URL_BASE + "/" + "repositories" + "/" + getRepositoryOwner() + "/" +
                getRepositoryName();
    }
}
