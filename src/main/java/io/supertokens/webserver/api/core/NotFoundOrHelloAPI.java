/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
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

package io.supertokens.webserver.api.core;

import io.supertokens.Main;
import io.supertokens.output.Logging;
import io.supertokens.pluginInterface.Storage;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.multitenancy.AppIdentifierWithStorage;
import io.supertokens.pluginInterface.multitenancy.exceptions.TenantOrAppNotFoundException;
import io.supertokens.utils.RateLimiter;
import io.supertokens.webserver.WebserverAPI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class NotFoundOrHelloAPI extends WebserverAPI {

    private static final long serialVersionUID = 1L;

    public NotFoundOrHelloAPI(Main main) {
        super(main, "");
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        handleRequest(req, resp);
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException,
            ServletException {
        // getServletPath returns the path without the base path.
        AppIdentifierWithStorage appIdentifierWithStorage = null;

        try {
            appIdentifierWithStorage = getAppIdentifierWithStorage(req);
        } catch (TenantOrAppNotFoundException e) {
            // we send 500 status code
            throw new ServletException(e);
        }

        if (req.getServletPath().equals("/")) {
            // API is app specific
            try {
                RateLimiter rateLimiter = RateLimiter.getInstance(getAppIdentifierWithStorage(req), super.main, 200);
                if (!rateLimiter.checkRequest()) {
                    if (Main.isTesting) {
                        super.sendTextResponse(200, "RateLimitedHello", resp);
                    } else {
                        super.sendTextResponse(200, "Hello", resp);
                    }
                    return;
                }

                for (Storage storage : appIdentifierWithStorage.getStorages()) {
                    // even if the public tenant does not exist, the following function will return a null
                    // idea here is to test that the storage is working
                    storage.getKeyValue(appIdentifierWithStorage.getAsPublicTenantIdentifier(), "Test");
                }
                super.sendTextResponse(200, "Hello", resp);

            } catch (StorageQueryException | TenantOrAppNotFoundException e) {
                // we send 500 status code
                throw new ServletException(e);
            }
        } else {
            super.sendTextResponse(404, "Not found", resp);

            Logging.error(main, appIdentifierWithStorage.getAsPublicTenantIdentifier(), "Unknown API called: " + req.getRequestURL(), false);
        }
    }

}
