/**
 * Copyright (C) 2016 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.platform.util;

import static java.util.Arrays.asList;
import static org.bonitasoft.platform.setup.PlatformSetup.PLATFORM_CONF_FOLDER_NAME;
import static org.bonitasoft.platform.setup.ScriptExecutor.ALL_SQL_FILES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Laurent Leseigneur
 */
public class ConfigurationFolderUtil {

    public void buildInitialFolder(Path rootFolder) throws IOException {
        Path initialFolder = rootFolder.resolve(PLATFORM_CONF_FOLDER_NAME).resolve("initial");
        Files.createDirectories(initialFolder.resolve("platform-init"));
        Files.write(initialFolder.resolve("platform-init").resolve("initialConfig.properties"), "key=value".getBytes());
    }

    public void buildCurrentFolder(Path rootFolder) throws IOException {
        Path currentFolder = rootFolder.resolve(PLATFORM_CONF_FOLDER_NAME).resolve("current");
        Files.createDirectories(currentFolder.resolve("platform-init"));
        Files.write(currentFolder.resolve("platform-init").resolve("currentConfig.properties"), "key=value".getBytes());
    }

    public void buildSqlFolder(Path rootFolder, String dbVendor) throws IOException {
        Path sqlPath = rootFolder.resolve(PLATFORM_CONF_FOLDER_NAME).resolve("sql").resolve(dbVendor);
        Files.createDirectories(sqlPath);
        for (String sqlFile : asList(ALL_SQL_FILES)) {
            Files.copy(ConfigurationFolderUtil.class.getResourceAsStream("/sql/" + dbVendor + "/" + sqlFile), sqlPath.resolve(sqlFile));
        }
    }

}