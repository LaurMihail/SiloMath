package com.silomath.feature.masterdata.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrganizationUiState(
    val organizations:
        List<AuthorizedOrganizationContext> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedOrganizationId: String? = null
)

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> =
        _uiState.asStateFlow()

    private var allAuthorizedOrganizations:
        List<AuthorizedOrganizationContext> = emptyList()

    init {
        observeAuthorizedOrganizations()
    }

    private fun observeAuthorizedOrganizations() {
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                allAuthorizedOrganizations =
                    session.authorizedOrganizations

                _uiState.value =
                    _uiState.value.copy(
                        organizations =
                            filteredOrganizations(
                                query =
                                    _uiState.value.searchQuery
                            ),
                        isLoading = false,
                        error = null
                    )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val normalized = query.trimStart()

        _uiState.value =
            _uiState.value.copy(
                searchQuery = normalized,
                organizations =
                    filteredOrganizations(
                        query = normalized
                    )
            )
    }

    fun selectOrganization(
        organizationId: String
    ) {
        val isAuthorized =
            allAuthorizedOrganizations.any {
                it.organizationId == organizationId
            }

        if (!isAuthorized) {
            _uiState.value =
                _uiState.value.copy(
                    error =
                        "Organization is not authorized " +
                            "for current user"
                )
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            val result =
                authRepository
                    .setActiveOrganization(
                        organizationId
                    )

            _uiState.value =
                if (result.isSuccess) {
                    _uiState.value.copy(
                        isLoading = false,
                        selectedOrganizationId =
                            organizationId
                    )
                } else {
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            result
                                .exceptionOrNull()
                                ?.message
                                ?: "Failed to set active organization"
                    )
                }
        }
    }

    private fun filteredOrganizations(
        query: String
    ): List<AuthorizedOrganizationContext> {
        val filter =
            query.trim().lowercase()

        if (filter.isEmpty()) {
            return allAuthorizedOrganizations
        }

        return allAuthorizedOrganizations.filter {
            it.organizationName
                .lowercase()
                .contains(filter) ||
                it.organizationCode
                    .lowercase()
                    .contains(filter)
        }
    }
}