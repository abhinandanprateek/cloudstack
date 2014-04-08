// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.iam;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.QuerySelector;
import org.apache.cloudstack.iam.api.IAMGroup;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;

public class RoleBasedEntityQuerySelector extends AdapterBase implements QuerySelector {

    private static final Logger s_logger = Logger.getLogger(RoleBasedEntityQuerySelector.class.getName());

    @Inject
    IAMService _iamService;

    @Override
    public List<Long> getAuthorizedDomains(Account caller, String action) {
        long accountId = caller.getAccountId();
        // Get the static Policies of the Caller
        List<IAMPolicy> policies = _iamService.listIAMPolicies(accountId);
        // for each policy, find granted permission with Domain scope
        List<Long> domainIds = new ArrayList<Long>();
        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> pp = _iamService.listPolicyPermissionsByScope(policy.getId(), action, PermissionScope.DOMAIN.toString());
            if (pp != null) {
                for (IAMPolicyPermission p : pp) {
                    if (p.getScopeId() != null) {
                        if (p.getScopeId().longValue() == -1) {
                            domainIds.add(caller.getDomainId());
                        } else {
                            domainIds.add(p.getScopeId());
                        }
                    }
                }
            }
        }
        return domainIds;
    }

    @Override
    public List<Long> getAuthorizedAccounts(Account caller, String action) {
        long accountId = caller.getAccountId();
        // Get the static Policies of the Caller
        List<IAMPolicy> policies = _iamService.listIAMPolicies(accountId);
        // for each policy, find granted permission with Account scope
        List<Long> accountIds = new ArrayList<Long>();
        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> pp = _iamService.listPolicyPermissionsByScope(policy.getId(), action, PermissionScope.ACCOUNT.toString());
            if (pp != null) {
                for (IAMPolicyPermission p : pp) {
                    if (p.getScopeId() != null) {
                        if (p.getScopeId().longValue() == -1) {
                            accountIds.add(caller.getId());
                        } else {
                            accountIds.add(p.getScopeId());
                        }
                    }
                }
            }
        }
        return accountIds;
    }

    @Override
    public List<Long> getAuthorizedResources(Account caller, String action) {
        long accountId = caller.getAccountId();
        // Get the static Policies of the Caller
        List<IAMPolicy> policies = _iamService.listIAMPolicies(accountId);

        // add the policies that grant recursive access
        List<IAMGroup> groups = _iamService.listIAMGroups(caller.getId());
        for (IAMGroup group : groups) {
            // for each group find the grand parent groups.
            List<IAMGroup> parentGroups = _iamService.listParentIAMGroups(group.getId());
            for (IAMGroup parentGroup : parentGroups) {
                policies.addAll(_iamService.listRecursiveIAMPoliciesByGroup(parentGroup.getId()));
            }
        }

        // for each policy, find granted permission with Resource scope
        List<Long> entityIds = new ArrayList<Long>();
        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> pp = _iamService.listPolicyPermissionsByScope(policy.getId(), action, PermissionScope.RESOURCE.toString());
            if (pp != null) {
                for (IAMPolicyPermission p : pp) {
                    if (p.getScopeId() != null) {
                        entityIds.add(p.getScopeId());
                    }
                }
            }
        }
        return entityIds;
    }

    @Override
    public boolean isGrantedAll(Account caller, String action) {
        long accountId = caller.getAccountId();
        // Get the static Policies of the Caller
        List<IAMPolicy> policies = _iamService.listIAMPolicies(accountId);
        // for each policy, find granted permission with ALL scope
        for (IAMPolicy policy : policies) {
            List<IAMPolicyPermission> pp = _iamService.listPolicyPermissionsByScope(policy.getId(), action, PermissionScope.ALL.toString());
            if (pp != null && pp.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> listAclGroupsByAccount(long accountId) {
        List<IAMGroup> groups = _iamService.listIAMGroups(accountId);
        List<String> groupNames = new ArrayList<String>();
        for (IAMGroup grp : groups) {
            groupNames.add(grp.getName());
        }
        return groupNames;
    }

}
