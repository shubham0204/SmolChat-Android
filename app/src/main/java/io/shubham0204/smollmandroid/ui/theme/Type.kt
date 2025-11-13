/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.shubham0204.smollmandroid.R

val bodyFontFamily =
    FontFamily(
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_black,
            weight = FontWeight.Black,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_bold,
            weight = FontWeight.Bold,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_light,
            weight = FontWeight.Light,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_regular,
            weight = FontWeight.Normal,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_medium,
            weight = FontWeight.Medium,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_semibold,
            weight = FontWeight.SemiBold,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_thin,
            weight = FontWeight.Thin,
        ),
    )

// Default Material 3 typography values
val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = bodyFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = bodyFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = bodyFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = bodyFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = bodyFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = bodyFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = bodyFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = bodyFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = bodyFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )
