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
import java.util.stream.Stream;
import local.mylan.service.api.exceptions.DataCollisionException;
import local.mylan.service.api.exceptions.NoDataException;
import local.mylan.service.api.exceptions.UnauthenticatedException;
import local.mylan.service.api.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExceptionHandlerUtilsTest {

    @ParameterizedTest
    @MethodSource
    void toResponseStatus(final HttpResponseStatus status, final Throwable throwable){
        Assertions.assertEquals(status, ExceptionHandlerUtils.defaultExceptionHandler().statusFromException(throwable));
    }

    private static Stream<Arguments> toResponseStatus(){
        final String msg = "error message";
        return Stream.of(
          Arguments.of(HttpResponseStatus.UNAUTHORIZED, new UnauthenticatedException(msg)),
          Arguments.of(HttpResponseStatus.FORBIDDEN, new UnauthorizedException(msg)),
          Arguments.of(HttpResponseStatus.NOT_FOUND, new NoDataException(msg)),
          Arguments.of(HttpResponseStatus.CONFLICT, new DataCollisionException(msg)),
          Arguments.of(HttpResponseStatus.BAD_REQUEST, new IllegalArgumentException(msg)),
          Arguments.of(HttpResponseStatus.INTERNAL_SERVER_ERROR, new IllegalStateException(msg))
        );

    }

}
