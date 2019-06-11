/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.scanner.impl;

import java.util.Map;
import java.util.Objects;

import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.MatchingRequirement;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

    public MatchingRequirementImpl(final Requirement req) {
        super(req.getResource(), req.getNamespace(), req.getDirectives(), req.getAttributes());
    }

    public MatchingRequirementImpl(final Resource res, final String ns, final Map<String, String> dirs,
            final Map<String, Object> attrs) {
        super(res, ns, dirs, attrs);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof RequirementImpl)) {
            return false;
        }
        final RequirementImpl that = (RequirementImpl) o;
        return Objects.equals(resource, that.getResource()) && Objects.equals(namespace, that.getNamespace())
                && Objects.equals(attributes, that.getAttributes()) && Objects.equals(directives, that.getDirectives());
    }
}
