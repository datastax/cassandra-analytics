/*
 * Copyright IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.spark.sparksql;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import org.apache.cassandra.spark.data.CassandraDataSourceHelper;
import org.apache.cassandra.spark.data.ClientConfig;
import org.apache.cassandra.spark.data.DataLayer;
import org.apache.cassandra.spark.utils.MapUtils;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

public class HcdDataSource extends CassandraDataSource
{
    public static final String SIDECAR_CONTACT_POINTS_DISCOVERY = "sidecar_contact_points_discovery";

    @Override
    public DataLayer getDataLayer(CaseInsensitiveStringMap options)
    {
        Map<String, String> parameters = new HashMap<>(options);
        resolveSidecarContactPoints(parameters);
        CaseInsensitiveStringMap immutableOptions = new CaseInsensitiveStringMap(parameters);
        return CassandraDataSourceHelper.getDataLayer(immutableOptions, this::initializeDataLayer);
    }

    public static void resolveSidecarContactPoints(Map<String, String> options)
    {
        options.computeIfAbsent(ClientConfig.SIDECAR_CONTACT_POINTS, (key) -> {
            String discoveryEndpoint = MapUtils.getOrDefault(options, SIDECAR_CONTACT_POINTS_DISCOVERY, null);
            Preconditions.checkNotNull(discoveryEndpoint,
                                       "One of parameters '" + ClientConfig.SIDECAR_CONTACT_POINTS
                                       + "' or '" + SIDECAR_CONTACT_POINTS_DISCOVERY + "' is required");
            return lookupSidecarContactPoints(discoveryEndpoint);
        });
    }

    public static String lookupSidecarContactPoints(String discoveryEndpoint)
    {
        try
        {
            InetAddress[] aRecords = InetAddress.getAllByName(discoveryEndpoint);
            return Arrays.stream(aRecords)
                         .map(InetAddress::getHostAddress)
                         .collect(Collectors.joining(","));
        }
        catch (UnknownHostException e)
        {
            throw new IllegalArgumentException("Unable to lookup Sidecar contact points from endpoint: " + discoveryEndpoint, e);
        }
    }
}
