package com.silomath.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Six filters: Organization, Campaign, Location, Commodity, Ownership, Client
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = false, onClick = {}, label = { Text("Organization") })
            FilterChip(selected = false, onClick = {}, label = { Text("Campaign") })
            FilterChip(selected = false, onClick = {}, label = { Text("Location") })
            FilterChip(selected = false, onClick = {}, label = { Text("Commodity") })
            FilterChip(selected = false, onClick = {}, label = { Text("Ownership") })
            FilterChip(selected = false, onClick = {}, label = { Text("Client") })
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                KpiCard(
                    title = "Current Inventory by Commodity",
                    content = {
                        Column {
                            uiState.inventoryByCommodity.forEach { (name, amount) ->
                                Text("$name: $amount MT", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                )
            }
            item {
                KpiCard(
                    title = "Facility/Bin Status",
                    content = {
                        Column {
                            Text("Validated: ${uiState.binStatusCount["Validated"]}", style = MaterialTheme.typography.bodySmall)
                            Text("To Do: ${uiState.binStatusCount["To Do"]}", style = MaterialTheme.typography.bodySmall)
                            Text("Issues: ${uiState.binStatusCount["Issues"]}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                )
            }
            item {
                KpiCard(
                    title = "Active Campaigns",
                    value = uiState.activeCampaigns.toString()
                )
            }
            item {
                KpiCard(
                    title = "Open Issues",
                    value = uiState.openIssues.toString()
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(12.dp))
            if (value != null) {
                Text(text = value, style = MaterialTheme.typography.headlineMedium)
            }
            content?.invoke()
        }
    }
}
