/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Scrolls one parameter page while keeping scroll state local to that page instance. */
@Composable
fun ParameterPage(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("parameter-page-root"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** A quiet outlined group whose rows own their horizontal and vertical insets. */
@Composable
fun ParameterSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
            if (title != null) {
                Text(
                    title,
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            content()
        }
    }
}

/** A labeled value control used for enum and other selection dialogs. */
@Composable
fun ParameterSelectionRow(
    title: String,
    value: String,
    testTag: String,
    enabled: Boolean = true,
    contentDescription: String? = null,
    stateDescription: String? = null,
    valueTestTag: String = "$testTag-value",
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(
                if (contentDescription != null || stateDescription != null) {
                    Modifier.semantics(mergeDescendants = true) {
                        if (contentDescription != null) this.contentDescription = contentDescription
                        if (stateDescription != null) this.stateDescription = stateDescription
                    }
                } else Modifier,
            )
            .testTag(testTag),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.weight(0.5f).testTag("$testTag-label"),
                style = MaterialTheme.typography.bodyMedium,
                softWrap = true,
            )
            Text(
                value,
                Modifier.weight(0.5f).testTag(valueTestTag),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                softWrap = true,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp).testTag("$testTag-dropdown-indicator"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** A labeled editable or read-only text value. A null callback removes its button action. */
@Composable
fun ParameterTextValueRow(
    title: String,
    value: String,
    testTag: String,
    enabled: Boolean = true,
    numberText: String? = null,
    units: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val numericValue = numberText?.let {
        buildAnnotatedString {
            append(it)
            if (!units.isNullOrBlank()) {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                ) {
                    append(" $units")
                }
            }
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick))
            .testTag(testTag),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.weight(if (numericValue == null) 0.5f else 0.62f).testTag("$testTag-label"),
                style = MaterialTheme.typography.bodyMedium,
                softWrap = true,
            )
            if (numericValue == null) {
                Text(
                    value,
                    Modifier.weight(0.5f).testTag("$testTag-value"),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    softWrap = true,
                )
            } else {
                Text(
                    numericValue,
                    Modifier.weight(0.38f).testTag("$testTag-value"),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    textAlign = TextAlign.End,
                    softWrap = true,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ParameterValueRow(
    title: String,
    units: String,
    value: Float,
    precision: Int,
    testTag: String,
    onClick: () -> Unit,
) = ParameterValueRow(title, units, value, precision, testTag, true, onClick)

@Composable
fun ParameterValueRow(
    title: String, units: String, value: Float, precision: Int, testTag: String,
    enabled: Boolean, onClick: () -> Unit,
) {
    val valueColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val locale = LocalConfiguration.current.locales[0]
    val formattedValue = buildAnnotatedString {
        append(String.format(locale, "%.${precision}f", value))
        if (units.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
            ) {
                append(" $units")
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(testTag),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.weight(0.62f).testTag("$testTag-label"),
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                softWrap = true,
            )
            Text(
                formattedValue,
                Modifier.weight(0.38f).testTag("$testTag-value"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                textAlign = TextAlign.End,
                color = valueColor,
                softWrap = true,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ParameterReadOnlyValueRow(
    title: String,
    units: String,
    value: Float,
    precision: Int,
    testTag: String,
    readOnlyLabel: String,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedValue = buildAnnotatedString {
        append(String.format(locale, "%.${precision}f", value))
        if (units.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
            ) {
                append(" $units")
            }
        }
    }
    Column(Modifier.fillMaxWidth().testTag(testTag)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, softWrap = true)
                Text(readOnlyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                formattedValue,
                Modifier.weight(0.38f).testTag("$testTag-value"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                textAlign = TextAlign.End,
                softWrap = true,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ParameterSwitchRow(title: String, checked: Boolean, testTag: String, onCheckedChange: (Boolean) -> Unit) =
    ParameterSwitchRow(title, checked, testTag, true, onCheckedChange)

@Composable
fun ParameterSwitchRow(title: String, checked: Boolean, testTag: String, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(testTag),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, softWrap = true)
            Switch(checked, onCheckedChange, enabled = enabled, modifier = Modifier.testTag("$testTag-switch"))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
