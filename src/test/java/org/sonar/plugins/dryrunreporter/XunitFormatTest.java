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

import org.sonar.api.i18n.RuleI18n;

import org.sonar.api.rules.RuleFinder;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XunitFormatTest {

  @Before
  public void setup() throws URISyntaxException, IOException {
    ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
    Settings settings = mock(Settings.class);
    Project project = new Project("dummy");
    SensorContext context = mock(SensorContext.class);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    RuleI18n ruleI18n = mock(RuleI18n.class);

    ModuleFileSystem fileSystem = mockModuleFileSystem(null, null);

    new XunitFormat(settings, fileSystem, resourcePerspectives, ruleFinder, ruleI18n).executeOn(project, context);
  }

  @Test
  public void checkReport() throws URISyntaxException {

  }

  private ModuleFileSystem mockModuleFileSystem(List<File> srcFiles, List<File> testFiles) {
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    when(fileSystem.sourceCharset()).thenReturn(Charset.forName("UTF-8"));
    return fileSystem;
  }

}
