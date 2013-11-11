/*
 * Sonar DryRun Reporter
 * Copyright (C) 2013 Tamas Kende
 * kende.tamas@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dryrunreporter;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.RuleI18n;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

public class XunitFormat implements PostJob {

  Logger logger = LoggerFactory.getLogger(getClass());

  private Settings settings;
  private ModuleFileSystem fileSystem;
  private RuleFinder ruleFinder;
  private RuleI18n ruleI18n;

  private ProjectIssues projectIssues;

  public XunitFormat(Settings settings, ModuleFileSystem fileSystem, ProjectIssues projectIssues,
      RuleFinder ruleFinder, RuleI18n ruleI18n) {
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.projectIssues = projectIssues;
    this.ruleFinder = ruleFinder;
    this.ruleI18n = ruleI18n;
  }

  public void executeOn(Project project, SensorContext context) {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = dom.createElement("testsuites");
        dom.appendChild(root);

        int alerts = reportAlerts(context, dom, root);
        int issues = reportIssues(context, dom, root);

        root.setAttribute("errors", String.valueOf(alerts + issues));

        if ((issues + alerts) == 0) {
          root.setAttribute("tests", "2");
          Element testCase = dom.createElement("testcase");
          testCase.setAttribute("classname", "issues");
          testCase.setAttribute("name", "passing");
          root.appendChild(testCase);

          testCase = dom.createElement("testcase");
          testCase.setAttribute("classname", "alerts");
          testCase.setAttribute("name", "passing");
          root.appendChild(testCase);
        } else {
          root.setAttribute("tests", String.valueOf(alerts + issues));
        }
        root.setAttribute("name", "Sonar dryrun results");

        File report = new File(fileSystem.baseDir(), settings.getString(DryrunReporterPlugin.REPORT_LOCATION_KEY));
        report.createNewFile();

        OutputFormat format = new OutputFormat(dom);
        format.setIndenting(true);

        XMLSerializer ser = new XMLSerializer(new FileOutputStream(report), format);
        ser.serialize(dom);

      } catch (ParserConfigurationException e) {
      } catch (IOException e) {
      }
    }
  }

  private int reportIssues(final SensorContext context, final Document dom, final Element root) {
    int counterOld = 0;
    int counterNew = 0;
    Element oldIssues = dom.createElement("testsuite");
    oldIssues.setAttribute("name", "Old issues");
    root.appendChild(oldIssues);

    Element newIssues = dom.createElement("testsuite");
    newIssues.setAttribute("name", "New issues");
    root.appendChild(newIssues);

    for (Issue issue : projectIssues.issues()) {
      Rule rule = ruleFinder.findByKey(issue.ruleKey());
      Element testCase = dom.createElement("testcase");
      Element error = dom.createElement("error");
      // TODO: with api 4.0 we can get the isNew
      testCase.setAttribute("classname", "issues");
      error.setAttribute("message", ruleI18n.getName(rule, Locale.ENGLISH));
      testCase.setAttribute("name", rule.getRepositoryKey() + ":" + rule.getKey());
      error.setTextContent(
          issue.severity() + "\n"
            + ruleI18n.getName(rule, Locale.ENGLISH)
            + "\nin file:"
            + issue.componentKey() + ":" + ((issue.line() != null) ? issue.line() : "")
            + "\nlink: "
            + settings.getString("sonar.host.url") + "/rules/show/" + rule.getRepositoryKey() + ":" + rule.getConfigKey()
          );
      logger.debug("Rule: " + ruleI18n.getName(rule, Locale.ENGLISH));
      logger.debug("Rule: " + issue.message());
      testCase.appendChild(error);
      if(issue.isNew()){
        newIssues.appendChild(testCase);
        counterNew++;
      }else {
        oldIssues.appendChild(testCase);
        counterOld++;
      }
      logger.debug("Saved issue severity: " + issue.severity());
    }
    oldIssues.setAttribute("errors", String.valueOf(counterOld));
    newIssues.setAttribute("errors", String.valueOf(counterNew));

    oldIssues.setAttribute("tests", String.valueOf(counterOld+1));
    newIssues.setAttribute("tests", String.valueOf(counterNew+1));

    return counterNew+counterOld;
  }

  private int reportAlerts(SensorContext context, Document dom, Element root) {
    int counter = 0;
    Collection<Measure> measures = context.getMeasures(MeasuresFilters.all());
    Element suite = dom.createElement("testsuite");
    suite.setAttribute("name", "Alerts");
    root.appendChild(suite);

    for (Measure measure : measures) {
      if (isErrorAlert(measure) || isWarningAlert(measure)) {
        Element testCase = dom.createElement("testcase");
        testCase.setAttribute("classname", "alerts");
        testCase.setAttribute("name", measure.getAlertText());
        Element error = dom.createElement("error");
        error.setAttribute("message", measure.getAlertText());
        String status = (isErrorAlert(measure)) ? "ERROR" : "WARNING";
        error.setTextContent(status + "\n"
          + measure.getAlertText()
          + "\n");
        testCase.appendChild(error);
        suite.appendChild(testCase);
        counter++;
      }
    }
    suite.setAttribute("errors", String.valueOf(counter));
    suite.setAttribute("tests", String.valueOf(counter+1));
    return counter;
  }

  private boolean isWarningAlert(Measure measure) {
    return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS) && Metric.Level.WARN.equals(measure.getAlertStatus());
  }

  private boolean isErrorAlert(Measure measure) {
    return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS) && Metric.Level.ERROR.equals(measure.getAlertStatus());
  }

}
