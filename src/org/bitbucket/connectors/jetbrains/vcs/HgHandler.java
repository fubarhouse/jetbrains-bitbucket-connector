package org.bitbucket.connectors.jetbrains.vcs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.bitbucket.connectors.jetbrains.ProjectUtil;
import org.bitbucket.connectors.jetbrains.ui.BitbucketBundle;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.*;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;
import org.zmlx.hg4idea.util.HgUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User: leha2000
 * Date: Oct 14, 2011
 * Time: 11:34:36 AM
 */
public class HgHandler implements VcsHandler {

    public boolean checkout(Project project, String folder, String repositoryUrl) {
        HgCloneCommand cmd = new HgCloneCommand(project);
        cmd.setRepositoryURL(repositoryUrl);
        cmd.setDirectory(folder);

        HgCommandResult result = cmd.execute();

        if (result == null) {
            deleteEmptyFolder(folder);
            error(project, BitbucketBundle.message("clone-failed"), BitbucketBundle.message("clone-failed-unknown-err"));
            return false;
        } else if (result.getExitValue() != 0) {
            deleteEmptyFolder(folder);
            error(project, BitbucketBundle.message("clone-failed"), BitbucketBundle.message("clone-failed-msg", repositoryUrl, result.getRawError() + "\n" + result.getRawOutput()));
            return false;
        }
        return true;
    }

    private void deleteEmptyFolder(String folder) {
        File f = new File(folder);
        if (f.exists() && f.isDirectory() && f.list().length == 0) {
            f.delete();
        }
    }

    private static void error(final Project project, final String title, final String msg) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            public void run() {
                Messages.showErrorDialog(project, msg, title);
            }
        });
    }

    public VirtualFile getRepositoryRoot(VirtualFile folder) {
        return HgUtil.getNearestHgRoot(folder);
    }

    public boolean initRepository(final Project project, final VirtualFile root) {
        final Boolean[] res = new Boolean[1];
        new HgInitCommand(project).execute(root, new Consumer<Boolean>() {
            public void consume(Boolean aBoolean) {
                synchronized (res) {
                    res[0] = Boolean.TRUE.equals(aBoolean);
                    res.notify();
                }
            }
        });

        try {
            synchronized (res) {
                if (res[0] == null) {
                    res.wait();
                }
            }
        } catch (InterruptedException e) {
            return false;
        }

        final Exception[] exc = new Exception[1];

        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            public void run() {
                ProgressManager.getInstance().getProgressIndicator().setText(BitbucketBundle.message("share-project-on-bitbucket"));
                new HgAddCommand(project).execute(ProjectUtil.getSourceFolders(project, root));
                try {
                    new HgCommitCommand(project, root, BitbucketBundle.message("initial-rev-msg")).execute();
                } catch (HgCommandException e) {
                    exc[0] = e;
                } catch (VcsException e) {
                    exc[0] = e;
                }
            }
        }, BitbucketBundle.message("create-local-repository"), true, project);

        return exc[0] == null;
    }

    public boolean push(Project project, VirtualFile root, String repositoryUrl) {
        final Boolean[] res = new Boolean[1];
        new HgPushCommand(project, root, repositoryUrl).execute(new HgCommandResultHandler() {
            public void process(@Nullable HgCommandResult result) {
                synchronized (res) {
                    res[0] = result != null && result.getExitValue() == 0;
                    res.notify();
                }
            }
        });
        try {
            synchronized (res) {
                if (res[0] == null) {
                    res.wait();
                }
            }
            return res[0] == Boolean.TRUE;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void setRepositoryDefaultUrl(Project project, VirtualFile root, String repositoryUrl) throws IOException {
        File hgrc = new File(new File(root.getPath(), ".hg"), "hgrc");
        if (!hgrc.exists()) {
            hgrc.createNewFile();
        }

        List<String> lines = new ArrayList<String>();
        List<String> pathLines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(hgrc));
        try {
            String line;
            boolean paths = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[")) {
                    paths = "[paths]".equals(line);
                    if (!paths) {
                        lines.add(line);
                    }
                    continue;
                }

                if (paths) {
                    if (!"default".equals(getKey(line))) {
                        pathLines.add(line);
                    }
                } else {
                    lines.add(line);
                }
            }
        } finally {
            reader.close();
        }

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(hgrc, false)));
        try {
            writer.println("[paths]");
            writer.println("default=" + repositoryUrl);

            for (String line: pathLines) {
                writer.println(line);
            }

            for (String line: lines) {
                writer.println(line);
            }
        } finally {
            writer.close();
        }
    }

    private static String getKey(String s) {
        String[] parts = s.split("=");
        return parts.length > 0 ? parts[0].trim() : null;
    }

    public AbstractVcs getVcs(Project project) {
        return HgVcs.getInstance(project);
    }
}
