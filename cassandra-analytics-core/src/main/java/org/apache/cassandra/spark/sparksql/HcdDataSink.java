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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap$;
import org.apache.spark.sql.sources.BaseRelation;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.immutable.Seq;
import scala.collection.immutable.Map$;

public class HcdDataSink extends CassandraDataSink
{
    @Override
    @NotNull
    public BaseRelation createRelation(@NotNull SQLContext sqlContext,
                                       @NotNull SaveMode saveMode,
                                       @NotNull scala.collection.immutable.Map<String, String> parameters,
                                       @NotNull Dataset<Row> data)
    {
        Map<String, String> options = new HashMap<>(JavaConverters.mapAsJavaMap(parameters));
        HcdDataSource.resolveSidecarContactPoints(options);
        scala.collection.immutable.Map<String, String> immutableOptions = toScalaImmutableMap(options);
        return super.createRelation(sqlContext, saveMode, CaseInsensitiveMap$.MODULE$.apply(immutableOptions), data);
    }

    private static <K, V> scala.collection.immutable.Map<K, V> toScalaImmutableMap(Map<K, V> map)
    {
        List<Tuple2<K, V>> tuples = map.entrySet()
                                       .stream()
                                       .map(e -> Tuple2.apply(e.getKey(), e.getValue()))
                                       .collect(Collectors.toList());
        Seq<Tuple2<K, V>> scalaSeq = JavaConverters.asScalaBuffer(tuples).toSeq().toList();
        return (scala.collection.immutable.Map<K, V>) Map$.MODULE$.apply(scalaSeq);
    }
}
