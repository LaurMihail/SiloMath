package com.silomath.core.data.di

import com.silomath.core.data.repository.ProdAuthRepository
import com.silomath.core.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReleaseDataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: ProdAuthRepository): AuthRepository
}
