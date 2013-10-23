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

import org.sonar.api.batch.SonarIndex;

import org.sonar.api.issue.IssueQuery;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
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

@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public class XunitFormat implements PostJob {

  private Settings settings;
  private ResourcePerspectives resourcePerspectives;
  private ModuleFileSystem fileSystem;

  public XunitFormat(Settings settings, ModuleFileSystem fileSystem, ResourcePerspectives resourcePerspectives) {
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.resourcePerspectives = resourcePerspectives;
  }

  public void executeOn(Project project, SensorContext context) {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = dom.createElement("testsuite");
        dom.appendChild(root);

        reportAlerts(context, dom, root);
        reportIssues(project, dom, root);

        File report = new File(fileSystem.baseDir(), "dryrun-results.xml");
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

  private void reportIssues(Project project, Document dom, Element root) {
    Issuable issuable = resourcePerspectives.as(Issuable.class, (Resource) project);
    for (Issue issue : issuable.issues()) {
      Element testCase = dom.createElement("testcase");
      Element error = dom.createElement("error");
      error.setAttribute("message", issue.message());
      testCase.appendChild(error);
      root.appendChild(testCase);
    }
  }

  private void reportAlerts(SensorContext context, Document dom, Element root) {
    Collection<Measure> measures = context.getMeasures(MeasuresFilters.all());
    for (Measure measure : measures) {
      if (isErrorAlert(measure) || isWarningAlert(measure)) {
        Element testCase = dom.createElement("testcase");
        Element error = dom.createElement("error");
        error.setAttribute("message", measure.getAlertText());
        testCase.appendChild(error);
        root.appendChild(testCase);
      }
    }
  }

  private boolean isWarningAlert(Measure measure) {
    return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS) && Metric.Level.WARN.equals(measure.getAlertStatus());
  }

  private boolean isErrorAlert(Measure measure) {
    return !measure.getMetric().equals(CoreMetrics.ALERT_STATUS) && Metric.Level.ERROR.equals(measure.getAlertStatus());
  }

}
