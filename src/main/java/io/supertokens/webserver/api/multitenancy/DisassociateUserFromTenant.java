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

package io.supertokens.webserver.api.multitenancy;

import com.google.gson.JsonObject;
import io.supertokens.AppIdentifierWithStorageAndUserIdMapping;
import io.supertokens.Main;
import io.supertokens.featureflag.exceptions.FeatureNotEnabledException;
import io.supertokens.multitenancy.Multitenancy;
import io.supertokens.pluginInterface.RECIPE_ID;
import io.supertokens.pluginInterface.emailpassword.exceptions.UnknownUserIdException;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.multitenancy.exceptions.TenantOrAppNotFoundException;
import io.supertokens.useridmapping.UserIdType;
import io.supertokens.webserver.InputParser;
import io.supertokens.webserver.WebserverAPI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class DisassociateUserFromTenant extends WebserverAPI {
    private static final long serialVersionUID = -4641988458637882374L;

    public DisassociateUserFromTenant(Main main) {
        super(main, RECIPE_ID.MULTITENANCY.toString());
    }

    @Override
    public String getPath() {
        return "/recipe/multitenancy/tenant/user/remove";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        JsonObject input = InputParser.parseJsonObjectOrThrowError(req);
        String userId = InputParser.parseStringOrThrowError(input, "userId", false);
        // normalize userId
        userId = userId.trim();

        if (userId.length() == 0) {
            throw new ServletException(
                    new WebserverAPI.BadRequestException("Field name 'userId' cannot be an empty String"));
        }

        try {
            AppIdentifierWithStorageAndUserIdMapping mappingAndStorage =
                    getAppIdentifierWithStorageAndUserIdMappingFromRequest(req, userId, UserIdType.ANY);
            if (mappingAndStorage.userIdMapping != null) {
                userId = mappingAndStorage.userIdMapping.superTokensUserId;
            }

            boolean wasAssociated = Multitenancy.removeUserIdFromTenant(main,
                    getTenantIdentifierWithStorageFromRequest(req), userId);

            JsonObject result = new JsonObject();
            result.addProperty("status", "OK");
            result.addProperty("wasAssociated", wasAssociated);
            super.sendJsonResponse(200, result, resp);

        } catch (UnknownUserIdException e) {
            JsonObject result = new JsonObject();
            result.addProperty("status", "UNKNOWN_USER_ID_ERROR");
            super.sendJsonResponse(200, result, resp);

        } catch (StorageQueryException | TenantOrAppNotFoundException | FeatureNotEnabledException e) {
            throw new ServletException(e);
        }

    }
}
