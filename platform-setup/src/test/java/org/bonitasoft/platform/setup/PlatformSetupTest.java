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

package org.bonitasoft.platform.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.platform.configuration.type.ConfigurationType.PLATFORM_ENGINE;
import static org.bonitasoft.platform.configuration.type.ConfigurationType.TENANT_TEMPLATE_PORTAL;
import static org.bonitasoft.platform.setup.PlatformSetup.BONITA_SETUP_FOLDER;
import static org.bonitasoft.platform.setup.PlatformSetup.PLATFORM_CONF_FOLDER_NAME;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.platform.configuration.model.FullBonitaConfiguration;
import org.bonitasoft.platform.configuration.type.ConfigurationType;
import org.bonitasoft.platform.configuration.util.AllConfigurationResourceVisitor;
import org.bonitasoft.platform.setup.jndi.MemoryJNDISetup;
import org.bonitasoft.platform.util.ConfigurationFolderUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Baptiste Mesta
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        PlatformSetupApplication.class
})
public class PlatformSetupTest {

    @Rule
    public final ClearSystemProperties bonitaSetupFolder = new ClearSystemProperties(BONITA_SETUP_FOLDER);

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Value("${db.vendor}")
    String dbVendor;

    @Autowired
    MemoryJNDISetup memoryJNDISetup;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformSetup platformSetup;

    private ConfigurationFolderUtil configurationFolderUtil = new ConfigurationFolderUtil();

    @After
    public void after() throws Exception {
        System.clearProperty(BONITA_SETUP_FOLDER);
        platformSetup.destroy();
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void init_method_should_init_table_and_insert_conf() throws Exception {
        //when
        platformSetup.init();

        //then
        final Integer sequences = jdbcTemplate.queryForObject("select count(*) from sequence", Integer.class);
        assertThat(sequences).isGreaterThan(1);
        final int platformRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "platform");
        assertThat(platformRows).isEqualTo(1);
        final int configurationFiles = JdbcTestUtils.countRowsInTable(jdbcTemplate, "configuration");
        assertThat(configurationFiles).isGreaterThan(1);
    }

