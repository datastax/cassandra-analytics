/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.testing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.DropwizardVertxMetricsFactory;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MatchType;

import org.apache.cassandra.sidecar.config.FileSystemOptionsConfiguration;
import org.apache.cassandra.sidecar.config.SidecarConfiguration;
import org.apache.cassandra.sidecar.config.VertxConfiguration;
import org.apache.cassandra.sidecar.config.VertxMetricsConfiguration;
import org.apache.cassandra.sidecar.metrics.MetricRegistryFactory;

import static org.apache.cassandra.sidecar.common.ApiEndpointsV1.API_V1_ALL_ROUTES;

/**
 * Overrides Sidecar's Vertx provider to support Vert.x 5 migration.
 */
public class AnalyticsVertxModule extends AbstractModule
{
    @Provides
    @Singleton
    Vertx vertx(SidecarConfiguration configuration,
                MetricRegistryFactory metricRegistryFactory)
    {
        VertxMetricsConfiguration metricsConfig =
        configuration.metricsConfiguration().vertxConfiguration();

        DropwizardMetricsOptions metricsOptions = new DropwizardMetricsOptions().setEnabled(metricsConfig.enabled())
                                                                                .setJmxEnabled(metricsConfig.exposeViaJMX())
                                                                                .setJmxDomain(metricsConfig.jmxDomainName())
                                                                                .addMonitoredHttpServerRoute(new Match().setValue(API_V1_ALL_ROUTES)
                                                                                                                        .setType(MatchType.REGEX));

        VertxOptions options = new VertxOptions()
                               .setMetricsOptions(metricsOptions);

        VertxConfiguration vertxConfig =
        configuration.vertxConfiguration();

        FileSystemOptionsConfiguration fsOptions = vertxConfig == null
                                                   ? null
                                                   : vertxConfig.filesystemOptionsConfiguration();

        if (fsOptions != null)
        {
            options.setFileSystemOptions(new FileSystemOptions()
                                         .setClassPathResolvingEnabled(fsOptions.classpathResolvingEnabled())
                                         .setFileCacheDir(fsOptions.fileCacheDir())
                                         .setFileCachingEnabled(fsOptions.fileCachingEnabled()));
        }

        return Vertx.builder()
                    .with(options)
                    .withMetrics(new DropwizardVertxMetricsFactory(metricRegistryFactory.getOrCreate()))
                    .build();
    }
}
