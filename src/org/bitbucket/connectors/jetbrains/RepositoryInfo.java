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
            return "ssh://" + user + "@" + BitbucketUtil.BITBUCKET_DN + "/" + owner + "/" + name;
        } else {
            String cred = URIUtil.encodeWithinAuthority(settings.getLogin());
            if (addPassword && !isGit()) { // todo: provide password for GIT
                cred += ":" + URIUtil.encodeWithinAuthority(settings.getPassword());
            }
            return "https://" + cred + "@" + BitbucketUtil.BITBUCKET_DN + "/" + owner + "/" + name;
        }
    }

    public static String addPassword(String url, boolean git) {
        if (url == null) {
            return null;
        }

        int pos = url.indexOf("@");
        if (pos != -1 && !git) {
            int start = url.lastIndexOf("/", pos);
            if (start != -1) {
                String name = url.substring(start + 1, pos);
                if (name.indexOf(":") != -1) {
                    return url;
                }
            }
            String password = BitbucketSettings.getInstance().getPassword();
            return url.substring(0, pos) + ":" + password + url.substring(pos);
        }
        return url;
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
