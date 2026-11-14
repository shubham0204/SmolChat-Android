/*
 * Copyright (C) 2026 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Activity
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.DownloadCloud
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Server
import compose.icons.feathericons.Shield
import compose.icons.feathericons.Sliders
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import kotlinx.coroutines.launch

private data class OnboardingFeatureItem(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

private sealed interface OnboardingPage {
    val heroIcon: ImageVector
    val titleRes: Int
    val descriptionRes: Int

    data class HighlightPage(
        override val heroIcon: ImageVector,
        override val titleRes: Int,
        override val descriptionRes: Int,
        val highlightsRes: List<Int>,
        val badgeText: String? = null,
    ) : OnboardingPage

    data class FeatureListPage(
        override val heroIcon: ImageVector,
        override val titleRes: Int,
        override val descriptionRes: Int,
        val features: List<OnboardingFeatureItem>,
    ) : OnboardingPage
}

private val onboardingPages: List<OnboardingPage> =
    listOf(
        OnboardingPage.HighlightPage(
            heroIcon = FeatherIcons.Shield,
            titleRes = R.string.onboarding_privacy_title,
            descriptionRes = R.string.onboarding_privacy_desc,
            highlightsRes =
                listOf(
                    R.string.onboarding_privacy_highlight_1,
                    R.string.onboarding_privacy_highlight_2,
                    R.string.onboarding_privacy_highlight_3,
                ),
        ),
        OnboardingPage.HighlightPage(
            heroIcon = FeatherIcons.DownloadCloud,
            titleRes = R.string.onboarding_hf_title,
            descriptionRes = R.string.onboarding_hf_desc,
            highlightsRes =
                listOf(
                    R.string.onboarding_hf_highlight_1,
                    R.string.onboarding_hf_highlight_2,
                    R.string.onboarding_hf_highlight_3,
                ),
            badgeText = "HuggingFace · .gguf · llama.cpp",
        ),
        OnboardingPage.HighlightPage(
            heroIcon = FeatherIcons.Server,
            titleRes = R.string.onboarding_server_title,
            descriptionRes = R.string.onboarding_server_desc,
            highlightsRes =
                listOf(
                    R.string.onboarding_server_highlight_1,
                    R.string.onboarding_server_highlight_2,
                    R.string.onboarding_server_highlight_3,
                ),
            badgeText = "http://localhost:8080/v1",
        ),
        OnboardingPage.FeatureListPage(
            heroIcon = FeatherIcons.Sliders,
            titleRes = R.string.onboarding_workspace_title,
            descriptionRes = R.string.onboarding_workspace_desc,
            features =
                listOf(
                    OnboardingFeatureItem(
                        icon = FeatherIcons.Mic,
                        titleRes = R.string.onboarding_feature_stt_title,
                        descriptionRes = R.string.onboarding_feature_stt_desc,
                    ),
                    OnboardingFeatureItem(
                        icon = FeatherIcons.Folder,
                        titleRes = R.string.onboarding_feature_folders_title,
                        descriptionRes = R.string.onboarding_feature_folders_desc,
                    ),
                    OnboardingFeatureItem(
                        icon = FeatherIcons.Sliders,
                        titleRes = R.string.onboarding_feature_settings_title,
                        descriptionRes = R.string.onboarding_feature_settings_desc,
                    ),
                ),
        ),
        OnboardingPage.HighlightPage(
            heroIcon = FeatherIcons.Activity,
            titleRes = R.string.onboarding_benchmark_title,
            descriptionRes = R.string.onboarding_benchmark_desc,
            highlightsRes =
                listOf(
                    R.string.onboarding_benchmark_highlight_1,
                    R.string.onboarding_benchmark_highlight_2,
                    R.string.onboarding_benchmark_highlight_3,
                ),
            badgeText = "tok/s · TTFT",
        ),
    )

@Preview(showBackground = true)
@Composable
private fun PreviewOnboardingScreenLight() {
    SmolLMAndroidTheme(darkTheme = false) {
        OnboardingScreen(onFinishOnboarding = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOnboardingScreenDark() {
    SmolLMAndroidTheme(darkTheme = true) {
        OnboardingScreen(onFinishOnboarding = {})
    }
}

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingTopBar(
                isLastPage = isLastPage,
                onSkip = onFinishOnboarding,
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = onboardingPages.size,
                isFirstPage = isFirstPage,
                isLastPage = isLastPage,
                onBack = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    if (isLastPage) {
                        onFinishOnboarding()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { pageIndex ->
            OnboardingPageContent(page = onboardingPages[pageIndex])
        }
    }
}

@Composable
private fun OnboardingTopBar(
    isLastPage: Boolean,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isLastPage) {
            TextButton(onClick = onSkip) {
                Text(text = stringResource(R.string.onboarding_skip))
            }
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.heroIcon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (page) {
            is OnboardingPage.HighlightPage -> {
                page.badgeText?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        page.highlightsRes.forEach { highlightRes ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(highlightRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            is OnboardingPage.FeatureListPage -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    page.features.forEach { feature ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = feature.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(feature.titleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(feature.descriptionRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    pageCount: Int,
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val isSelected = index == currentPage
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    label = "indicatorWidth",
                )
                val color by animateColorAsState(
                    targetValue =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    label = "indicatorColor",
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!isFirstPage) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = FeatherIcons.ArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.onboarding_back))
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text =
                        stringResource(
                            if (isLastPage) {
                                R.string.onboarding_get_started
                            } else {
                                R.string.onboarding_next
                            },
                        ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = FeatherIcons.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
