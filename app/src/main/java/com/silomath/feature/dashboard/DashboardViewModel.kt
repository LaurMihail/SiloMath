package com.silomath.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.domain.repository.StocktakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
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
            // Global KPI: Active Campaigns
            campaignRepository.getActiveCampaigns().collect { campaigns ->
                _uiState.update { it.copy(activeCampaigns = campaigns.size) }
            }
        }

        viewModelScope.launch {
            // Filter-dependent KPIs
            _filters.flatMapLatest { filters ->
                val campId = filters.campId
                if (campId != null) {
                    stocktakingRepository.getRecords(campId).map { records ->
                        // Apply other filters locally for now if repo doesn't support them
                        val filtered = records.filter { record ->
                            (filters.commId == null || record.commodityId == filters.commId) &&
                            (filters.ownership == null || record.ownership == filters.ownership) &&
                            (filters.clientId == null || record.clientId == filters.clientId)
                        }
                        
                        _uiState.update { 
                            it.copy(
                                binStatusCount = mapOf(
                                    "Validated" to filtered.count { r -> r.status == "Validated" },
                                    "To Do" to filtered.count { r -> r.status == "To Do" },
                                    "Issues" to filtered.count { r -> r.status == "Issues" }
                                )
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(binStatusCount = mapOf("Validated" to 0, "To Do" to 0, "Issues" to 0)) }
                    flowOf(Unit)
                }
            }.collect()
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
