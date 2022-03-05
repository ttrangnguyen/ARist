package flute.jdtparser;

import flute.config.Config;
import flute.preprocessing.FileFilter;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodResolutionChecker {
    public static void main(String args[]) {
        AtomicInteger repoCount = new AtomicInteger(0);
        AtomicInteger completeRepoCount = new AtomicInteger(0);
        AtomicInteger fullyResolvableRepoCount = new AtomicInteger(0);
        AtomicInteger nineTenthMinResolvableRepoCount = new AtomicInteger(0);

        List<String> repos = FileProcessor.readLineByLineToList("docs/10000_projects.txt");
        for (int i = 0; i < Math.min(repos.size(), 1500); ++i) {
            String repo = repos.get(i);
            repoCount.incrementAndGet();

            String usr = repo.split("_")[0];
            String name = repo.split("_")[1];
            try {
                repo = repo.replaceAll("/", "_");
                String repoUrl = "https://github.com/" + usr + "/" + name;
                System.out.println("Cloning " + repoUrl + " into " + repo);
                File directory = new File(Config.REPO_DIR + "git/JAVA_repos/" + repo);
                Git result = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(directory)
                        .call();

                Logger.write(String.format("%d", i));
                if (APITest.initProject(repo) == 0) {
                    completeRepoCount.incrementAndGet();

                    AtomicInteger methodInvocationCount = new AtomicInteger(0);
                    AtomicInteger resolvableMethodCount = new AtomicInteger(0);

                    ProjectParser projectParser = APITest.getCurProjectParser();

                    List<File> rawJavaFiles = DirProcessor.walkJavaFile(directory.getAbsolutePath());
                    List<File> javaFiles = FileFilter.filter(rawJavaFiles);

                    for (File javaFile: javaFiles) {
                        CompilationUnit cu = null;
                        try {
                            cu = projectParser.createCU(javaFile.getName(), FileProcessor.read(javaFile));
                        } catch (IllegalArgumentException iae) {
                            iae.printStackTrace();
                            return;
                        }

                        cu.accept(new ASTVisitor() {
                            @Override
                            public boolean visit(TypeDeclaration typeDeclaration) {
                                if (typeDeclaration.isInterface()) return true;
                                typeDeclaration.accept(new ASTVisitor() {
                                    @Override
                                    public boolean visit(MethodInvocation methodInvocation) {
                                        methodInvocationCount.incrementAndGet();
                                        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                                        if (methodBinding != null) {
                                            resolvableMethodCount.incrementAndGet();
                                        }
                                        return true;
                                    }
                                });
                                return true;
                            }
                        });
                    }

                    Logger.write(String.format("Number of complete repositories: %d", completeRepoCount.get()));
                    float resolvableMethodRate = resolvableMethodCount.floatValue() / methodInvocationCount.intValue();
                    Logger.write(String.format("%.2f%% of method invocations can be resolved", resolvableMethodRate * 100));
                    System.out.printf("%.2f%% of method invocations can be resolved\n", resolvableMethodRate * 100);
                    if (resolvableMethodCount.get() == methodInvocationCount.get()) fullyResolvableRepoCount.incrementAndGet();
                    Logger.write(String.format("Number of fully resolvable repositories: %d", fullyResolvableRepoCount.get()));
                    if (resolvableMethodRate >= 0.9) nineTenthMinResolvableRepoCount.incrementAndGet();
                    Logger.write(String.format("Number of repositories whose 90%% of method invocations can be resolved: %d", nineTenthMinResolvableRepoCount.get()));

                    Logger.write(repo, "complete_maven_repos.txt");
                }
            } catch (GitAPIException e) {
                System.out.println("Exception occurred while cloning repo");
                e.printStackTrace();
            } catch (JGitInternalException e) {
                System.out.println("Exception occurred while cloning repo");
                e.printStackTrace();
            }
        }
    }
}
