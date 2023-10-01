package me.mnlr.vripper

import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.EventBusImpl
import me.mnlr.vripper.host.*
import me.mnlr.vripper.repositories.*
import me.mnlr.vripper.repositories.impl.*
import me.mnlr.vripper.services.*
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<EventBus> {
        EventBusImpl()
    }
    single<SettingsService> {
        SettingsService(get())
    }
    single<ImageRepository> {
        ImageRepositoryImpl(get())
    }
    single<PostDownloadStateRepository> {
        PostDownloadStateRepositoryImpl(get())
    }
    single<MetadataRepository> {
        MetadataRepositoryImpl()
    }
    single<ThreadRepository> {
        ThreadRepositoryImpl(get())
    }
    single<LogEventRepository> {
        LogEventRepositoryImpl()
    }
    single<DataTransaction> {
        DataTransaction(get(), get(), get(), get(), get())
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
    single<GlobalStateService> {
        GlobalStateService(get(), get(), get(), get())
    }
    single<Host> {
        AcidimgHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        DPicMeHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImageBamHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImageTwistHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImageVenueHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImageZillaHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImgboxHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImgSpiceHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ImxHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        PimpandhostHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        PixhostHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        PixRouteHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        PixxxelsHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        PostImgHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        TurboImageHost(get(), get(), get())
    } bind Host::class
    single<Host> {
        ViprImHost(get(), get(), get())
    } bind Host::class
}