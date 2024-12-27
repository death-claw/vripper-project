package me.vripper

import me.vripper.data.repositories.ImageRepository
import me.vripper.data.repositories.MetadataRepository
import me.vripper.data.repositories.PostRepository
import me.vripper.data.repositories.ThreadRepository
import me.vripper.data.repositories.impl.ImageRepositoryImpl
import me.vripper.data.repositories.impl.MetadataRepositoryImpl
import me.vripper.data.repositories.impl.PostRepositoryImpl
import me.vripper.data.repositories.impl.ThreadRepositoryImpl
import me.vripper.download.DownloadService
import me.vripper.event.EventBus
import me.vripper.host.*
import me.vripper.services.*
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    single<EventBus> {
        EventBus
    }
    single<SettingsService> {
        SettingsService(get())
    }
    single<LogService> {
        LogService(get())
    }
    single<ImageRepository> {
        ImageRepositoryImpl()
    }
    single<PostRepository> {
        PostRepositoryImpl()
    }
    single<MetadataRepository> {
        MetadataRepositoryImpl()
    }
    single<ThreadRepository> {
        ThreadRepositoryImpl()
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
        DownloadService(get(), get(), get(), get(), get())
    }
    single<DownloadSpeedService> {
        DownloadSpeedService(get())
    }

    single<AppEndpointService> {
        AppEndpointService(get(), get(), get(), get(), get(), get())
    }

    single((named("localAppEndpointService"))) {
        AppEndpointService(get(), get(), get(), get(), get(), get())
    } bind IAppEndpointService::class

    single<MetadataService> {
        MetadataService(get(), get(), get(), get())
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