    @Test
    public void init_method_should_init_configuration_from_folder_if_exists() throws Exception {
        //given
        File setupFolder = temporaryFolder.newFolder("conf");
        System.setProperty(BONITA_SETUP_FOLDER, setupFolder.getAbsolutePath());
        FileUtils.write(setupFolder.toPath().resolve(PLATFORM_CONF_FOLDER_NAME).resolve("initial").resolve("platform_init_engine")
                .resolve("bonita-platform-init-community.properties").toFile(), "custom content");
        configurationFolderUtil.buildSqlFolder(setupFolder.toPath(), dbVendor);

        //when
        platformSetup.init();

        //then
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList("SELECT * FROM CONFIGURATION WHERE resource_name = 'bonita-platform-init-community.properties'");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("resource_content")).isEqualTo("custom content".getBytes());
    }

    @Test
    public void init_method_should_store_tenant_portal_resources_from_classpath() throws Exception {
        //when
        platformSetup.init();
        //then
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList("SELECT * FROM CONFIGURATION WHERE content_type= '" + ConfigurationType.TENANT_TEMPLATE_PORTAL + "' ORDER BY resource_name");
        assertThat(rows).hasSize(8);
        assertThat(rows.get(0).get("RESOURCE_NAME")).isEqualTo("authenticationManager-config.properties");
        assertThat(rows.get(1).get("RESOURCE_NAME")).isEqualTo("compound-permissions-mapping.properties");
        assertThat(rows.get(2).get("RESOURCE_NAME")).isEqualTo("console-config.properties");
        assertThat(rows.get(3).get("RESOURCE_NAME")).isEqualTo("custom-permissions-mapping.properties");
        assertThat(rows.get(4).get("RESOURCE_NAME")).isEqualTo("dynamic-permissions-checks.properties");
        assertThat(rows.get(5).get("RESOURCE_NAME")).isEqualTo("forms-config.properties");
        assertThat(rows.get(6).get("RESOURCE_NAME")).isEqualTo("resources-permissions-mapping.properties");
        assertThat(rows.get(7).get("RESOURCE_NAME")).isEqualTo("security-config.properties");
    }

    @Test
    public void init_method_should_store_platform_portal_resources_from_classpath() throws Exception {
        //when
        platformSetup.init();
        //then
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList("SELECT * FROM CONFIGURATION WHERE content_type= '" + ConfigurationType.PLATFORM_PORTAL + "' ORDER BY resource_name");
        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).get("RESOURCE_NAME")).isEqualTo("cache-config.xml");
        assertThat(rows.get(1).get("RESOURCE_NAME")).isEqualTo("jaas-standard.cfg");
        assertThat(rows.get(2).get("RESOURCE_NAME")).isEqualTo("platform-tenant-config.properties");
        assertThat(rows.get(3).get("RESOURCE_NAME")).isEqualTo("security-config.properties");
    }

    @Test
    public void init_method_should_store_security_scripts_from_classpath() throws Exception {
        //when
        platformSetup.init();
        //then
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList(
                        "SELECT * FROM CONFIGURATION WHERE content_type= '" + ConfigurationType.TENANT_TEMPLATE_SECURITY_SCRIPTS + "' ORDER BY resource_name");
        assertThat(rows).hasSize(19);
        assertThat(rows.get(0).get("RESOURCE_NAME")).isEqualTo("ActorMemberPermissionRule.groovy");
        assertThat(rows.get(1).get("RESOURCE_NAME")).isEqualTo("ActorPermissionRule.groovy");
        assertThat(rows.get(2).get("RESOURCE_NAME")).isEqualTo("CaseContextPermissionRule.groovy");
        assertThat(rows.get(3).get("RESOURCE_NAME")).isEqualTo("CasePermissionRule.groovy");
        assertThat(rows.get(4).get("RESOURCE_NAME")).isEqualTo("CaseVariablePermissionRule.groovy");
        assertThat(rows.get(5).get("RESOURCE_NAME")).isEqualTo("CommentPermissionRule.groovy");
        assertThat(rows.get(6).get("RESOURCE_NAME")).isEqualTo("ConnectorInstancePermissionRule.groovy");
        assertThat(rows.get(7).get("RESOURCE_NAME")).isEqualTo("DocumentPermissionRule.groovy");
        assertThat(rows.get(8).get("RESOURCE_NAME")).isEqualTo("ProcessConfigurationPermissionRule.groovy");
        assertThat(rows.get(9).get("RESOURCE_NAME")).isEqualTo("ProcessConnectorDependencyPermissionRule.groovy");
        assertThat(rows.get(10).get("RESOURCE_NAME")).isEqualTo("ProcessInstantiationPermissionRule.groovy");
        assertThat(rows.get(11).get("RESOURCE_NAME")).isEqualTo("ProcessPermissionRule.groovy");
        assertThat(rows.get(12).get("RESOURCE_NAME")).isEqualTo("ProcessResolutionProblemPermissionRule.groovy");
        assertThat(rows.get(13).get("RESOURCE_NAME")).isEqualTo("ProcessSupervisorPermissionRule.groovy");
        assertThat(rows.get(14).get("RESOURCE_NAME")).isEqualTo("ProfileEntryPermissionRule.groovy");
        assertThat(rows.get(15).get("RESOURCE_NAME")).isEqualTo("ProfilePermissionRule.groovy");
        assertThat(rows.get(16).get("RESOURCE_NAME")).isEqualTo("TaskExecutionPermissionRule.groovy");
        assertThat(rows.get(17).get("RESOURCE_NAME")).isEqualTo("TaskPermissionRule.groovy");
        assertThat(rows.get(18).get("RESOURCE_NAME")).isEqualTo("UserPermissionRule.groovy");
    }

    @Test
    public void should_extract_configuration() throws Exception {
        final File destFolder = temporaryFolder.newFolder("setup");
        //given
        platformSetup.init();

        //when
        System.setProperty(BONITA_SETUP_FOLDER, destFolder.getAbsolutePath());
        platformSetup.pull();

        //then
        File folderContainingResultOfGet = destFolder.toPath().resolve(PLATFORM_CONF_FOLDER_NAME).resolve("current").toFile();
        assertThat(folderContainingResultOfGet).as("should retrieve config files")
                .exists()
                .isDirectory();

        List<FullBonitaConfiguration> configurations = new ArrayList<>();
        AllConfigurationResourceVisitor allConfigurationResourceVisitor = new AllConfigurationResourceVisitor(configurations);
        Files.walkFileTree(destFolder.toPath(), allConfigurationResourceVisitor);

        assertThat(configurations).extracting("resourceName").containsOnly(
                "bonita-platform-community-custom.properties",
                "bonita-platform-custom.xml",
                "bonita-platform-init-community-custom.properties",
                "bonita-platform-init-custom.xml",
                "cache-config.xml",
                "jaas-standard.cfg",
                "platform-tenant-config.properties",
                "security-config.properties",
                "bonita-tenant-community-custom.properties",
                "bonita-tenants-custom.xml",
                "authenticationManager-config.properties",
                "compound-permissions-mapping.properties",
                "console-config.properties",
                "custom-permissions-mapping.properties",
                "dynamic-permissions-checks.properties",
                "forms-config.properties",
                "resources-permissions-mapping.properties",
                "ActorMemberPermissionRule.groovy",
                "ActorPermissionRule.groovy",
                "CaseContextPermissionRule.groovy",
                "CasePermissionRule.groovy",
                "CaseVariablePermissionRule.groovy",
                "CommentPermissionRule.groovy",
                "ConnectorInstancePermissionRule.groovy",
                "DocumentPermissionRule.groovy",
                "ProcessConfigurationPermissionRule.groovy",
                "ProcessConnectorDependencyPermissionRule.groovy",
                "ProcessInstantiationPermissionRule.groovy",
                "ProcessPermissionRule.groovy",
                "ProcessResolutionProblemPermissionRule.groovy",
                "ProcessSupervisorPermissionRule.groovy",
                "ProfileEntryPermissionRule.groovy",
                "ProfilePermissionRule.groovy",
                "TaskExecutionPermissionRule.groovy",
                "TaskPermissionRule.groovy",
                "UserPermissionRule.groovy");

    }

    @Test
    public void init_method_should_log_when_created() throws Exception {
        //given
        assertThat(platformSetup.isPlatformAlreadyCreated()).isFalse();

        //when
        systemOutRule.clearLog();
        platformSetup.init();

        //then
        assertThat(platformSetup.isPlatformAlreadyCreated()).isTrue();

        final String log = systemOutRule.getLogWithNormalizedLineSeparator();
        final String[] split = log.split("\n");
        assertThat(log).as("should setup log message").doesNotContain("Platform is already created. Nothing to do.");
        assertThat(split).as("should setup log message").isNotEmpty();
        assertThat(log).as("should create platform and log message").contains("Platform created.");
        assertThat(split[split.length - 1]).as("should push Initial configuration and log message")
                .contains("INFO")
                .endsWith("Initial configuration successfully pushed to database from folder platform_conf" + File.separator + "initial");
    }

    @Test
    public void init_method_should_do_nothing_when_already_created() throws Exception {
        //given
        platformSetup.init();

        //when
        systemOutRule.clearLog();
        platformSetup.init();

        //then
        assertThat(platformSetup.isPlatformAlreadyCreated()).isTrue();

        final String log = systemOutRule.getLogWithNormalizedLineSeparator();
        final String[] split = log.split("\n");
        assertThat(log).as("should setup log message").doesNotContain("Platform created.");
        assertThat(split).as("should setup log message").isNotEmpty();
        assertThat(split[split.length - 1]).as("should setup platform and log message").contains("INFO")
                .endsWith("Platform is already created. Nothing to do.");

    }

    @Test
    public void push_method_should_log_when_created() throws Exception {
        // given
        platformSetup.init();

        // when
        systemOutRule.clearLog();
        platformSetup.push();

        final String log = systemOutRule.getLogWithNormalizedLineSeparator();
        final String[] split = log.split("\n");

        // then
        assertThat(split[split.length - 1]).as("should push new configuration and log message").contains("INFO")
                .endsWith("New configuration successfully pushed to database. You can now restart Bonita BPM to reflect your changes.");
    }

    @Test
    public void push_should_throw_exception_when_platform_is_not_created() throws Exception {
        //given
        assertThat(platformSetup.isPlatformAlreadyCreated()).isFalse();

        //expect
        expectedException.expect(PlatformSetupException.class);
        expectedException.expectMessage("Platform is not created. run platform setup before pushing configuration.");

        //when
        platformSetup.push();
    }

    @Test
    public void clean_method_should_delete_and_log() throws Exception {
        //given
        final Path path = temporaryFolder.newFolder("afterClean").toPath();
        platformSetup.init();

        //when
        systemOutRule.clearLog();
        platformSetup.clean();

        //then
        final String log = systemOutRule.getLogWithNormalizedLineSeparator();
        final String[] split = log.split("\n");
        assertThat(split).as("should log message").isNotEmpty();
        assertThat(split[split.length - 1]).as("should log message").contains("INFO")
                .endsWith("Delete all configuration.");

        platformSetup.pull(path);
        List<FullBonitaConfiguration> configurations = new ArrayList<>();
        Files.walkFileTree(path, new AllConfigurationResourceVisitor(configurations));
        assertThat(configurations).as("should remove all files").isEmpty();
    }

    @Test
    public void push_method_should_clean_previous_config() throws Exception {
        //given
        List<FullBonitaConfiguration> configurations = new ArrayList<>();
        final Path initPath = temporaryFolder.newFolder("init").toPath();
        final Path pushPath = temporaryFolder.newFolder("push").toPath();
        final Path checkPath = temporaryFolder.newFolder("check").toPath();

        FileUtils.writeByteArrayToFile(
                initPath.resolve(PLATFORM_CONF_FOLDER_NAME).resolve("initial").resolve(PLATFORM_ENGINE.name().toLowerCase()).resolve("initial.properties")
                        .toFile(),
                "key1=value1".getBytes());

        FileUtils.writeByteArrayToFile(
                pushPath.resolve(PLATFORM_CONF_FOLDER_NAME).resolve("current").resolve(TENANT_TEMPLATE_PORTAL.name().toLowerCase())
                        .resolve("current.properties").toFile(),
                "key2=value2".getBytes());

        System.setProperty(BONITA_SETUP_FOLDER, initPath.toString());
        configurationFolderUtil.buildSqlFolder(initPath.toFile().toPath(), dbVendor);
        platformSetup.init();

        //when
        System.setProperty(BONITA_SETUP_FOLDER, pushPath.toString());
        platformSetup.push();

        //then
        platformSetup.pull(checkPath);
        Files.walkFileTree(checkPath, new AllConfigurationResourceVisitor(configurations));
        assertThat(configurations).as("should remove all files").hasSize(1)
                .extracting("resourceName").containsOnly("current.properties");
    }
}