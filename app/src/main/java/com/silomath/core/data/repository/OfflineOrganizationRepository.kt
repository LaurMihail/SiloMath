package com.silomath.core.data.repository

import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.entity.OrganizationEntity
import com.silomath.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineOrganizationRepository @Inject constructor(
    private val organizationDao: OrganizationDao
) : OrganizationRepository {
    override fun getOrganizations(): Flow<List<OrganizationEntity>> = organizationDao.getAllOrganizations()

    override suspend fun saveOrganization(organization: OrganizationEntity) {
        organizationDao.insertOrganization(organization)
    }
}
