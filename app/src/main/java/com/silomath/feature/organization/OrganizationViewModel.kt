package com.silomath.feature.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.database.entity.OrganizationEntity
import com.silomath.core.domain.repository.OrganizationRepository
import com.silomath.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrganizationUiState(
    val organizations: List<OrganizationEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedOrganizationId: Long? = null
)

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = _uiState.asStateFlow()

    init {
        loadOrganizations()
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            organizationRepository.getOrganizations().collect { result ->
                _uiState.value = _uiState.value.copy(
                    organizations = result,
                    isLoading = false
                )
            }
        }
    }

    fun selectOrganization(organizationId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.setActiveOrganization(organizationId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, selectedOrganizationId = organizationId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = result.exceptionOrNull()?.message ?: "Failed to set active organization"
                )
            }
        }
    }
}
