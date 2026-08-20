import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;

import java.io.*;
import java.util.*;

public class JacocoAggregateVerificationTask extends DefaultTask {

    private List<String> execDirPaths = new ArrayList<>();
    private List<String> classDirPaths = new ArrayList<>();
    private double minCoverage = 0.80;

    public JacocoAggregateVerificationTask() {
        setDescription("Verify aggregate JaCoCo coverage across all subprojects");
        setGroup("verification");
    }

    public void setExecDirPaths(List<String> paths) { this.execDirPaths = paths; }
    @org.gradle.api.tasks.Input
    public List<String> getExecDirPaths() { return execDirPaths; }

    public void setClassDirPaths(List<String> paths) { this.classDirPaths = paths; }
    @org.gradle.api.tasks.Input
    public List<String> getClassDirPaths() { return classDirPaths; }

    public void setMinCoverage(double v) { this.minCoverage = v; }
    @org.gradle.api.tasks.Input
    public double getMinCoverage() { return minCoverage; }

    @TaskAction
    public void verify() throws Exception {
        ExecFileLoader loader = new ExecFileLoader();
        for (String execDirPath : execDirPaths) {
            File execDir = new File(execDirPath);
            if (!execDir.exists()) continue;
            File[] execFiles = execDir.listFiles((d, name) -> name.endsWith(".exec"));
            if (execFiles != null) {
                for (File exec : execFiles) {
                    try {
                        loader.load(exec);
                    } catch (Exception e) {
                        getLogger().warn("Failed to load " + exec);
                    }
                }
            }
        }

        ExecutionDataStore dataStore = loader.getExecutionDataStore();
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(dataStore, builder);

        for (String classDirPath : classDirPaths) {
            File dir = new File(classDirPath);
            if (dir.exists()) {
                analyzeClasses(analyzer, dir);
            }
        }

        IBundleCoverage bundle = builder.getBundle("RWAMP");
        ICounter lineCounter = bundle.getLineCounter();

        int total = lineCounter.getTotalCount();
        int covered = lineCounter.getCoveredCount();
        double lineRate = total > 0 ? (double) covered / total : 1.0;

        getLogger().info("Aggregate line coverage: " + String.format("%.1f%%", lineRate * 100)
            + " (" + covered + "/" + total + " lines, min: " + String.format("%.0f%%", minCoverage * 100) + ")");

        if (lineRate < minCoverage) {
            throw new GradleException(
                "Aggregate coverage is " + String.format("%.1f%%", lineRate * 100) + ", "
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
