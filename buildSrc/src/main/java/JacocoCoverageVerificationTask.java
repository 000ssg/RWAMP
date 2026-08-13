import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.*;

public class JacocoCoverageVerificationTask extends DefaultTask {

    private File execFile;
    private File classesDir;
    private double minCoverage = 0.50;

    public JacocoCoverageVerificationTask() {
        setDescription("Verify JaCoCo coverage meets minimum threshold");
        setGroup("verification");
    }

    public void setExecFile(File f) { this.execFile = f; }
    @InputFile
    public File getExecFile() { return execFile; }

    public void setClassesDir(File d) { this.classesDir = d; }
    @InputDirectory
    @Optional
    public File getClassesDir() { return classesDir; }

    public void setMinCoverage(double v) { this.minCoverage = v; }
    @org.gradle.api.tasks.Input
    public double getMinCoverage() { return minCoverage; }

    @TaskAction
    public void verify() throws Exception {
        if (!execFile.exists()) {
            throw new GradleException("No JaCoCo execution data found for " + getProject().getName() + ". Run tests first.");
        }

        ExecFileLoader loader = new ExecFileLoader();
        loader.load(execFile);
        ExecutionDataStore dataStore = loader.getExecutionDataStore();

        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(dataStore, builder);

        if (classesDir.exists()) {
            analyzeClasses(analyzer, classesDir);
        }

        IBundleCoverage bundle = builder.getBundle(getProject().getName());
        ICounter lineCounter = bundle.getLineCounter();

        int total = lineCounter.getTotalCount();
        int covered = lineCounter.getCoveredCount();
        double lineRate = total > 0 ? (double) covered / total : 1.0;

        getLogger().info("Line coverage: " + String.format("%.1f%%", lineRate * 100)
            + " (" + covered + "/" + total + " lines, min: " + String.format("%.0f%%", minCoverage * 100) + ")");

        if (lineRate < minCoverage) {
            throw new GradleException(
                "Coverage for " + getProject().getName() + " is " + String.format("%.1f%%", lineRate * 100) + ", "
                + "below minimum " + String.format("%.0f%%", minCoverage * 100) + " threshold."
            );
        }
    }

    private void analyzeClasses(Analyzer analyzer, File dir) throws Exception {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (entry.getName().endsWith(".class")) {
                try (InputStream is = new FileInputStream(entry)) {
                    analyzer.analyzeAll(is, entry.getName());
                }
            } else if (entry.isDirectory()) {
                analyzeClasses(analyzer, entry);
            }
        }
    }
}
