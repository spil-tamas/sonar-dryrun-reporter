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

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

import java.util.List;

@Properties({
  @Property(
    key = DryrunReporterPlugin.REPORT_LOCATION_KEY,
    defaultValue = DryrunReporterPlugin.REPORT_LOCATION_DEFAULT_VALUE,
    name = "The location of the report",
    description = "Name and path of the file to generate the report.",
    global = true, project = true)
})
public class DryrunReporterPlugin extends SonarPlugin {

  public static final String REPORT_LOCATION_KEY ="sonar.dryrunreporter.location";
  public static final String REPORT_LOCATION_DEFAULT_VALUE ="dryrun-results.xml";

  public List<?> getExtensions() {
    return ImmutableList.of(XunitFormat.class);
  }

}
