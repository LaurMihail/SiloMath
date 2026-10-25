package com.silomath.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.domain.repository.StocktakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val selectedOrganizationId: Long? = null,
    val selectedCampaignId: Long? = null,
    val selectedLocationId: Long? = null,
    val selectedCommodityId: Long? = null,
    val selectedOwnership: String? = null,
    val selectedClientId: Long? = null,
    val inventoryByCommodity: Map<String, Double> = emptyMap(),
    val binStatusCount: Map<String, Int> = mapOf("Validated" to 0, "To Do" to 0, "Issues" to 0),
    val activeCampaigns: Int = 0,
    val openIssues: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    @Suppress("unused")
    private val stocktakingRepository: StocktakingRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterState())
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    data class FilterState(
        val orgId: Long? = null,
        val campId: Long? = null,
        val locId: Long? = null,
        val commId: Long? = null,
        val ownership: String? = null,
        val clientId: Long? = null
    )

    init {
        viewModelScope.launch {
            campaignRepository.observeActiveCampaignCount().collect { count ->
                _uiState.update { it.copy(activeCampaigns = count) }
            }
        }
    }

    fun onFilterChanged(
        orgId: Long? = _filters.value.orgId,
        campId: Long? = _filters.value.campId,
        locId: Long? = _filters.value.locId,
        commId: Long? = _filters.value.commId,
        ownership: String? = _filters.value.ownership,
        clientId: Long? = _filters.value.clientId
    ) {
        _filters.update {
            it.copy(
                orgId = orgId,
                campId = campId,
                locId = locId,
                commId = commId,
                ownership = ownership,
                clientId = clientId
            )
        }
        _uiState.update {
            it.copy(
                selectedOrganizationId = orgId,
                selectedCampaignId = campId,
                selectedLocationId = locId,
                selectedCommodityId = commId,
                selectedOwnership = ownership,
                selectedClientId = clientId
            )
        }
    }
}
