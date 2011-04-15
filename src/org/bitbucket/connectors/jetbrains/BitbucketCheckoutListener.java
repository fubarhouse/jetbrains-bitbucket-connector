package org.bitbucket.connectors.jetbrains;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.checkout.CheckoutListener;

import java.io.File;

/**
 * User: leha2000
 * Date: Apr 7, 2011
 * Time: 1:55:35 PM
 */
public class BitbucketCheckoutListener implements CheckoutListener {
    public boolean processCheckedOutDirectory(Project project, File file) {
        return true;
    }

    public void processOpenedProject(Project project) {
    }
}
