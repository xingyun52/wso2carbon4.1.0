package org.wso2.carbon.automation.core.utils.reportutills;

import org.apache.maven.surefire.report.AbstractReporter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.shade.org.codehaus.plexus.util.IOUtil;
import org.apache.maven.surefire.shade.org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.surefire.shade.org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.apache.maven.surefire.util.PrettyPrintXMLWriter;

import java.io.*;
import java.util.*;


public class XmlReporter
        extends AbstractReporter {
    private static final String LS = System.getProperty("line.separator");

    private final File reportsDirectory;

    private final boolean deleteOnStarting;

    private final String reportNameSuffix;

    private final List results = Collections.synchronizedList(new ArrayList());


    public XmlReporter(boolean trimStackTrace, File reportsDirectory) {
        this(trimStackTrace, reportsDirectory, null);
    }

    public XmlReporter(boolean trimStackTrace, File reportsDirectory, String reportNameSuffix) {
        super(false);
        this.reportsDirectory = reportsDirectory;
        this.deleteOnStarting = true;
        this.reportNameSuffix = reportNameSuffix;
    }


    public void writeMessage(String message) {
    }


    public void writeDetailMessage(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMessage(byte[] b, int off, int len) {
    }


    public void testSetStarting(ReportEntry report)
            throws ReporterException {
        super.testSetStarting(report);

        if (deleteOnStarting) {
            final File reportFile = getReportFile(report);
            deleteIfExisting(reportFile);
        }
    }

    public void testSetCompleted(ReportEntry report)
            throws ReporterException {
        super.testSetCompleted(report);

        long runTime = System.currentTimeMillis() - testSetStartTime;

        Xpp3Dom testSuite = createTestSuiteElement(report, runTime);

        showProperties(testSuite);

        testSuite.setAttribute("tests", String.valueOf(this.getNumTests()));

        testSuite.setAttribute("errors", String.valueOf(this.getNumErrors()));

        testSuite.setAttribute("skipped", String.valueOf(this.getNumSkipped()));

        testSuite.setAttribute("failures", String.valueOf(this.getNumFailures()));

        for (Iterator i = results.iterator(); i.hasNext(); ) {
            Xpp3Dom testcase = (Xpp3Dom) i.next();
            testSuite.addChild(testcase);
        }

        File reportFile = getReportFile(report);

        File reportDir = reportFile.getParentFile();

        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();

        PrintWriter writer = null;

        try {
            writer = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8")));

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + LS);

            Xpp3DomWriter.write(new PrettyPrintXMLWriter(writer), testSuite);
        } catch (UnsupportedEncodingException e) {
            throw new ReporterException("Unable to use UTF-8 encoding", e);
        } catch (FileNotFoundException e) {
            throw new ReporterException("Unable to create file: " + e, e);
        } finally {
            IOUtil.close(writer);
        }
    }

    private File getReportFile(ReportEntry report) {
        File reportFile;

        if (reportNameSuffix != null && reportNameSuffix.length() > 0) {
            reportFile = new File(reportsDirectory, "TEST-" + report.getName() + "-" + reportNameSuffix + ".xml");
        } else {
            reportFile = new File(reportsDirectory, "TEST-" + report.getName() + ".xml");
        }

        return reportFile;
    }

    private String getReportName(ReportEntry report) {
        String reportName;

        if (report.getName().indexOf("(") > 0) {
            reportName = report.getName().substring(0, report.getName().indexOf("("));
        } else {
            reportName = report.getName();
        }
        return reportName;
    }

    public void testSucceeded(ReportEntry report) {
        super.testSucceeded(report);

        long runTime = getActualRunTime(report);

        Xpp3Dom testCase = createTestElement(report, runTime);

        results.add(testCase);
    }

    private Xpp3Dom createTestElement(ReportEntry report, long runTime) {
        Xpp3Dom testCase = new Xpp3Dom("testcase");
        testCase.setAttribute("name", getReportName(report));
        if (report.getGroup() != null) {
            testCase.setAttribute("group", report.getGroup());
        }
        if (report.getSourceName() != null) {
            if (reportNameSuffix != null && reportNameSuffix.length() > 0) {
                testCase.setAttribute("classname", report.getSourceName() + "(" + reportNameSuffix + ")");
            } else {
                testCase.setAttribute("classname", report.getSourceName());
            }
        }
        testCase.setAttribute("time", elapsedTimeAsString(runTime));
        return testCase;
    }

    private Xpp3Dom createTestSuiteElement(ReportEntry report, long runTime) {
        Xpp3Dom testCase = new Xpp3Dom("testsuite");

        if (reportNameSuffix != null && reportNameSuffix.length() > 0) {
            testCase.setAttribute("name", getReportName(report) + "(" + reportNameSuffix + ")");
        } else {
            testCase.setAttribute("name", getReportName(report));
        }
        if (report.getGroup() != null) {
            testCase.setAttribute("group", report.getGroup());
        }
        testCase.setAttribute("time", elapsedTimeAsString(runTime));
        return testCase;
    }

    public void testError(ReportEntry report, String stdOut, String stdErr) {
        super.testError(report, stdOut, stdErr);

        writeTestProblems(report, stdOut, stdErr, "error");
    }

    public void testFailed(ReportEntry report, String stdOut, String stdErr) {
        super.testFailed(report, stdOut, stdErr);

        writeTestProblems(report, stdOut, stdErr, "failure");
    }

    public void testSkipped(ReportEntry report) {
        super.testSkipped(report);
        writeTestProblems(report, null, null, "skipped");
    }

    private void writeTestProblems(ReportEntry report, String stdOut, String stdErr, String name) {
        long runTime = getActualRunTime(report);

        Xpp3Dom testCase = createTestElement(report, runTime);

        Xpp3Dom element = createElement(testCase, name);

        String stackTrace = getStackTrace(report);

        Throwable t = null;
        if (report.getStackTraceWriter() != null) {
            t = report.getStackTraceWriter().getThrowable();
        }

        if (t != null) {

            String message = t.getMessage();

            if (message != null) {
                element.setAttribute("message", message);

                element.setAttribute("type", (stackTrace.indexOf(":") > -1
                                              ? stackTrace.substring(0, stackTrace.indexOf(":"))
                                              : stackTrace));
            } else {
                element.setAttribute("type", new StringTokenizer(stackTrace).nextToken());
            }
        }

        if (stackTrace != null) {
            element.setValue(stackTrace);
        }

        addOutputStreamElement(stdOut, "system-out", testCase);

        addOutputStreamElement(stdErr, "system-err", testCase);

        results.add(testCase);
    }

    private void addOutputStreamElement(String stdOut, String name, Xpp3Dom testCase) {
        if (stdOut != null && stdOut.trim().length() > 0) {
            createElement(testCase, name).setValue(stdOut);
        }
    }

    private Xpp3Dom createElement(Xpp3Dom element, String name) {
        Xpp3Dom component = new Xpp3Dom(name);

        element.addChild(component);

        return component;
    }

    /**
     * Adds system properties to the XML report.
     *
     * @param testSuite The test suite to report to
     */
    private void showProperties(Xpp3Dom testSuite) {
        Xpp3Dom properties = createElement(testSuite, "properties");

        Properties systemProperties = System.getProperties();

        if (systemProperties != null) {
            Enumeration propertyKeys = systemProperties.propertyNames();

            while (propertyKeys.hasMoreElements()) {
                String key = (String) propertyKeys.nextElement();

                String value = systemProperties.getProperty(key);

                if (value == null) {
                    value = "null";
                }

                Xpp3Dom property = createElement(properties, "property");

                property.setAttribute("name", key);

                property.setAttribute("value", value);

            }
        }
    }

    public Iterator getResults() {
        return results.iterator();
    }

    public void reset() {
        results.clear();
        super.reset();
    }
}
