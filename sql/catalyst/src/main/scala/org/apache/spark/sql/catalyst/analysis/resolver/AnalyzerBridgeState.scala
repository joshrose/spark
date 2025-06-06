/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.analysis.resolver

import java.util.HashMap

import org.apache.spark.sql.catalyst.analysis.{AnalysisContext, UnresolvedRelation}
import org.apache.spark.sql.catalyst.catalog.UnresolvedCatalogRelation
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

/**
 * The [[AnalyzerBridgeState]] is a state passed from legacy [[Analyzer]] to the single-pass
 * [[Resolver]]. It is used  in dual-run mode (when
 * [[ANALYZER_SINGLE_PASS_RESOLVER_RELATION_BRIDGING_ENABLED]] is true).
 *
 * @param relationsWithResolvedMetadata A map from [[BridgedRelationId]] to the relations with
 *   resolved metadata. It allows us to reuse the relation metadata and avoid duplicate
 *   catalog/table lookups.
 * @param catalogRelationsWithResolvedMetadata A map from [[UnresolvedCatalogRelation]] to the
 *   relations with resolved metadata. It allows us to reuse the relation metadata and avoid
 *   duplicate catalog/table lookups.
 * @param hiveRelationsWithResolvedMetadata A map from [[HiveTableRelation]] to their resolved
 *   [[LogicalRelation]] counterparts. We cannot import those nodes here because of recursive
 *   dependencies, so we rely on overridden [[LogicalPlan.equals]] and [[LogicalPlan.hashCode]].
 *   Keys are canonicalized to compensate for stats added by [[DetermineTableStats]].
 */
case class AnalyzerBridgeState(
    relationsWithResolvedMetadata: AnalyzerBridgeState.RelationsWithResolvedMetadata =
      new AnalyzerBridgeState.RelationsWithResolvedMetadata,
    catalogRelationsWithResolvedMetadata: AnalyzerBridgeState.CatalogRelationsWithResolvedMetadata =
      new AnalyzerBridgeState.CatalogRelationsWithResolvedMetadata,
    hiveRelationsWithResolvedMetadata: AnalyzerBridgeState.HiveRelationsWithResolvedMetadata =
      new AnalyzerBridgeState.HiveRelationsWithResolvedMetadata
) {
  def addUnresolvedRelation(unresolvedRelation: UnresolvedRelation, relation: LogicalPlan): Unit = {
    relationsWithResolvedMetadata.put(
      BridgedRelationId(unresolvedRelation, AnalysisContext.get.catalogAndNamespace),
      relation
    )
  }

  def addLogicalRelationForHiveRelation(
      hiveRelation: LogicalPlan,
      logicalRelation: LogicalPlan): Unit = {
    hiveRelationsWithResolvedMetadata.put(hiveRelation.canonicalized, logicalRelation)
  }

  def getLogicalRelationForHiveRelation(hiveRelation: LogicalPlan): Option[LogicalPlan] = {
    Option(hiveRelationsWithResolvedMetadata.get(hiveRelation.canonicalized))
  }
}

object AnalyzerBridgeState {
  type RelationsWithResolvedMetadata = HashMap[BridgedRelationId, LogicalPlan]
  type CatalogRelationsWithResolvedMetadata = HashMap[UnresolvedCatalogRelation, LogicalPlan]
  type HiveRelationsWithResolvedMetadata = HashMap[LogicalPlan, LogicalPlan]
}
