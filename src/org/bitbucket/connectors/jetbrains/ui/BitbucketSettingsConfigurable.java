package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.bitbucket.connectors.jetbrains.BitbucketSettings;
import org.bitbucket.connectors.jetbrains.BitbucketUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * User: leha2000
 * Date: Apr 7, 2011
 * Time: 2:01:11 PM
 */
public class BitbucketSettingsConfigurable implements SearchableConfigurable {

    private BitbucketSettingsPanel mySettingsPane;
    private final BitbucketSettings mySettings;

    public BitbucketSettingsConfigurable() {
      mySettings = BitbucketSettings.getInstance();
    }

    public String getDisplayName() {
      return "Bitbucket";
    }

    public Icon getIcon() {
      return BitbucketUtil.ICON;
    }

    public String getHelpTopic() {
      return "settings.bitbucket";
    }

    public JComponent createComponent() {
      if (mySettingsPane == null) {
        mySettingsPane = new BitbucketSettingsPanel();
      }
      reset();
      return mySettingsPane.getPanel();
    }

    public boolean isModified() {
      return mySettingsPane == null || !mySettings.getLogin().equals(mySettingsPane.getLogin()) ||
             !mySettings.getPassword().equals(mySettingsPane.getPassword());
    }

    public void apply() throws ConfigurationException {
      if (mySettingsPane != null) {
        mySettings.setLogin(mySettingsPane.getLogin());
        mySettings.setPassword(mySettingsPane.getPassword());
      }
    }

    public void reset() {
      if (mySettingsPane != null) {
        mySettingsPane.setLogin(mySettings.getLogin());
        mySettingsPane.setPassword(mySettings.getPassword());
      }
    }

    public void disposeUIResources() {
      mySettingsPane = null;
    }

    @NotNull
    public String getId() {
      return getHelpTopic();
    }

    public Runnable enableSearch(String option) {
      return null;
    }
}
