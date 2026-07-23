package com.security.chat.multiplatform.common.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

public class NetworkManager(
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
        return httpClient.post(urlString = baseUrl + relativePath) {
            setBody(request)
            contentType(ContentType.Application.Json)
        }
            .body()
    }

    public suspend fun runPostFile(
        relativePath: String,
        filePath: String,
    ) {
        val path = Path(filePath)
        val fileSize = SystemFileSystem.metadataOrNull(path)?.size

        httpClient.post(urlString = baseUrl + relativePath) {
            setBody(
                object : OutgoingContent.WriteChannelContent() {
                    override val contentType: ContentType = ContentType.Application.OctetStream
                    override val contentLength: Long? = fileSize

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        SystemFileSystem.source(path).buffered().use { source ->
                            channel.writeSource(source)
                        }
                    }
                },
            )
        }
    }

    public suspend fun runGetFile(
        relativePath: String,
        destinationPath: String,
    ) {
        httpClient.prepareGet(urlString = baseUrl + relativePath).execute { response ->
            val channel = response.bodyAsChannel()
            SystemFileSystem.sink(Path(destinationPath)).buffered().use { sink ->
                while (!channel.isClosedForRead) {
                    channel.readRemaining(DOWNLOAD_CHUNK_SIZE).transferTo(sink)
                }
            }
        }
    }

    public suspend inline fun runDelete(
        relativePath: String,
    ) {
        httpClient.delete(urlString = baseUrl + relativePath) {
            contentType(ContentType.Application.Json)
        }
    }
}

private const val DOWNLOAD_CHUNK_SIZE = 8L * 1024L