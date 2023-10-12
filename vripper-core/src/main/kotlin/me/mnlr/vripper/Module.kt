package me.mnlr.vripper

import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.host.*
import me.mnlr.vripper.repositories.*
import me.mnlr.vripper.repositories.impl.*
import me.mnlr.vripper.services.*
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    single<EventBus> {
        EventBus
    }
    single<SettingsService> {
        SettingsService(get())
    }
    single<ImageRepository> {
        ImageRepositoryImpl()
    }
    single<PostDownloadStateRepository> {
        PostDownloadStateRepositoryImpl()
    }
    single<MetadataRepository> {
        MetadataRepositoryImpl()
    }
    single<ThreadRepository> {
        ThreadRepositoryImpl()
    }
    single<LogEventRepository> {
        LogEventRepositoryImpl()
    }
    single<DataTransaction> {
        DataTransaction(get(), get(), get(), get(), get(), get())
    }
    single<RetryPolicyService> {
        RetryPolicyService(get(), get())
    }
    single<HTTPService> {
        HTTPService(get(), get())
    }
    single<VGAuthService> {
        VGAuthService(get(), get(), get())
    }
    single<ThreadCacheService> {
        ThreadCacheService(get())
    }
    single<DownloadService> {
        DownloadService(get(), get(), get(), get(), get(), get())
    }
    single<DownloadSpeedService> {
        DownloadSpeedService(get())
    }
    single<AppEndpointService> {
        AppEndpointService(get(), get(), get(), get(), get())
    }
    single {
        AcidimgHost(get(), get(), get())
    } bind Host::class
    single {
        DPicMeHost(get(), get(), get())
    } bind Host::class
    single {
        ImageBamHost(get(), get(), get())
    } bind Host::class
    single {
        ImageTwistHost(get(), get(), get())
    } bind Host::class
    single {
        ImageVenueHost(get(), get(), get())
    } bind Host::class
    single {
        ImageZillaHost(get(), get(), get())
    } bind Host::class
    single {
        ImgboxHost(get(), get(), get())
    } bind Host::class
    single {
        ImgSpiceHost(get(), get(), get())
    } bind Host::class
    single {
        ImxHost(get(), get(), get())
    } bind Host::class
    single {
        PimpandhostHost(get(), get(), get())
    } bind Host::class
    single {
        PixhostHost(get(), get(), get())
    } bind Host::class
    single {
        PixRouteHost(get(), get(), get())
    } bind Host::class
    single {
        PixxxelsHost(get(), get(), get())
    } bind Host::class
    single {
        PostImgHost(get(), get(), get())
    } bind Host::class
    single {
        TurboImageHost(get(), get(), get())
    } bind Host::class
    single {
        ViprImHost(get(), get(), get())
    } bind Host::class
}