package com.vmware.action.base;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.domain.Sites;

public abstract class BaseSshAction extends BaseVappAction {

    public BaseSshAction(WorkflowConfig config) {
        super(config);
        JSch.setLogger(new SshLogger(log));
    }

    protected void waitForChannelToFinish(Channel channel) {
        while (channel.isConnected()) {
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }

    protected SiteConfig createSshSiteConfig() {
        if (vappData.getSelectedSite() != null && vcdConfig.useDatabaseHost) {
            Sites.Site site = vappData.getSelectedSite();
            Sites.DatabaseServer databaseConfig = site.databaseServer;
            log.info("Using database host {} for ssh site config", databaseConfig.host);
            return new SiteConfig(databaseConfig.host, 22, "root", databaseConfig.deployment.guestProperties.adminPassword);
        } else if (vappData.getSelectedVcdCell() != null) {
            Sites.DeployedVM cell = vappData.getSelectedVcdCell();
            if (cell.deployment == null) {
                throw new FatalException("No deployment section found for cell " + vappData.getSelectedVcdCell().name);
            }
            Sites.OvfProperties ovfProperties = cell.deployment.ovfProperties;
            return new SiteConfig(ovfProperties.hostname, 22, cell.osCredentials.username, cell.osCredentials.password);
        } else if (sshConfig.hasCommandLineSite()) {
            return sshConfig.commandLineSite();
        } else {
            String sshSite = sshConfig.sshSite;
            TreeMap<String, SiteConfig> sshSiteConfigs = sshConfig.sshSiteConfigs;
            if (sshSiteConfigs == null || sshSiteConfigs.isEmpty()) {
                throw new FatalException("No ssh sites configured");
            }
            if (StringUtils.isEmpty(sshSite)) {
                sshSite = InputUtils.readValueUntilNotBlank("Ssh site", sshSiteConfigs.keySet());
            }
            if (!sshSiteConfigs.containsKey(sshSite)) {
                throw new FatalException("Ssh site {} is not present in list {}", sshSite, sshSiteConfigs.keySet().toString());
            }
            return sshSiteConfigs.get(sshSite);
        }
    }

    private static class SshLogger implements Logger {

        private DynamicLogger logger;

        SshLogger(org.slf4j.Logger logger) {
            this.logger = new DynamicLogger(logger);
        }

        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            logger.log(LogLevel.fromJschLevel(level), message);
        }
    }
}
