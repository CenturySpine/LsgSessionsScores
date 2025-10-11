package fr.centuryspine.lsgscores.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import fr.centuryspine.lsgscores.data.authuser.CurrentUserProvider
import fr.centuryspine.lsgscores.data.authuser.CurrentUserProviderImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthUserModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(impl: CurrentUserProviderImpl): CurrentUserProvider
}
