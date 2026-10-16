package com.silomath.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.silomath.core.designsystem.EnterpriseNavy

@Composable
fun SelectOrganizationScreen(
    onOrganizationSelected: (String) -> Unit,
    viewModel: OrganizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.selectedOrganizationId) {
        uiState.selectedOrganizationId?.let { id ->
            onOrganizationSelected(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 16.dp)
    ) {
        // TITLE
        Text(
            text = "Select Organization",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EnterpriseNavy
        )

        // TITLE TO LIST SPACING
        Spacer(modifier = Modifier.height(20.dp))

        // ERROR STATE
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // LOADING STATE
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EnterpriseNavy)
            }
        } else if (uiState.organizations.isEmpty()) {
            // EMPTY STATE
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No organizations available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF5E6878)
                )
            }
        } else {
            // ORGANIZATION LIST
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.organizations) { org ->
                    // ORGANIZATION CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clickable { viewModel.selectOrganization(org.organizationId.value) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        // CARD CONTENT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ORGANIZATION MARK
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8EDF4)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = org.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EnterpriseNavy
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // ORGANIZATION NAME
                            Text(
                                text = org.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EnterpriseNavy,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // DISCLOSURE INDICATOR
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select organization",
                                tint = Color(0xFF7A8494),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
