package org.bitbucket.connectors.jetbrains;


import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.PasswordUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * User: leha2000
 * Date: Apr 7, 2011
 * Time: 1:01:55 PM
 */
@State(
        name = "BitbucketSettings",
        storages = {
                @Storage(
                        id = "main",
                        file = "$APP_CONFIG$/bitbucket_settings.xml"
                )}
)
public class BitbucketSettings implements PersistentStateComponent<Element> {

    private static final String SETTINGS_TAG = "BitbucketSettings";
    private static final String LOGIN = "Login";
    private static final String PASSWORD = "Password";

    private String myLogin;
    private String myPassword;

    public static BitbucketSettings getInstance() {
        return ServiceManager.getService(BitbucketSettings.class);
    }

    public Element getState() {
        if (StringUtil.isEmptyOrSpaces(myLogin) && StringUtil.isEmptyOrSpaces(myPassword)) {
            return null;
        }
        final Element element = new Element(SETTINGS_TAG);
        element.setAttribute(LOGIN, getLogin());
        element.setAttribute(PASSWORD, getEncodedPassword());
        return element;
    }

    public String getEncodedPassword() {
        return PasswordUtil.encodePassword(getPassword());
    }

    public void setEncodedPassword(final String password) {
        try {
            setPassword(PasswordUtil.decodePassword(password));
        }
        catch (NumberFormatException e) {
            // do nothing
        }
    }

    public void loadState(@NotNull final Element element) {
        try {
            setLogin(element.getAttributeValue(LOGIN));
            setEncodedPassword(element.getAttributeValue(PASSWORD));
        }
        catch (Exception e) {
            // ignore
        }
    }

    @NotNull
    public String getLogin() {
        return myLogin != null ? myLogin : "";
    }

    @NotNull
    public String getPassword() {
        return myPassword != null ? myPassword : "";
    }

    public void setLogin(String login) {
        myLogin = login != null ? login : "";
    }

    public void setPassword(String password) {
        myPassword = password != null ? password : "";
    }
}
