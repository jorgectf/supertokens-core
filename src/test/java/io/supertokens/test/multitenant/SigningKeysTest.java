/*
 *    Copyright (c) 2023, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test.multitenant;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.supertokens.ProcessState;
import io.supertokens.config.Config;
import io.supertokens.featureflag.EE_FEATURES;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.pluginInterface.exceptions.DbInitException;
import io.supertokens.pluginInterface.exceptions.InvalidConfigException;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.multitenancy.EmailPasswordConfig;
import io.supertokens.pluginInterface.multitenancy.PasswordlessConfig;
import io.supertokens.pluginInterface.multitenancy.TenantConfig;
import io.supertokens.pluginInterface.multitenancy.ThirdPartyConfig;
import io.supertokens.session.accessToken.AccessTokenSigningKey;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.test.TestingProcessManager;
import io.supertokens.test.Utils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.IOException;

import static org.junit.Assert.*;

public class SigningKeysTest {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void normalConfigContinuesToWork()
            throws InterruptedException, IOException, StorageQueryException, StorageTransactionLogicException {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.LOADING_ALL_TENANT_CONFIG));

        assertEquals(AccessTokenSigningKey.getInstance(null, null, process.main).getAllKeys().size(), 1);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void keysAreGeneratedForAllUserPoolIds()
            throws InterruptedException, IOException, StorageQueryException, StorageTransactionLogicException,
            InvalidConfigException, DbInitException {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        JsonObject tenantConfig = new JsonObject();
        StorageLayer.getStorage(null, null, process.getProcess())
                .modifyConfigToAddANewUserPoolForTesting(tenantConfig, 2);
        tenantConfig.add("access_token_signing_key_update_interval", new JsonPrimitive(200));

        TenantConfig[] tenants = new TenantConfig[]{
                new TenantConfig("c1", null, new EmailPasswordConfig(false),
                        new ThirdPartyConfig(false, new ThirdPartyConfig.Provider[0]),
                        new PasswordlessConfig(false),
                        tenantConfig)};

        Config.loadAllTenantConfig(process.getProcess(), tenants);

        StorageLayer.loadAllTenantStorage(process.getProcess(), tenants);

        AccessTokenSigningKey.loadForAllTenants(process.getProcess(), tenants);

        assertEquals(AccessTokenSigningKey.getInstance(null, null, process.main).getAllKeys().size(), 1);
        assertEquals(AccessTokenSigningKey.getInstance("c1", null, process.main).getAllKeys().size(), 1);
        AccessTokenSigningKey.KeyInfo baseTenant = AccessTokenSigningKey.getInstance(null, null, process.main)
                .getAllKeys().get(0);
        AccessTokenSigningKey.KeyInfo c1Tenant = AccessTokenSigningKey.getInstance("c1", null, process.main)
                .getAllKeys().get(0);

        assertNotEquals(baseTenant.createdAtTime, c1Tenant.createdAtTime);
        assertNotEquals(baseTenant.expiryTime, c1Tenant.expiryTime);
        assertTrue(baseTenant.expiryTime + (31 * 3600 * 1000) < c1Tenant.expiryTime);
        assertNotEquals(baseTenant.value, c1Tenant.value);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void signingKeyClassesAreThereForAllTenants() throws InterruptedException, IOException {

    }
}
