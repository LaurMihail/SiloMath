package com.silomath.feature.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.model.OrganizationReadModel
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.OrganizationRepository
import com.silomath.core.model.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class OrganizationUiState(
    val organizations: List<OrganizationReadModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedOrganizationId: String? = null
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
            authRepository.sessionState.collectLatest { session ->
                val userId = session.userId
                    ?.let { rawUserId ->
                        runCatching { UserId(rawUserId) }.getOrNull()
                    }

                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        organizations = emptyList(),
                        isLoading = false
                    )
                    return@collectLatest
                }

                _uiState.value = _uiState.value.copy(isLoading = true)

                organizationRepository
                    .observeAuthorizedOrganizations(userId)
                    .collect { organizations ->
                        _uiState.value = _uiState.value.copy(
                            organizations = organizations,
                            isLoading = false
                        )
                    }
            }
        }
    }

    fun selectOrganization(organizationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result =
                authRepository.setActiveOrganization(organizationId)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedOrganizationId = organizationId
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                        ?: "Failed to set active organization"
                )
            }
        }
    }
}