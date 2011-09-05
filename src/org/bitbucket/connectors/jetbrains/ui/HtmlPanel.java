package org.bitbucket.connectors.jetbrains.ui;

import com.intellij.ide.BrowserUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * User: leha
 * Date: 5/11/11
 * Time: 6:05 PM
 */
public class HtmlPanel {
    private JPanel myPane;
    private JTextPane myTextPane;

    public HtmlPanel() {
        myTextPane.setBackground(myPane.getBackground());
        myTextPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.launchBrowser(e.getURL().toExternalForm());
                }
            }
        });

    }

    public void setText(String text) {
        myTextPane.setText("<html>" + text + "</html>");
    }

    public JPanel getPanel() {
        return myPane;
    }
}
