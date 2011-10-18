package org.bitbucket.connectors.jetbrains;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.jdom.Element;

public class RepositoryInfo implements Comparable<RepositoryInfo> {

    private Element myRepositoryElement;
    private String myCheckoutUrl;

    RepositoryInfo(Element repositoryElement) {
        myRepositoryElement = repositoryElement;
    }

    public String getName() {
        return myRepositoryElement.getChildText("name");
    }

    public String getSlug() {
        return myRepositoryElement.getChildText("slug");
    }

    public String getOwner() {
        return myRepositoryElement.getChildText("owner");
    }

    public String isPrivate() {
        return myRepositoryElement.getChildText("is_private");
    }

    public String getCheckoutUrl() throws URIException {
        if (myCheckoutUrl == null) {
            myCheckoutUrl = calculateCheckoutUrl();
        }
        return myCheckoutUrl;
    }

    public boolean isGit() {
        return "git".equals(myRepositoryElement.getChild("scm").getText());
    }

    private String calculateCheckoutUrl() throws URIException {
        BitbucketSettings settings = BitbucketSettings.getInstance();
        boolean ssh = BitbucketUtil.sshEnabled(null, settings.getLogin(), settings.getPassword());

        return getCheckoutUrl(ssh, true);
    }

    public String getCheckoutUrl(boolean ssh, boolean addPassword) throws URIException {
        BitbucketSettings settings = BitbucketSettings.getInstance();

        String name = getSlug();
        if (isGit()) {
            name += ".git";
        }
        String owner = getOwner();
        if (ssh) {
            String user = isGit() ? "git" : "hg";
            return "ssh://" + user + "@" + BitbucketUtil.BITBUCKET + "/" + owner + "/" + name;
        } else {
            String cred = URIUtil.encodeWithinAuthority(settings.getLogin());
            if (addPassword && !isGit()) { // todo: provide password for GIT
                cred += ":" + URIUtil.encodeWithinAuthority(settings.getPassword());
            }
            return "https://" + cred + "@" + BitbucketUtil.BITBUCKET + "/" + owner + "/" + name;
        }
    }

    public boolean isMy() {
        return BitbucketSettings.getInstance().getLogin().equals(getOwner());
    }

    public int compareTo(RepositoryInfo r) {
        if (r == this) {
            return 0;
        }
        if (r == null) {
            return -1;
        }

        if (isMy() == r.isMy()) {
            int res = getOwner().compareTo(r.getOwner());
            if (res != 0) {
                return res;
            }
            return getName().compareTo(r.getName());
        } else {
            return isMy() ? -1 : 1;
        }
    }

    public boolean isCreating() {
        Element state = myRepositoryElement.getChild("state");
        return state != null && "creating".equals(state.getText());
    }
}
