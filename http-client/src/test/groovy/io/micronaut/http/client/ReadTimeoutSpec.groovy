/*
 * Copyright 2017-2019 original authors
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
package io.micronaut.http.client

import io.micronaut.context.ApplicationContext
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.ReadTimeoutException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.http.annotation.Get
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ReadTimeoutSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(
            "micronaut.http.client.readTimeout":'1s'
    )

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    @Shared
    @AutoCleanup
    HttpClient client = context.createBean(HttpClient, embeddedServer.getURL())


    void "test read timeout setting"() {
        when:
        client.toBlocking().retrieve(HttpRequest.GET('/timeout'), String)

        then:
        def e = thrown(ReadTimeoutException)
        e.message == 'Read Timeout'
    }

    void "test read timeout setting with connection pool"() {
        given:
        ApplicationContext clientContext = ApplicationContext.run(
                'micronaut.http.client.read-timeout':'1s',
                'micronaut.http.client.pool.enabled':true
        )
        RxHttpClient client = clientContext.createBean(RxHttpClient, embeddedServer.getURL())
        when:
        client.retrieve(HttpRequest.GET('/timeout/client'), String).blockingFirst()

        then:
        def e = thrown(ReadTimeoutException)
        e.message == 'Read Timeout'

        cleanup:
        clientContext.close()
    }

    @Controller("/timeout")
    static class GetController {

        @Inject
        TestClient testClient

        @Get(value = "/", produces = MediaType.TEXT_PLAIN)
        String index() {
            sleep 5000
            return "success"
        }

        @Get(value = "/client", produces = MediaType.TEXT_PLAIN)
        String test() {
            return testClient.get()
        }
    }

    @Client("/timeout")
    static interface TestClient {
        @Get
        String get()
    }
}
