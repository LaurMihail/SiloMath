package com.silomath.feature.masterdata.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(
        uiState.selectedOrganizationId
    ) {
        uiState.selectedOrganizationId
            ?.let(onOrganizationSelected)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                )
    ) {
        Text(
            text = "Select Organization",
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EnterpriseNavy
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange =
                viewModel::onSearchQueryChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            label = { Text("Search") },
            placeholder = {
                Text("Name or code")
            },
            leadingIcon = {
                Icon(
                    imageVector =
                        Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true
        )

        uiState.error?.let { message ->
            Text(
                text = message,
                color =
                    MaterialTheme.colorScheme.error,
                style =
                    MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier.padding(top = 12.dp)
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = EnterpriseNavy
                    )
                }
            }

            uiState.organizations.isEmpty() -> {
                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            if (
                                uiState.searchQuery
                                    .isBlank()
                            ) {
                                "No authorized organizations"
                            } else {
                                "No organizations match search"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                        color = Color(0xFF5E6878)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.organizations,
                        key = {
                            it.organizationId
                        }
                    ) { organization ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = 76.dp
                                    )
                                    .clickable {
                                        viewModel
                                            .selectOrganization(
                                                organization
                                                    .organizationId
                                            )
                                    },
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                ),
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            Color.White
                                    ),
                            elevation =
                                CardDefaults
                                    .cardElevation(
                                        defaultElevation =
                                            1.dp
                                    )
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal =
                                                16.dp,
                                            vertical =
                                                14.dp
                                        ),
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                Surface(
                                    modifier =
                                        Modifier.size(
                                            44.dp
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            8.dp
                                        ),
                                    color =
                                        Color(
                                            0xFFE8EDF4
                                        )
                                ) {
                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Text(
                                            text =
                                                organization
                                                    .organizationName
                                                    .take(1)
                                                    .uppercase(),
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleMedium,
                                            fontWeight =
                                                FontWeight
                                                    .Bold,
                                            color =
                                                EnterpriseNavy
                                        )
                                    }
                                }

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            14.dp
                                        )
                                )

                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {
                                    Text(
                                        text =
                                            organization
                                                .organizationName,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,
                                        fontWeight =
                                            FontWeight
                                                .SemiBold,
                                        color =
                                            EnterpriseNavy,
                                        maxLines = 2,
                                        overflow =
                                            TextOverflow
                                                .Ellipsis
                                    )

                                    Text(
                                        text =
                                            organization
                                                .organizationCode,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        color =
                                            Color(
                                                0xFF5E6878
                                            )
                                    )
                                }

                                Icon(
                                    imageVector =
                                        Icons
                                            .AutoMirrored
                                            .Filled
                                            .KeyboardArrowRight,
                                    contentDescription =
                                        "Select organization",
                                    tint =
                                        Color(
                                            0xFF7A8494
                                        ),
                                    modifier =
                                        Modifier.size(
                                            24.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
