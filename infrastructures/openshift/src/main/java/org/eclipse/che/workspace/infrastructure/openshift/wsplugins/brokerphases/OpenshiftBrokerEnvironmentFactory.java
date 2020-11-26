/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.openshift.wsplugins.brokerphases;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.provision.env.AgentAuthEnableEnvVarProvider;
import org.eclipse.che.api.workspace.server.spi.provision.env.MachineTokenEnvVarProvider;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginFQN;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.CertificateProvisioner;
import org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins.brokerphases.BrokerEnvironmentFactory;
import org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins.brokerphases.KubernetesBrokerEnvironmentFactory;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;
import org.eclipse.che.workspace.infrastructure.openshift.provision.OpenshiftTrustedCAProvisioner;

/**
 * Extends {@link KubernetesBrokerEnvironmentFactory} to be used in the openshift infrastructure.
 *
 * @author Oleksandr Garagatyi
 */
public class OpenshiftBrokerEnvironmentFactory
    extends BrokerEnvironmentFactory<OpenShiftEnvironment> {

  private final OpenshiftTrustedCAProvisioner trustedCAProvisioner;

  @Inject
  public OpenshiftBrokerEnvironmentFactory(
      @Named("che.websocket.endpoint") String cheWebsocketEndpoint,
      @Named("che.workspace.plugin_broker.pull_policy") String brokerPullPolicy,
      AgentAuthEnableEnvVarProvider authEnableEnvVarProvider,
      MachineTokenEnvVarProvider machineTokenEnvVarProvider,
      @Named("che.workspace.plugin_broker.artifacts.image") String artifactsBrokerImage,
      @Named("che.workspace.plugin_broker.metadata.image") String metadataBrokerImage,
      @Nullable @Named("che.workspace.plugin_registry_url") String pluginRegistryUrl,
      @Named("che.infra.openshift.trusted_ca_bundles_mount_path") String caCertificatesMountPath,
      CertificateProvisioner certProvisioner,
      OpenshiftTrustedCAProvisioner trustedCAProvisioner) {
    super(
        cheWebsocketEndpoint,
        brokerPullPolicy,
        authEnableEnvVarProvider,
        machineTokenEnvVarProvider,
        artifactsBrokerImage,
        metadataBrokerImage,
        pluginRegistryUrl,
        trustedCAProvisioner,
        caCertificatesMountPath,
        certProvisioner);
    this.trustedCAProvisioner = trustedCAProvisioner;
  }

  @Override
  protected OpenShiftEnvironment doCreate(BrokersConfigs brokersConfigs) {
    return OpenShiftEnvironment.builder()
        .setConfigMaps(brokersConfigs.configMaps)
        .setMachines(brokersConfigs.machines)
        .setPods(brokersConfigs.pods)
        .build();
  }

  @Override
  public OpenShiftEnvironment createForMetadataBroker(
      Collection<PluginFQN> pluginFQNs, RuntimeIdentity runtimeID, boolean mergePlugins)
      throws InfrastructureException {
    OpenShiftEnvironment openShiftEnvironment =
        super.createForMetadataBroker(pluginFQNs, runtimeID, mergePlugins);
    trustedCAProvisioner.provision(openShiftEnvironment, runtimeID);
    return openShiftEnvironment;
  }
}
