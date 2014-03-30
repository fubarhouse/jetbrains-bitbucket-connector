package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;

public class HtmlMessageDialog extends DialogWrapper {

    private HtmlPanel myPanel;

    public HtmlMessageDialog(Project project, String text, String title) {
        super(project, false);

        myPanel = new HtmlPanel();
        setTitle(title);
        myPanel.setText(text);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getPanel();
    }

    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}
