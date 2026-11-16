/*
 * Copyright 2026 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.service.rest.utils;

import io.netty.handler.codec.http.HttpResponseStatus;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;

public final class ExceptionHandlerUtils {

    private ExceptionHandlerUtils() {
        // utility class
    }

    public static HttpResponseStatus toResponseStatus(final Throwable throwable) {
        return switch (throwable){
            case UnauthenticatedException e -> HttpResponseStatus.UNAUTHORIZED;
            case UnauthorizedException e -> HttpResponseStatus.FORBIDDEN;
            case NoDataException e -> HttpResponseStatus.NOT_FOUND;
            case DataCollisionException e -> HttpResponseStatus.CONFLICT;
            case IllegalArgumentException e -> HttpResponseStatus.BAD_REQUEST;
            default -> HttpResponseStatus.INTERNAL_SERVER_ERROR;
        };
    }

}
