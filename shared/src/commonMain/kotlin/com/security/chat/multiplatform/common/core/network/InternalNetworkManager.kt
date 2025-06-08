package com.security.chat.multiplatform.common.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class InternalNetworkManager(
    @PublishedApi
    internal val httpClient: HttpClient,
    @PublishedApi
    internal val baseUrl: String,
) {

    public suspend inline fun <reified Response> runGet(
        relativePath: String,
        request: Map<String, String> = emptyMap(),
    ): Response {
        return httpClient.get(urlString = baseUrl + relativePath) {
            url {
                request.forEach { param ->
                    parameters.append(name = param.key, value = param.value)
                }
            }
        }
            .body()
    }

    public suspend inline fun <reified Params, reified Response> runPost(
        relativePath: String,
        request: Params,
    ): Response {
        return httpClient.post(urlString = baseUrl + relativePath)
        {
            setBody(request)
            contentType(ContentType.Application.Json)
        }
            .body()
    }

}