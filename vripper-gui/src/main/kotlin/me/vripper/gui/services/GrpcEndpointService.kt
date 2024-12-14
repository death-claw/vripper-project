package me.vripper.gui.services

import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.vripper.entities.MetadataEntity
import me.vripper.entities.Status
import me.vripper.model.*
import me.vripper.model.DownloadSpeed
import me.vripper.model.ErrorCount
import me.vripper.model.PostSelection
import me.vripper.model.QueueState
import me.vripper.model.ThreadPostId
import me.vripper.proto.*
import me.vripper.proto.EndpointServiceOuterClass.*
import me.vripper.proto.LogOuterClass.Log
import me.vripper.services.IAppEndpointService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GrpcEndpointService : IAppEndpointService {

    private var channel: ManagedChannel? = null
    private var endpointServiceCoroutineStub: EndpointServiceGrpcKt.EndpointServiceCoroutineStub? = null

    override suspend fun scanLinks(postLinks: String) {
        endpointServiceCoroutineStub!!.scanLinks(Links.newBuilder().setLinks(postLinks).build())
    }

    override suspend fun restartAll(posIds: List<Long>) {
        endpointServiceCoroutineStub!!.restartAll(IdList.newBuilder().addAllIds(posIds).build())
    }

    override suspend fun remove(postIdList: List<Long>) {
        endpointServiceCoroutineStub!!.remove(IdList.newBuilder().addAllIds(postIdList).build())
    }

    override suspend fun stopAll(postIdList: List<Long>) {
        endpointServiceCoroutineStub!!.stopAll(IdList.newBuilder().addAllIds(postIdList).build())
    }

    override suspend fun clearCompleted(): List<Long> {
        return endpointServiceCoroutineStub!!.clearCompleted(EmptyRequest.getDefaultInstance()).idsList
    }

    override suspend fun findPost(postId: Long): Post =
        mapper(endpointServiceCoroutineStub!!.findPost(Id.newBuilder().setId(postId).build()))


    override suspend fun findAllPosts(): List<Post> =
        endpointServiceCoroutineStub!!.findAllPosts(EmptyRequest.getDefaultInstance()).postsList.map(::mapper)


    override suspend fun rename(postId: Long, newName: String) {
        endpointServiceCoroutineStub!!.rename(
            Rename.newBuilder().setPostId(postId).setName(newName).build()
        )
    }

    override suspend fun renameToFirst(postIds: List<Long>) {
        endpointServiceCoroutineStub!!.renameToFirst(RenameToFirst.newBuilder().addAllPostIds(postIds).build())
    }

    override fun onNewPosts(): Flow<Post> =
        endpointServiceCoroutineStub!!.onNewPosts(EmptyRequest.getDefaultInstance()).map { mapper(it) }


    override fun onUpdatePosts(): Flow<Post> =
        endpointServiceCoroutineStub!!.onUpdatePosts(EmptyRequest.getDefaultInstance()).map {
            mapper(it)
        }


    override fun onDeletePosts() =
        endpointServiceCoroutineStub!!.onDeletePosts(EmptyRequest.getDefaultInstance()).map {
            it.id
        }


    override fun onUpdateMetadata(): Flow<Metadata> =
        endpointServiceCoroutineStub!!.onUpdateMetadata(EmptyRequest.getDefaultInstance()).map { mapper(it) }


    override suspend fun findImagesByPostId(postId: Long): List<Image> =
        endpointServiceCoroutineStub!!.findImagesByPostId(
            Id.newBuilder().setId(postId).build()
        ).imagesList.map(::mapper)


    override fun onUpdateImagesByPostId(postId: Long): Flow<Image> =
        endpointServiceCoroutineStub!!.onUpdateImagesByPostId(Id.newBuilder().setId(postId).build()).map(::mapper)

    override fun onUpdateImages(): Flow<Image> =
        endpointServiceCoroutineStub!!.onUpdateImages(EmptyRequest.getDefaultInstance()).map(::mapper)


    override fun onStopped(): Flow<Long> =
        endpointServiceCoroutineStub!!.onStopped(EmptyRequest.getDefaultInstance()).map { it.id }


    override fun onNewLog(): Flow<LogEntry> =
        endpointServiceCoroutineStub!!.onNewLog(EmptyRequest.getDefaultInstance()).map { mapper(it) }

    override suspend fun initLogger() {
        endpointServiceCoroutineStub!!.initLogger(EmptyRequest.getDefaultInstance())
    }

    override fun onNewThread() =
        endpointServiceCoroutineStub!!.onNewThread(EmptyRequest.getDefaultInstance()).map { mapper(it) }

    override fun onUpdateThread() =
        endpointServiceCoroutineStub!!.onUpdateThread(EmptyRequest.getDefaultInstance()).map { mapper(it) }

    override fun onDeleteThread() =
        endpointServiceCoroutineStub!!.onDeleteThread(EmptyRequest.getDefaultInstance()).map { it.id }

    override fun onClearThreads() =
        endpointServiceCoroutineStub!!.onClearThreads(EmptyRequest.getDefaultInstance()).map { }

    override suspend fun findAllThreads(): List<Thread> =
        endpointServiceCoroutineStub!!.findAllThreads(EmptyRequest.getDefaultInstance()).threadsList.map(::mapper)


    override suspend fun threadRemove(threadIdList: List<Long>) {
        endpointServiceCoroutineStub!!.threadRemove(IdList.newBuilder().addAllIds(threadIdList).build())
    }

    override suspend fun threadClear() {
        endpointServiceCoroutineStub!!.threadClear(EmptyRequest.getDefaultInstance())
    }

    override suspend fun grab(threadId: Long): List<PostSelection> =
        endpointServiceCoroutineStub!!.grab(Id.newBuilder().setId(threadId).build()).postSelectionListList.map {
            mapper(
                it
            )
        }


    override suspend fun download(posts: List<ThreadPostId>) {
        endpointServiceCoroutineStub!!.download(
            ThreadPostIdList.newBuilder().addAllThreadPostIdList(posts.map {
                EndpointServiceOuterClass.ThreadPostId.newBuilder().setThreadId(it.threadId).setPostId(it.postId)
                    .build()
            }).build()
        )
    }

    override fun onDownloadSpeed(): Flow<DownloadSpeed> =
        endpointServiceCoroutineStub!!.onDownloadSpeed(EmptyRequest.getDefaultInstance())
            .map { DownloadSpeed(it.speed) }

    override fun onVGUserUpdate(): Flow<String> =
        endpointServiceCoroutineStub!!.onVGUserUpdate(EmptyRequest.getDefaultInstance()).map { it.user }

    override fun onQueueStateUpdate(): Flow<QueueState> =
        endpointServiceCoroutineStub!!.onQueueStateUpdate(EmptyRequest.getDefaultInstance())
            .map { QueueState(it.running, it.remaining) }

    override fun onErrorCountUpdate(): Flow<ErrorCount> =
        endpointServiceCoroutineStub!!.onErrorCountUpdate(EmptyRequest.getDefaultInstance())
            .map { ErrorCount(it.count) }

    override fun onTasksRunning(): Flow<Boolean> =
        endpointServiceCoroutineStub!!.onTasksRunning(EmptyRequest.getDefaultInstance()).map { it.running }

    override suspend fun getSettings(): Settings =
        mapper(endpointServiceCoroutineStub!!.getSettings(EmptyRequest.getDefaultInstance()))

    override suspend fun saveSettings(settings: Settings) {
        val viperSettings = with(SettingsOuterClass.ViperSettings.newBuilder()) {
            login = settings.viperSettings.login
            username = settings.viperSettings.username
            password = settings.viperSettings.password
            thanks = settings.viperSettings.thanks
            host = settings.viperSettings.host
            build()
        }

        val downloadSettings = with(SettingsOuterClass.DownloadSettings.newBuilder()) {
            downloadPath = settings.downloadSettings.downloadPath
            autoStart = settings.downloadSettings.autoStart
            autoQueueThreshold = settings.downloadSettings.autoQueueThreshold
            forceOrder = settings.downloadSettings.forceOrder
            forumSubDirectory = settings.downloadSettings.forumSubDirectory
            threadSubLocation = settings.downloadSettings.threadSubLocation
            clearCompleted = settings.downloadSettings.clearCompleted
            appendPostId = settings.downloadSettings.appendPostId
            build()
        }

        val connectionSettings = with(SettingsOuterClass.ConnectionSettings.newBuilder()) {
            maxConcurrentPerHost = settings.connectionSettings.maxConcurrentPerHost
            maxGlobalConcurrent = settings.connectionSettings.maxGlobalConcurrent
            timeout = settings.connectionSettings.timeout
            maxAttempts = settings.connectionSettings.maxAttempts
            build()
        }

        val systemSettings = with(SettingsOuterClass.SystemSettings.newBuilder()) {
            tempPath = settings.systemSettings.tempPath
            enableClipboardMonitoring = settings.systemSettings.enableClipboardMonitoring
            clipboardPollingRate = settings.systemSettings.clipboardPollingRate
            maxEventLog = settings.systemSettings.maxEventLog
            build()
        }

        val settingsRequest = with(SettingsOuterClass.Settings.newBuilder()) {
            this.connectionSettings = connectionSettings
            this.downloadSettings = downloadSettings
            this.viperSettings = viperSettings
            this.systemSettings = systemSettings
            build()
        }
        endpointServiceCoroutineStub!!.saveSettings(settingsRequest)
    }

    override suspend fun getProxies(): List<String> =
        endpointServiceCoroutineStub!!.getProxies(EmptyRequest.getDefaultInstance()).proxiesList

    override fun onUpdateSettings(): Flow<Settings> =
        endpointServiceCoroutineStub!!.onUpdateSettings(EmptyRequest.getDefaultInstance()).map(::mapper)

    override suspend fun loggedInUser(): String =
        endpointServiceCoroutineStub!!.loggedInUser(EmptyRequest.getDefaultInstance()).user

    override suspend fun getVersion(): String =
        endpointServiceCoroutineStub!!.getVersion(EmptyRequest.getDefaultInstance()).version

    override suspend fun dbMigration(): String =
        endpointServiceCoroutineStub!!.dbMigration(EmptyRequest.getDefaultInstance()).message


    private fun mapper(settings: SettingsOuterClass.Settings): Settings =
        Settings(
            viperSettings = ViperSettings(
                login = settings.viperSettings.login,
                username = settings.viperSettings.username,
                password = settings.viperSettings.password,
                thanks = settings.viperSettings.thanks,
                host = settings.viperSettings.host,
            ),
            downloadSettings = DownloadSettings(
                downloadPath = settings.downloadSettings.downloadPath,
                autoStart = settings.downloadSettings.autoStart,
                autoQueueThreshold = settings.downloadSettings.autoQueueThreshold,
                forceOrder = settings.downloadSettings.forceOrder,
                forumSubDirectory = settings.downloadSettings.forumSubDirectory,
                threadSubLocation = settings.downloadSettings.threadSubLocation,
                clearCompleted = settings.downloadSettings.clearCompleted,
                appendPostId = settings.downloadSettings.appendPostId,
            ),
            connectionSettings = ConnectionSettings(
                maxConcurrentPerHost = settings.connectionSettings.maxConcurrentPerHost,
                maxGlobalConcurrent = settings.connectionSettings.maxGlobalConcurrent,
                timeout = settings.connectionSettings.timeout,
                maxAttempts = settings.connectionSettings.maxAttempts,
            ),
            systemSettings = SystemSettings(
                tempPath = settings.systemSettings.tempPath,
                enableClipboardMonitoring = settings.systemSettings.enableClipboardMonitoring,
                clipboardPollingRate = settings.systemSettings.clipboardPollingRate,
                maxEventLog = settings.systemSettings.maxEventLog,
            ),
        )


    private fun mapper(postEntity: PostOuterClass.Post): Post {
        return Post(
            id = postEntity.id,
            postTitle = postEntity.postTitle,
            threadTitle = postEntity.threadTitle,
            forum = postEntity.forum,
            url = postEntity.url,
            token = postEntity.token,
            postId = postEntity.postId,
            threadId = postEntity.threadId,
            total = postEntity.total,
            hosts = postEntity.hostsList.toSet(),
            downloadDirectory = postEntity.downloadDirectory,
            addedOn = LocalDateTime.parse(postEntity.addedOn, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            folderName = postEntity.folderName,
            status = Status.valueOf(postEntity.status),
            done = postEntity.done,
            rank = postEntity.rank,
            size = postEntity.size,
            downloaded = postEntity.downloaded,
            previews = postEntity.previewsList.toList(),
            postedBy = postEntity.postedBy,
            resolvedNames = postEntity.resolvedNamesList.toList(),
        )
    }

    private fun mapper(image: ImageOuterClass.Image): Image {
        return Image(
            id = image.id,
            postId = image.postId,
            url = image.url,
            thumbUrl = image.thumbUrl,
            host = image.host.toByte(),
            index = image.index,
            postIdRef = image.postIdRef,
            size = image.size,
            downloaded = image.downloaded,
            status = Status.valueOf(image.status),
            filename = image.filename,
        )
    }

    private fun mapper(metadata: MetadataOuterClass.Metadata): Metadata {
        return Metadata(
            metadata.postId,
            MetadataEntity.Data(metadata.postedBy, metadata.resolvedNamesList)
        )
    }

    private fun mapper(logEntry: Log): LogEntry {
        return LogEntry(
            logEntry.sequence,
            logEntry.timestamp,
            logEntry.threadName,
            logEntry.loggerName,
            logEntry.levelString,
            logEntry.formattedMessage,
            logEntry.throwable,
        )
    }

    private fun mapper(thread: ThreadOuterClass.Thread): Thread {
        return Thread(
            thread.id,
            thread.title,
            thread.link,
            thread.threadId,
            thread.total,
        )
    }

    private fun mapper(postSelection: EndpointServiceOuterClass.PostSelection): PostSelection {
        return PostSelection(
            postSelection.threadId,
            postSelection.threadTitle,
            postSelection.postId,
            postSelection.number,
            postSelection.title,
            postSelection.imageCount,
            postSelection.url,
            postSelection.hosts,
            postSelection.forum,
            postSelection.previewsList
        )
    }

    fun connect(host: String, port: Int) {
        channel = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(134217728).usePlaintext().build()
        endpointServiceCoroutineStub = EndpointServiceGrpcKt.EndpointServiceCoroutineStub(channel!!)
    }

    fun disconnect() {
        try {
            channel?.shutdownNow()
        } catch (_: Exception) {

        }
    }

    fun connectionState(): ConnectivityState = channel?.getState(true) ?: ConnectivityState.SHUTDOWN

    override fun ready(): Boolean {
        return connectionState() == ConnectivityState.READY
    }
}