/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.metadata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.database.DatabaseConfiguration;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeEngine;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.resource.CachedDatabaseMetaData;
import org.apache.shardingsphere.infra.metadata.resource.DataSourcesMetaData;
import org.apache.shardingsphere.infra.metadata.resource.ShardingSphereResource;
import org.apache.shardingsphere.infra.metadata.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * ShardingSphere meta data.
 */
@RequiredArgsConstructor
@Getter
public final class ShardingSphereMetaData {
    
    private final DatabaseType frontendDatabaseType;
    
    private final ShardingSphereResource resource;
    
    private final ShardingSphereRuleMetaData ruleMetaData;
    
    private final ShardingSphereDatabase database;
    
    /**
     * Create ShardingSphere meta data.
     * 
     * @param frontendDatabaseType frontend database type
     * @param database database
     * @param databaseConfig database configuration
     * @param rules rules
     * @return ShardingSphere meta data
     * @throws SQLException SQL exception
     */
    public static ShardingSphereMetaData create(final DatabaseType frontendDatabaseType, final ShardingSphereDatabase database,
                                                final DatabaseConfiguration databaseConfig, final Collection<ShardingSphereRule> rules) throws SQLException {
        ShardingSphereResource resource = createResource(frontendDatabaseType, databaseConfig.getDataSources());
        ShardingSphereRuleMetaData ruleMetaData = new ShardingSphereRuleMetaData(databaseConfig.getRuleConfigurations(), rules);
        return new ShardingSphereMetaData(frontendDatabaseType, resource, ruleMetaData, database);
    }
    
    private static ShardingSphereResource createResource(final DatabaseType frontendDatabaseType, final Map<String, DataSource> dataSourceMap) throws SQLException {
        DatabaseType databaseType = dataSourceMap.isEmpty() ? frontendDatabaseType : DatabaseTypeEngine.getDatabaseType(dataSourceMap.values());
        DataSourcesMetaData dataSourcesMetaData = new DataSourcesMetaData(databaseType, dataSourceMap);
        CachedDatabaseMetaData cachedDatabaseMetaData = createCachedDatabaseMetaData(dataSourceMap).orElse(null);
        return new ShardingSphereResource(dataSourceMap, dataSourcesMetaData, cachedDatabaseMetaData, databaseType);
    }
    
    private static Optional<CachedDatabaseMetaData> createCachedDatabaseMetaData(final Map<String, DataSource> dataSources) throws SQLException {
        if (dataSources.isEmpty()) {
            return Optional.empty();
        }
        try (Connection connection = dataSources.values().iterator().next().getConnection()) {
            return Optional.of(new CachedDatabaseMetaData(connection.getMetaData()));
        }
    }
    
    /**
     * Judge whether is completed.
     *
     * @return is completed or not
     */
    public boolean isComplete() {
        return !ruleMetaData.getRules().isEmpty() && !resource.getDataSources().isEmpty();
    }
    
    /**
     * Determine whether there is a data source.
     *
     * @return has datasource or not
     */
    public boolean hasDataSource() {
        return !resource.getDataSources().isEmpty();
    }
    
    /**
     * Get schema by name.
     * 
     * @param schemaName schema name
     * @return ShardingSphereSchema schema
     */
    public ShardingSphereSchema getSchemaByName(final String schemaName) {
        return database.getSchemas().get(schemaName);
    }
}
