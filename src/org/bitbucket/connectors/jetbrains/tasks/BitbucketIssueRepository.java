package org.bitbucket.connectors.jetbrains.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.impl.LocalTaskImpl;
import com.intellij.tasks.impl.TaskUtil;
import com.intellij.tasks.pivotal.PivotalTrackerRepository;
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
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


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

        return element.getChild("issues").getChildren("resource");
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
        final String description = element.getChildText("content");
        final Ref<Date> created = new Ref<Date>();
        try {
            created.set(parseDate(element, "created_on"));
        } catch (ParseException e) {
            log.warn(e);
        }

        final String kind = element.getChild("metadata").getChildText("kind");
        final TaskType taskType =
                kind.equals("bug") ? TaskType.BUG : (kind.equals("enhancement") ? TaskType.FEATURE : TaskType.OTHER);

        final String state = element.getChildText("status");
        final TaskState taskState = mapState(state);

        final boolean isClosed = taskState.equals(TaskState.RESOLVED);

        //  /1.0/repositories/sylvanaar2/lua-for-idea/issues/79
        final String issueUrl = element.getChildText("resource_uri").replace("/1.0/repositories", "").replace("/issues/", "/issue/");


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
                return created.get();
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
                return MessageFormat.format("#{0}: {1}", getId(), getSummary());
            }
        });
    }

    private static Date parseDate(final Element element, final String name) throws ParseException {
        try {
            Method parseDateMethod = getParseDateMethod();
            if (parseDateMethod != null) {
                return (Date) parseDateMethod.invoke(null, element, name);
            } else { // fix for newer versions without PivotalTrackerRepository.parseDate() method
                String val = element.getChildText(name);
                return TaskUtil.parseDate(val);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Method getParseDateMethod() {
        final Class[] PARAMETER_TYPES = new Class[] { Element.class, String.class };
        for (Method method: PivotalTrackerRepository.class.getMethods()) {
            if ("parseDate".equals(method.getName()) && Arrays.equals(PARAMETER_TYPES, method.getParameterTypes())) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
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
